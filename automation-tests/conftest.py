"""Shared pytest lifecycle for safe, isolated BookingApi automation tests."""

from __future__ import annotations

import os
import uuid
from dataclasses import dataclass
from typing import Iterator, Mapping

import pytest

pytest.register_assert_rewrite("framework.reporting.artifacts")

from framework.api.booking import BookingApi
from framework.api.client import ApiBusinessError, ApiClient, ApiHttpError
from framework.auth.customer_session import customer_init_script
from framework.cleanup.registry import CleanupRegistry
from framework.config.models import Settings
from framework.data.factory import TestDataFactory
from framework.reporting.artifacts import BrowserArtifacts, attach_json


pytest_plugins = ("framework.config.pytest_plugin", "framework.reporting.artifacts")

_USER_NOT_FOUND_CODE = 100100


@dataclass(frozen=True, slots=True)
class TemporaryCustomer:
    """Authenticated customer data with no retained plaintext password."""

    run_id: str
    user_id: int
    username: str
    token: str
    expires_in: int


@pytest.fixture(scope="session")
def settings(pytestconfig: pytest.Config) -> Settings:
    return pytestconfig._booking_settings


@pytest.fixture(scope="session")
def api_client(settings: Settings) -> Iterator[ApiClient]:
    client = ApiClient(settings.api_url, settings.timeout_seconds)
    try:
        yield client
    finally:
        client.close()


@pytest.fixture(scope="session")
def booking_api(api_client: ApiClient) -> BookingApi:
    return BookingApi(api_client)


@pytest.fixture
def run_id() -> str:
    return uuid.uuid4().hex[:12]


@pytest.fixture(scope="session")
def admin_token(booking_api: BookingApi) -> str:
    """Resolve secrets at execution only; collection never logs in."""
    username = _non_empty_string(os.environ.get("BOOKING_ADMIN_USERNAME"), "BOOKING_ADMIN_USERNAME")
    password = _non_empty_string(os.environ.get("BOOKING_ADMIN_PASSWORD"), "BOOKING_ADMIN_PASSWORD")
    try:
        login = booking_api.login_admin(username, password)
    finally:
        del password
    return login["token"]


@pytest.fixture
def test_data_factory(run_id: str) -> TestDataFactory:
    return TestDataFactory(run_id)


@pytest.fixture
def cleanup_reporter():
    """A rejected or malformed cleanup result is a teardown failure."""
    def report(result):
        assert isinstance(result, dict), "invalid cleanup response"
        for category in ("deleted", "notFound", "rejected"):
            counts = result.get(category)
            assert isinstance(counts, dict), "invalid cleanup response counts"
            assert all(type(v) is int and v >= 0 for v in counts.values()), "invalid cleanup response counts"
        assert sum(result["rejected"].values()) == 0, "cleanup rejected run-owned resources"
    return report


@pytest.fixture
def cleanup_registry(
    run_id: str, booking_api: BookingApi, cleanup_reporter
) -> Iterator[CleanupRegistry]:
    registry = CleanupRegistry(run_id, booking_api, _course_test_key())
    try:
        yield registry
    finally:
        resources = {name: list(getattr(registry, f"_{name}_ids"))
                     for name in ("user", "order", "ticket", "coupon")}
        attach_json("cleanup pending: registered resource IDs", {"runId": run_id, "resources": resources})
        try:
            result = registry.cleanup()
            attach_json("cleanup observed: response", {"runId": run_id, "result": result})
            cleanup_reporter(result)
        except Exception:
            attach_json("cleanup observed: ERROR (see teardown report)", {"runId": run_id, "resources": resources})
            raise


@pytest.fixture(autouse=True)
def capture_test_evidence(request, monkeypatch):
    """Observe live suites only; all fixtures created by these suites stay lazy."""
    if not any(request.node.get_closest_marker(mark) for mark in ("api", "ui", "e2e")):
        return
    client = request.getfixturevalue("api_client")
    original_request = client.request

    def observed_request(method, path, **kwargs):
        # Payload text/headers never enter an attachment without recursive redaction.
        attach_json("API request summary", {"params": kwargs.get("params"), "body": kwargs.get("json")})
        try:
            result = original_request(method, path, **kwargs)
        except Exception as error:
            attach_json("API observed error", {"statusCode": getattr(error, "status_code", None),
                                              "businessCode": getattr(error, "code", None)})
            raise
        attach_json("API observed response summary", result)
        return result

    monkeypatch.setattr(client, "request", observed_request)
    if "browser" in request.fixturenames:
        browser = request.getfixturevalue("browser")
        name = request.getfixturevalue("browser_name")
        capture = BrowserArtifacts(request.config._booking_artifact_root, request.node.nodeid, name)
        request.node._booking_capture = capture
        capture.install(browser, monkeypatch)


