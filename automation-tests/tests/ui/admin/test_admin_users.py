"""Run-owned user creation, search, detail and status persistence."""

import pytest
from framework.api.client import ApiBusinessError
from pages.admin.user_page import UserPage

pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.write]


class UserEditNotPersisted(AssertionError):
    """Only this business readback mismatch is an expected frontend TODO."""


def test_admin_ui_creates_and_reads_user(logged_in_admin_page, booking_api, admin_token, cleanup_registry, test_data_factory):
    data = test_data_factory.user()
    try:
        users = UserPage(logged_in_admin_page)
        user_id = users.create(data, cleanup_registry)
        profile = booking_api.get_admin_user(admin_token, user_id)
        assert profile["username"] == data["username"]
        assert profile["phone"] == data["phone"]
        users.open()
        users.search(data["username"])
        users.show_detail(data["username"])
    finally:
        data.clear()


def test_admin_ui_disables_owned_user(logged_in_admin_page, booking_api, admin_token, admin_ui_customer):
    user_id, data = admin_ui_customer["id"], admin_ui_customer["data"]
    users = UserPage(logged_in_admin_page)
    users.open()
    users.search(data["username"])
    users.disable(user_id, data["username"])
    assert booking_api.get_admin_user(admin_token, user_id)["status"] == 0
    with pytest.raises(ApiBusinessError) as error:
        booking_api.login_user(data["username"], data["password"])
    assert error.value.code == 100102


@pytest.mark.xfail(strict=True, raises=UserEditNotPersisted, reason="UI-TODO-001: UserEditPage saves only a toast without an API call")
def test_admin_ui_edit_nickname_persists(logged_in_admin_page, booking_api, admin_token, admin_ui_customer, run_id):
    user_id = admin_ui_customer["id"]
    expected = f"pw-course-{run_id}-updated"
    original = booking_api.get_admin_user(admin_token, user_id)["nickname"]
    if original == expected:
        pytest.fail("precondition: update must change the original nickname")
    UserPage(logged_in_admin_page).edit_nickname(user_id, expected)
    after = booking_api.get_admin_user(admin_token, user_id)
    if after["nickname"] != expected:
        raise UserEditNotPersisted("UI-TODO-001: saved nickname did not persist")
