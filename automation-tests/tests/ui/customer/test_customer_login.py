"""Browser authentication using disposable API users and UI registration."""

import pytest

from pages.customer.login_page import LoginPage
from pages.customer.shop_page import ShopPage


pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.public]


@pytest.mark.smoke
@pytest.mark.cross_browser
@pytest.mark.parametrize("customer_ui_page", [{"width": 1440, "height": 1000}, {"width": 390, "height": 844}], indirect=True, ids=["desktop", "mobile"])
def test_login_requires_username_and_password(customer_ui_page):
    login = LoginPage(customer_ui_page)
    login.open()
    login.expect_required_fields()
    login.fill("required-field-example", "")
    login.expect_required_fields()
    login.fill("", "not-submitted")
    login.expect_required_fields()


@pytest.mark.cross_browser
def test_registration_requires_phone(customer_ui_page):
    login = LoginPage(customer_ui_page)
    login.open()
    login.choose_registration()
    login.fill("not-submitted", "not-submitted")
    login.expect_required_fields()


@pytest.mark.write
@pytest.mark.chromium
def test_customer_can_login_with_newly_created_account(customer_ui_page, ui_login_credentials):
    login = LoginPage(customer_ui_page)
    login.open()
    login.login(ui_login_credentials["username"], ui_login_credentials["password"])
    login.expect_authenticated_at()
    ShopPage(customer_ui_page).expect_authenticated(ui_login_credentials["nickname"])


@pytest.mark.write
@pytest.mark.chromium
def test_login_rejects_wrong_password(customer_ui_page, ui_login_credentials):
    login = LoginPage(customer_ui_page)
    login.open()
    login.login(ui_login_credentials["username"], "Wrong-Disposable-Password!99")
    login.expect_login_error()


@pytest.mark.write
@pytest.mark.chromium
def test_customer_registration_logs_in_and_registers_cleanup(customer_ui_page, test_data_factory, cleanup_registry, booking_api):
    data = test_data_factory.user()
    login = LoginPage(customer_ui_page)
    login.open()
    try:
        user_id = login.register(data, cleanup_registry)
        login.expect_authenticated_at()
        ShopPage(customer_ui_page).expect_authenticated(data["username"])
        account = booking_api.login_user(data["username"], data["password"])
        assert account["userId"] == user_id
        assert booking_api.get_user(user_id, account["token"])["username"] == data["username"]
    finally:
        data.clear()


@pytest.mark.write
@pytest.mark.chromium
def test_login_preserves_ticket_return_to(customer_ui_page, ui_login_credentials, catalog_ticket):
    target = f"/shop/tickets/{catalog_ticket['id']}"
    login = LoginPage(customer_ui_page)
    login.open(target)
    login.login(ui_login_credentials["username"], ui_login_credentials["password"])
    login.expect_authenticated_at(target)
