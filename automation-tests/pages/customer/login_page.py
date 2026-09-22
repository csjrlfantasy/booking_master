"""Customer authentication actions; registration is tracked before UI assertions."""

import re
from urllib.parse import quote, urlsplit

from playwright.sync_api import expect


class LoginPage:
    def __init__(self, page):
        self.page = page
        self.form = page.get_by_test_id("shop-login-form")
        self.submit = page.get_by_test_id("shop-login-submit")

    def open(self, return_to="/shop"):
        self.page.goto(f"/shop/login?returnTo={quote(return_to, safe='')}")
        expect(self.form).to_be_visible()

    def choose_registration(self):
        self.page.get_by_role("button", name="注册", exact=True).click()
        expect(self.page.get_by_placeholder("手机号", exact=True)).to_be_visible()

    def fill(self, username, password, phone=None):
        self.page.get_by_placeholder("用户名", exact=True).fill(username)
        self.page.get_by_placeholder("密码", exact=True).fill(password)
        if phone is not None:
            self.page.get_by_placeholder("手机号", exact=True).fill(phone)

    def login(self, username, password):
        self.fill(username, password)
        self.submit.click()

    def register(self, data, cleanup_registry):
        self.choose_registration()
        self.fill(data["username"], data["password"], data["phone"])
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/user/register")
            and r.request.method == "POST"
        ) as registered:
            self.submit.click()
        response = registered.value
        payload = response.json()
        # Register as soon as the server returns an ID, before navigation/assertions.
        user_id = payload.get("data")
        if type(user_id) is int and user_id > 0:
            cleanup_registry.add_user(user_id)
        assert response.ok and payload.get("code") == 0, "UI registration failed"
        assert type(user_id) is int and user_id > 0, "registration returned no user ID"
        return user_id

    def expect_required_fields(self):
        expect(self.submit).to_be_disabled()

    def expect_authenticated_at(self, return_to="/shop"):
        expect(self.page).to_have_url(re.compile(rf"{re.escape(return_to)}$"))
        expect(self.form).to_have_count(0)

    def expect_login_error(self):
        expect(self.page.get_by_role("status")).to_contain_text("密码")
        expect(self.form).to_be_visible()
