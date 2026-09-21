"""Administrator login and authorization boundaries."""

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.regression]


@pytest.mark.smoke
def test_admin_login_grants_access_to_protected_overview(booking_api, admin_token):
    stats = booking_api.get_admin_overview_stats(admin_token)
    assert stats["todayOrders"] >= 0


@pytest.mark.parametrize("path", ["/admin/users", "/admin/tickets", "/admin/orders", "/admin/coupons", "/admin/roles", "/admin/stats/overview"])
def test_admin_endpoints_reject_missing_token(api_client, path):
    with pytest.raises(ApiHttpError) as denied:
        api_client.get(path)
    assert denied.value.status_code == 401


@pytest.mark.write
@pytest.mark.parametrize("path", ["/admin/users", "/admin/tickets", "/admin/orders", "/admin/coupons", "/admin/roles", "/admin/stats/overview"])
def test_admin_endpoints_reject_customer_token(api_client, temporary_customer, path):
    with pytest.raises(ApiHttpError) as denied:
        api_client.get(path, token=temporary_customer.token)
    assert denied.value.status_code == 403


@pytest.mark.write
def test_admin_login_rejects_bad_password_and_customer_role(booking_api, cleanup_registry, test_data_factory):
    # Never try bad passwords against the shared admin: that can lock it out.
    payload = test_data_factory.user()
    user_id = booking_api.register_user(payload)
    cleanup_registry.add_user(user_id)
    with pytest.raises(ApiBusinessError) as bad_password:
        booking_api.login_admin(payload["username"], payload["password"] + "wrong")
    assert bad_password.value.code == 100103
    with pytest.raises(ApiBusinessError) as non_admin:
        booking_api.login_admin(payload["username"], payload["password"])
    assert non_admin.value.code == 100001


@pytest.mark.parametrize("payload", [{}, {"username": ""}, {"password": ""}])
def test_admin_login_requires_credentials(api_client, payload):
    with pytest.raises(ApiHttpError) as invalid:
        api_client.post("/admin/login", json=payload)
    assert invalid.value.status_code == 400
