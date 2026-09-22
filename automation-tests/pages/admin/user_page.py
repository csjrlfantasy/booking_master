"""Create and manage only run-owned users; capture IDs before UI assertions."""

from urllib.parse import urlsplit
from playwright.sync_api import expect


class UserPage:
    def __init__(self, page):
        self.page = page
        self.table = page.get_by_test_id("admin-user-table")

    def open(self):
        self.page.goto("/users")
        expect(self.page.get_by_role("heading", name="用户管理", exact=True)).to_be_visible()

    def search(self, username):
        self.page.get_by_test_id("admin-user-search").fill(username)
        self.page.get_by_role("button", name="筛选", exact=True).click()
        expect(self.row(username)).to_be_visible()

    def row(self, username):
        return self.table.get_by_role("row").filter(has=self.page.get_by_text(username, exact=True))

    def show_detail(self, username):
        self.row(username).get_by_role("button").click()
        self.page.get_by_role("menuitem", name="查看详情", exact=True).click()
        expect(self.page.get_by_role("heading", name=username, exact=True)).to_be_visible()

    def create(self, data, registry):
        self.page.goto("/users/new")
        for field, placeholder in (("username", "请输入用户名"), ("password", "请输入登录密码"), ("phone", "请输入手机号")):
            self.page.get_by_placeholder(placeholder, exact=True).fill(data[field])
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/admin/users") and r.request.method == "POST"
        ) as created:
            self.page.get_by_test_id("admin-user-create-submit").click()
        response = created.value
        payload = response.json()
        user_id = payload.get("data")
        if type(user_id) is int and user_id > 0:
            registry.add_user(user_id)
        if not response.ok or payload.get("code") != 0 or type(user_id) is not int or user_id <= 0:
            raise RuntimeError("admin user creation failed")
        expect(self.page.get_by_role("heading", name=data["username"], exact=True)).to_be_visible()
        return user_id

    def disable(self, user_id, username):
        self.row(username).get_by_role("button").click()
        self.page.once("dialog", lambda dialog: dialog.accept())
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/users/{user_id}/status") and r.request.method == "PUT"
        ) as response:
            self.page.get_by_role("menuitem", name="禁用用户", exact=True).click()
        if not response.value.ok or response.value.json().get("code") != 0:
            raise RuntimeError("admin user status request failed")
        expect(self.row(username).get_by_text("禁用", exact=True)).to_be_visible()

    def edit_nickname(self, user_id, nickname):
        self.page.goto(f"/users/{user_id}/edit")
        expect(self.page.get_by_role("heading", name="编辑用户", exact=True)).to_be_visible()
        self.page.get_by_placeholder("请输入昵称", exact=True).fill(nickname)
        self.page.get_by_role("button", name="保存", exact=True).click()
        expect(self.page.get_by_role("heading", name="基本信息", exact=True)).to_be_visible()
