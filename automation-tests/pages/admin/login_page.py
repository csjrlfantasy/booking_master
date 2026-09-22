"""Admin login uses the real form and browser redirect contract."""

import re
from playwright.sync_api import expect


class LoginPage:
    def __init__(self, page):
        self.page = page
        self.form = page.get_by_test_id("admin-login-form")

    def open(self):
        self.page.goto("/login")
        expect(self.form).to_be_visible()

    def login(self, username, password):
        self.page.get_by_test_id("admin-username").fill(username)
        self.page.get_by_test_id("admin-password").fill(password)
        self.page.get_by_test_id("admin-login-submit").click()

    def expect_error(self, message):
        expect(self.form.get_by_text(message, exact=True)).to_be_visible()

    def expect_authenticated(self):
        expect(self.page).to_have_url(re.compile(r"/$"))
        expect(self.page.get_by_role("heading", name="数据概览", exact=True)).to_be_visible()
        expect(self.form).to_have_count(0)
