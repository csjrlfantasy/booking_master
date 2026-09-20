"""Run-owned customer authentication coverage for the public API environment."""

from __future__ import annotations

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.write, pytest.mark.public]


def _register_customer(booking_api, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.register_user(payload)
    cleanup_registry.add_user(user_id)
    return user_id, payload


def _assert_validation_failure(error: BaseException) -> None:
    if isinstance(error, ApiBusinessError):
        assert error.code in {100600, 100601, 100602}
        return
    assert isinstance(error, ApiHttpError)
    assert error.status_code in {400, 422}


def _assert_forbidden(error: BaseException) -> None:
    if isinstance(error, ApiBusinessError):
        assert error.code == 100001
        return
    assert isinstance(error, ApiHttpError)
    assert error.status_code == 403


def test_registered_temporary_customer_can_fetch_own_profile(temporary_customer, booking_api):
    profile = booking_api.get_user(
        temporary_customer.user_id, temporary_customer.token
    )

    assert profile["userId"] == temporary_customer.user_id
    assert profile["username"] == temporary_customer.username


def test_valid_login_returns_non_empty_token_matching_user_and_positive_expiry(
    booking_api, cleanup_registry, test_data_factory
):
    user_id, payload = _register_customer(
        booking_api, cleanup_registry, test_data_factory
    )

    login = booking_api.login_user(payload["username"], payload["password"])

    assert isinstance(login["token"], str) and login["token"].strip()
    assert login["userId"] == user_id
    assert type(login["expiresIn"]) is int and login["expiresIn"] > 0


def test_invalid_password_returns_current_business_code(
    booking_api, cleanup_registry, test_data_factory
):
    _, payload = _register_customer(booking_api, cleanup_registry, test_data_factory)

    with pytest.raises(ApiBusinessError) as error:
        booking_api.login_user(payload["username"], f"{payload['password']}!wrong")

    assert error.value.code == 100103


def test_duplicate_username_returns_current_backend_code(
    booking_api, cleanup_registry, test_data_factory
):
    _, original = _register_customer(booking_api, cleanup_registry, test_data_factory)
    duplicate = test_data_factory.user()
    duplicate["username"] = original["username"]

    with pytest.raises(ApiBusinessError) as error:
        booking_api.register_user(duplicate)

    assert error.value.code in {100101, 100106}


def test_duplicate_phone_returns_current_backend_code(
    booking_api, cleanup_registry, test_data_factory
):
    _, original = _register_customer(booking_api, cleanup_registry, test_data_factory)
    duplicate = test_data_factory.user()
    duplicate["phone"] = original["phone"]

    with pytest.raises(ApiBusinessError) as error:
        booking_api.register_user(duplicate)

    assert error.value.code == 100104


@pytest.mark.parametrize("missing_field", ("username", "password", "phone"))
def test_registration_rejects_each_missing_required_field_without_creating_a_user(
    booking_api, test_data_factory, missing_field
):
    payload = test_data_factory.user()
    payload.pop(missing_field)

    with pytest.raises((ApiBusinessError, ApiHttpError)) as error:
        booking_api.register_user(payload)

    _assert_validation_failure(error.value)


def test_authenticated_customer_cannot_fetch_another_users_profile(
    temporary_customer, booking_api, cleanup_registry, test_data_factory
):
    other_user_id, _ = _register_customer(
        booking_api, cleanup_registry, test_data_factory
    )

    with pytest.raises((ApiBusinessError, ApiHttpError)) as error:
        booking_api.get_user(other_user_id, temporary_customer.token)

    _assert_forbidden(error.value)
