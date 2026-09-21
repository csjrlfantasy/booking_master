"""Run-owned administrator user management with persisted-state assertions."""

import pytest

from framework.api.client import ApiBusinessError


pytestmark = [pytest.mark.api, pytest.mark.regression]


def test_admin_lists_users_and_available_roles(booking_api, admin_token):
    users = booking_api.list_admin_users(admin_token, page=1, page_size=10)
    roles = booking_api.list_admin_roles(admin_token)
    assert len(users["list"]) <= 10
    assert roles, "precondition: admin role catalogue is empty"
    assert len({role["id"] for role in roles}) == len(roles)


@pytest.mark.write
def test_admin_creates_searches_disables_and_reenables_own_user(booking_api, admin_token, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.create_admin_user(admin_token, payload)
    cleanup_registry.add_user(user_id)
    assert booking_api.get_admin_user(admin_token, user_id)["username"] == payload["username"]
    matches = booking_api.list_admin_users(admin_token, keyword=payload["username"])
    assert [item["id"] for item in matches["list"]] == [user_id]
    booking_api.update_admin_user_status(admin_token, user_id, 0)
    assert booking_api.get_admin_user(admin_token, user_id)["status"] == 0
    with pytest.raises(ApiBusinessError) as disabled:
        booking_api.login_user(payload["username"], payload["password"])
    assert disabled.value.code == 100102
    booking_api.update_admin_user_status(admin_token, user_id, 1)
    assert booking_api.get_admin_user(admin_token, user_id)["status"] == 1
    assert booking_api.login_user(payload["username"], payload["password"])["userId"] == user_id


@pytest.mark.write
@pytest.mark.xfail(strict=True, raises=AssertionError, reason="ADMIN-TODO-001: assignUserRole does not persist a role or grant its permission")
def test_assigned_super_admin_role_grants_admin_access(booking_api, admin_token, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.create_admin_user(admin_token, payload)
    cleanup_registry.add_user(user_id)
    roles = booking_api.list_admin_roles(admin_token)
    target = next((role for role in roles if role["roleCode"] == "SUPER_ADMIN"), None)
    if target is None:
        pytest.fail("precondition: SUPER_ADMIN role absent")
    booking_api.assign_admin_user_roles(admin_token, user_id, [target["id"]])
    # Re-read the persisted account through login; its level drives JwtAuthInterceptor.
    account = booking_api.login_user(payload["username"], payload["password"])
    assert account["userLevel"] >= 10, "ADMIN-TODO-001: assigned role has no effective admin permission"
    assert booking_api.get_admin_user(account["token"], user_id)["id"] == user_id
