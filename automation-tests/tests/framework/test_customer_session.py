import json
from types import SimpleNamespace

import pytest

from framework.auth.customer_session import customer_init_script
from conftest import (
    TemporaryCustomer,
    _create_temporary_customer,
    authenticated_customer_context,
    temporary_customer,
)
from framework.api.client import ApiBusinessError


def test_customer_session_matches_frontend_storage_contract():
    script, values = customer_init_script(
        token="secret-token", user_id=7, name="course-user", expires_in=3600
    )

    assert "booking_customer_token" in script
    assert values["token"] == "secret-token"
    assert values["userId"] == "7"
    assert values["expiresAt"] > 0


@pytest.mark.parametrize(
    ("token", "user_id", "name", "expires_in"),
    [
        ("", 7, "course-user", 3600),
        ("secret-token", 0, "course-user", 3600),
        ("secret-token", 7, "", 3600),
        ("secret-token", 7, "course-user", 0),
    ],
)
def test_customer_session_rejects_invalid_values(token, user_id, name, expires_in):
    with pytest.raises(ValueError):
        customer_init_script(
            token=token, user_id=user_id, name=name, expires_in=expires_in
        )


def test_customer_session_serializes_hostile_values_as_data():
    token = 'quote " and </script><script>bad()</script>'
    name = 'customer " name </script><script>bad()</script>'

    script, values = customer_init_script(
        token=token, user_id=7, name=name, expires_in=3600
    )

    payload = json.loads(script.removeprefix("const values = ").split(";", 1)[0])
    assert payload == values
    assert "bad()" in payload["token"]
    assert "</script>" not in script


def test_authenticated_context_closes_when_init_script_installation_fails():
    class FailingContext:
        closed = False

        def add_init_script(self, *, script):
            raise RuntimeError("init script rejected")

        def close(self):
            self.closed = True

    class FakeBrowser:
        def __init__(self):
            self.context = FailingContext()

        def new_context(self, *, base_url):
            assert base_url == "http://web.example"
            return self.context

    browser = FakeBrowser()
    customer = TemporaryCustomer(
        run_id="a1b2c3d4",
        user_id=7,
        username="course-user",
        token="secret-token",
        expires_in=3600,
    )
    fixture = authenticated_customer_context.__wrapped__(
        browser, SimpleNamespace(web_url="http://web.example"), customer
    )

    with pytest.raises(RuntimeError, match="init script rejected"):
        next(fixture)

    assert browser.context.closed is True


def test_temporary_customer_record_does_not_expose_factory_password():
    class FakeBookingApi:
        def register_user(self, payload):
            assert payload["password"] == "Course!secret"
            return 7

        def login_user(self, username, password):
            assert (username, password) == ("course-user", "Course!secret")
            return {"token": "secret-token", "userId": 7, "expiresIn": 3600}

    class FakeRegistry:
        def __init__(self):
            self.user_ids = []

        def add_user(self, user_id):
            self.user_ids.append(user_id)

    class FakeFactory:
        def user(self):
            return {
                "username": "course-user",
                "nickname": "course-user",
                "phone": "19000000000",
                "password": "Course!secret",
                "source": "h5",
            }

    registry = FakeRegistry()
    customer = _create_temporary_customer(
        "a1b2c3d4", FakeBookingApi(), registry, FakeFactory()
    )

    assert customer.user_id == 7
    assert registry.user_ids == [7]
    assert "password" not in repr(customer).casefold()
    assert "Course!secret" not in repr(customer)


def test_temporary_customer_teardown_uses_the_yielded_record(monkeypatch):
    class FakeBookingApi:
        def __init__(self):
            self.delete_call = None
            self.lookup_call = None

        def register_user(self, payload):
            return 17

        def login_user(self, username, password):
            return {"token": "record-token", "userId": 17, "expiresIn": 3600}

        def delete_test_user(self, run_id, user_id, course_key):
            self.delete_call = (run_id, user_id, course_key)
            return {"deleted": True}

        def get_user(self, user_id, token):
            self.lookup_call = (user_id, token)
            raise ApiBusinessError(100100, "not found")

    class FakeRegistry:
        def add_user(self, user_id):
            assert user_id == 17

    class FakeFactory:
        def user(self):
            return {
                "username": "course-user",
                "nickname": "course-user",
                "phone": "19000000000",
                "password": "Course!secret",
                "source": "h5",
            }

    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-course-key")
    api = FakeBookingApi()
    fixture = temporary_customer.__wrapped__(
        "a1b2c3d4", api, FakeRegistry(), FakeFactory()
    )
    customer = next(fixture)

    with pytest.raises(StopIteration):
        next(fixture)

    assert api.delete_call == (customer.run_id, customer.user_id, "synthetic-course-key")
    assert api.lookup_call == (customer.user_id, customer.token)
