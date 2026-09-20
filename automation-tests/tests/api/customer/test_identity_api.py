"""Run-owned common-attendee identity coverage for the public API environment."""

from __future__ import annotations

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.write, pytest.mark.public]


def _register_customer(booking_api, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.register_user(payload)
    cleanup_registry.add_user(user_id)
    login = booking_api.login_user(payload["username"], payload["password"])
    return user_id, login["token"]


def _teaching_only_id_card(run_id: str, variant: int) -> str:
    sequence = (int(run_id[-8:], 16) + variant) % 1000
    base = f"00000020000101{sequence:03d}"
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    checks = "10X98765432"
    return base + checks[sum(int(digit) * weight for digit, weight in zip(base, weights)) % 11]


def _identity_payload(run_id: str, variant: int) -> dict[str, object]:
    return {
        "realName": f"TEACHING-ONLY-{variant}",
        "idCardNo": _teaching_only_id_card(run_id, variant),
        "phone": f"190{int(run_id[-8:], 16) % 100_000_000:08d}",
        "self": variant % 2 == 0,
        "defaultIdentity": variant == 1,
    }


def _assert_forbidden(error: BaseException) -> None:
    if isinstance(error, ApiBusinessError):
        assert error.code == 100001
        return
    assert isinstance(error, ApiHttpError)
    assert error.status_code == 403


def test_new_customer_identity_list_is_empty(temporary_customer, booking_api):
    identities = booking_api.list_identities(temporary_customer.token)

    assert identities == []


def test_customer_can_create_update_and_delete_own_identity(
    run_id, temporary_customer, booking_api
):
    created = booking_api.create_identity(
        temporary_customer.token, _identity_payload(run_id, 1)
    )
    identity_id = created["id"]
    assert type(identity_id) is int and identity_id > 0
    assert any(
        identity["id"] == identity_id
        for identity in booking_api.list_identities(temporary_customer.token)
    )

    updated_payload = _identity_payload(run_id, 2)
    updated = booking_api.update_identity(
        temporary_customer.token, identity_id, updated_payload
    )
    assert updated["id"] == identity_id
    assert any(
        identity["id"] == identity_id
        and identity["realName"] == updated_payload["realName"]
        and identity["idCardNo"] == updated_payload["idCardNo"]
        for identity in booking_api.list_identities(temporary_customer.token)
    )

    assert booking_api.delete_identity(temporary_customer.token, identity_id) is None
    assert all(
        identity["id"] != identity_id
        for identity in booking_api.list_identities(temporary_customer.token)
    )


@pytest.mark.parametrize("missing_field", ("realName", "idCardNo"))
def test_identity_create_requires_name_and_id_card(
    run_id, temporary_customer, booking_api, missing_field
):
    payload = _identity_payload(run_id, 3)
    payload.pop(missing_field)

    with pytest.raises(ApiHttpError) as error:
        booking_api.create_identity(temporary_customer.token, payload)

    assert error.value.status_code == 400


def test_identity_list_rejects_unauthenticated_access(api_client):
    with pytest.raises(ApiHttpError) as error:
        api_client.get("/user/identities")

    assert error.value.status_code == 401


def test_customer_cannot_update_or_delete_another_customers_identity(
    run_id, booking_api, cleanup_registry, test_data_factory
):
    _, owner_token = _register_customer(
        booking_api, cleanup_registry, test_data_factory
    )
    _, other_token = _register_customer(
        booking_api, cleanup_registry, test_data_factory
    )
    created = booking_api.create_identity(owner_token, _identity_payload(run_id, 4))
    identity_id = created["id"]
    assert type(identity_id) is int and identity_id > 0

    with pytest.raises((ApiBusinessError, ApiHttpError)) as update_error:
        booking_api.update_identity(other_token, identity_id, _identity_payload(run_id, 5))
    _assert_forbidden(update_error.value)

    with pytest.raises((ApiBusinessError, ApiHttpError)) as delete_error:
        booking_api.delete_identity(other_token, identity_id)
    _assert_forbidden(delete_error.value)
