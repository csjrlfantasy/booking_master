"""Customer registration -> real admin UI disable -> denied customer API login."""

import pytest

from framework.api.client import ApiBusinessError
from pages.admin.user_page import UserPage

pytestmark = [pytest.mark.e2e, pytest.mark.chromium, pytest.mark.write,
              pytest.mark.destructive_commerce, pytest.mark.xdist_group("booking_e2e_commerce")]


def test_admin_disabling_customer_prevents_customer_login(
    logged_in_admin_page, e2e_customer_credentials, booking_api, admin_token, e2e_evidence,
):
    user_id = e2e_customer_credentials["user_id"]
    credentials = e2e_customer_credentials["credentials"]
    users = UserPage(logged_in_admin_page)
    users.open()
    users.search(credentials["username"])
    users.show_detail(credentials["username"])
    profile = booking_api.get_admin_user(admin_token, user_id)
    e2e_evidence("registered customer visible to administrator", userId=user_id, status=profile["status"])
    assert profile["username"] == credentials["username"] and profile["status"] == 1
    users.open()
    users.search(credentials["username"])
    users.disable(user_id, credentials["username"])
    disabled = booking_api.get_admin_user(admin_token, user_id)
    e2e_evidence("admin UI disabled customer", userId=user_id, status=disabled["status"])
    assert disabled["status"] == 0
    with pytest.raises(ApiBusinessError) as rejected:
        booking_api.login_user(credentials["username"], credentials["password"])
    assert rejected.value.code == 100102
