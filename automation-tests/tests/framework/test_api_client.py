import pytest
import requests

from framework.api.booking import BookingApi
from framework.api.client import (
    ApiBusinessError,
    ApiClient,
    ApiContractError,
    ApiHttpError,
    ApiTransportError,
    redact_sensitive,
)


def test_client_extracts_data_from_successful_result_envelope(requests_mock):
    requests_mock.get(
        "http://example.test/api/tickets",
        json={"code": 0, "message": "ok", "data": {"id": 3}},
    )

    assert ApiClient("http://example.test/api").get("/tickets") == {"id": 3}


def test_client_rejects_non_result_envelope(requests_mock):
    requests_mock.get("http://example.test/api/tickets", json={"items": []})

    with pytest.raises(ApiContractError, match="Result envelope"):
        ApiClient("http://example.test/api").get("/tickets")


def test_client_rejects_non_zero_application_code(requests_mock):
    requests_mock.get(
        "http://example.test/api/tickets",
        json={"code": 4001, "message": "invalid request", "data": None},
    )

    with pytest.raises(ApiBusinessError, match="4001.*invalid request"):
        ApiClient("http://example.test/api").get("/tickets")


def test_client_converts_non_success_http_status_without_leaking_secrets(requests_mock):
    requests_mock.get(
        "http://example.test/api/tickets",
        status_code=503,
        json={"password": "not-for-logs", "message": "temporarily unavailable"},
    )

    with pytest.raises(ApiHttpError) as error:
        ApiClient("http://example.test/api").get("/tickets")

    assert error.value.status_code == 503
    assert "not-for-logs" not in str(error.value)
    assert "temporarily unavailable" not in str(error.value)


def test_client_forwards_configured_timeout_and_bearer_header(requests_mock):
    requests_mock.get(
        "http://example.test/api/tickets",
        json={"code": 0, "message": "ok", "data": []},
    )
    session = requests.Session()
    client = ApiClient("http://example.test/api/", timeout_seconds=17, session=session)

    client.get("/tickets", token="private-token")

    request = requests_mock.request_history[0]
    assert request.headers["Authorization"] == "Bearer private-token"
    assert request.timeout == 17
    assert client.base_url == "http://example.test/api"


def test_client_requires_absolute_api_path_and_wraps_request_failures():
    class FailingSession:
        def request(self, **kwargs):
            raise requests.RequestException("network down")

        def close(self):
            pass

    client = ApiClient("http://example.test/api", session=FailingSession())

    with pytest.raises(ValueError, match="begin with '/'"):
        client.get("tickets")
    with pytest.raises(ApiTransportError) as error:
        client.get("/tickets")
    assert "network down" not in str(error.value)
    assert error.value.__cause__ is None


def test_client_does_not_mutate_headers_and_supports_context_manager(requests_mock):
    requests_mock.get(
        "http://example.test/api/tickets",
        json={"code": 0, "message": "ok", "data": []},
    )
    headers = {"X-Request-Id": "req-1"}

    with ApiClient("http://example.test/api") as client:
        client.get("/tickets", token="private-token", headers=headers)

    assert headers == {"X-Request-Id": "req-1"}


def test_sensitive_values_are_recursively_redacted():
    value = {
        "PASSWORD": "secret",
        "nested": [{"access_token": "private"}],
        "plain": "safe",
    }

    assert redact_sensitive(value) == {
        "PASSWORD": "***REDACTED***",
        "nested": [{"access_token": "***REDACTED***"}],
        "plain": "safe",
    }
    assert "secret" not in repr(redact_sensitive(value))


def test_booking_api_builds_test_support_payloads_and_validates_ids(requests_mock):
    requests_mock.delete(
        "http://example.test/api/test-support/users/7",
        json={"code": 0, "message": "ok", "data": {"deleted": True}},
    )
    requests_mock.post(
        "http://example.test/api/test-support/cleanup",
        json={"code": 0, "message": "ok", "data": {"cleaned": True}},
    )
    booking = BookingApi(ApiClient("http://example.test/api"))

    assert booking.delete_test_user("a1b2c3d4", 7, "course-key") == {"deleted": True}
    assert booking.cleanup(
        "a1b2c3d4",
        user_ids=[1],
        order_ids=[2],
        ticket_ids=[3],
        coupon_ids=[4],
        course_key="course-key",
    ) == {"cleaned": True}
    delete_request, cleanup_request = requests_mock.request_history
    assert delete_request.url == "http://example.test/api/test-support/users/7?runId=a1b2c3d4"
    assert delete_request.headers["X-Course-Test-Key"] == "course-key"
    assert cleanup_request.json() == {
        "runId": "a1b2c3d4",
        "userIds": [1],
        "orderIds": [2],
        "ticketIds": [3],
        "couponIds": [4],
    }
    assert cleanup_request.headers["X-Course-Test-Key"] == "course-key"
    with pytest.raises(ValueError, match="positive"):
        booking.get_user(0, "token")
    with pytest.raises(ValueError, match="runId"):
        booking.delete_test_user("bad-run-id", 1, "course-key")


def test_booking_api_user_operations_use_expected_routes(requests_mock):
    requests_mock.post(
        "http://example.test/api/user/register",
        json={"code": 0, "message": "ok", "data": 9},
    )
    requests_mock.post(
        "http://example.test/api/user/login",
        json={"code": 0, "message": "ok", "data": {"token": "abc"}},
    )
    requests_mock.get(
        "http://example.test/api/user/9",
        json={"code": 0, "message": "ok", "data": {"id": 9}},
    )
    booking = BookingApi(ApiClient("http://example.test/api"))

    assert booking.register_user({"username": "learner"}) == 9
    assert booking.login_user("learner", "secret") == {"token": "abc"}
    assert booking.get_user(9, "abc") == {"id": 9}
    login_request, get_request = requests_mock.request_history[1:]
    assert login_request.json() == {"username": "learner", "password": "secret"}
    assert get_request.headers["Authorization"] == "Bearer abc"
