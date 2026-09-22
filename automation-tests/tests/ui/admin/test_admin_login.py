"""Admin authentication and guard journeys without storing credentials on disk."""

import re
import pytest
from playwright.sync_api import expect
from pages.admin.login_page import LoginPage

pytestmark = [pytest.mark.ui, pytest.mark.regression]


@pytest.mark.smoke
@pytest.mark.cross_browser
@pytest.mark.parametrize(("username", "password", "message"), [
    ("", "", "请输入用户名"), ("teaching-validation-only", "", "请输入密码"),
])
def test_admin_login_requires_fields(admin_ui_page, username, password, message):
    login = LoginPage(admin_ui_page)
    login.open()
    login.login(username, password)
    login.expect_error(message)


@pytest.mark.smoke
@pytest.mark.cross_browser
def test_unauthenticated_admin_route_redirects_to_login(admin_ui_page):
    admin_ui_page.goto("/users")
    expect(admin_ui_page).to_have_url(re.compile(r"/login$"))
    expect(admin_ui_page.get_by_test_id("admin-login-form")).to_be_visible()


@pytest.mark.smoke
def test_admin_login_success_redirects_to_dashboard(admin_ui_page, admin_ui_credentials):
    login = LoginPage(admin_ui_page)
    login.open()
    login.login(**admin_ui_credentials)
    login.expect_authenticated()


@pytest.mark.write
def test_admin_login_rejects_wrong_password_and_customer_role(admin_ui_page, admin_ui_customer):
    data = admin_ui_customer["data"]
    login = LoginPage(admin_ui_page)
    login.open()
    login.login(data["username"], data["password"] + "wrong")
    login.expect_error("用户名或密码错误")
    login.login(data["username"], data["password"])
    login.expect_error("仅管理员可登录后台")
    expect(admin_ui_page).to_have_url(re.compile(r"/login$"))