@pytest.fixture
def temporary_customer(
    run_id: str,
    booking_api: BookingApi,
    cleanup_registry: CleanupRegistry,
    test_data_factory: TestDataFactory,
) -> Iterator[TemporaryCustomer]:
    customer = _create_temporary_customer(
        run_id, booking_api, cleanup_registry, test_data_factory
    )
    try:
        yield customer
    finally:
        booking_api.delete_test_user(run_id, customer.user_id, _course_test_key())
        _assert_user_absent(booking_api, customer.user_id, customer.token)


@pytest.fixture(scope="session", autouse=True)
def write_environment_guard(
    pytestconfig: pytest.Config, booking_api: BookingApi
) -> None:
    """Validate write endpoint authorization before any selected write test runs."""
    if not pytestconfig._booking_selected_write_tests:
        return
    booking_api.cleanup(uuid.uuid4().hex[:12], course_key=_course_test_key())


@pytest.fixture
def authenticated_customer_context(browser, settings: Settings, temporary_customer: TemporaryCustomer):
    """Create an authenticated browser context before its first page is opened."""
    context = browser.new_context(base_url=settings.web_url)
    try:
        script, _ = customer_init_script(
            token=temporary_customer.token,
            user_id=temporary_customer.user_id,
            name=temporary_customer.username,
            expires_in=temporary_customer.expires_in,
        )
        context.add_init_script(script=script)
        yield context
    finally:
        context.close()


def _course_test_key() -> str:
    key = os.environ.get("COURSE_TEST_KEY")
    if not isinstance(key, str) or not key.strip():
        raise RuntimeError("COURSE_TEST_KEY must be a non-empty environment variable")
    return key


def _non_empty_string(value: object, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise RuntimeError(f"{field} must be a non-empty string")
    return value


def _positive_integer(value: object, field: str) -> int:
    if type(value) is not int or value <= 0:
        raise RuntimeError(f"{field} must be a positive integer")
    return value


def _create_temporary_customer(
    run_id: str,
    booking_api: BookingApi,
    cleanup_registry: CleanupRegistry,
    test_data_factory: TestDataFactory,
) -> TemporaryCustomer:
    """Create a logged-in user and discard the password before returning its record."""
    user_data = test_data_factory.user()
    username = _non_empty_string(user_data.get("username"), "registered username")
    password = _non_empty_string(user_data.get("password"), "registered password")
    user_id = booking_api.register_user(user_data)
    cleanup_registry.add_user(user_id)
    try:
        login = booking_api.login_user(username, password)
    finally:
        user_data.clear()
        del user_data
        del password

    token = _non_empty_string(login.get("token"), "login token")
    logged_in_user_id = _positive_integer(login.get("userId"), "login user ID")
    if logged_in_user_id != user_id:
        raise RuntimeError("login user ID did not match the registered user")
    expires_in = _positive_integer(login.get("expiresIn"), "login expiry")
    return TemporaryCustomer(
        run_id=run_id,
        user_id=user_id,
        username=username,
        token=token,
        expires_in=expires_in,
    )


def _assert_user_absent(booking_api: BookingApi, user_id: int, token: str) -> None:
    try:
        user = booking_api.get_user(user_id, token)
    except ApiBusinessError as error:
        if error.code == _USER_NOT_FOUND_CODE:
            return
        raise AssertionError("user lookup returned an unrelated business error") from error
    except ApiHttpError as error:
        if error.status_code == 404:
            return
        raise AssertionError("user lookup returned an unexpected HTTP response") from error
    except Exception as error:
        raise AssertionError("user lookup failed while confirming deletion") from error
    if isinstance(user, Mapping):
        raise AssertionError("temporary user remained after delete")
    raise AssertionError("user lookup returned an unexpected successful response")
