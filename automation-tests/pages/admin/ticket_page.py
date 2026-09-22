"""Search and change status for a freshly-created teaching ticket."""

from urllib.parse import urlsplit
from playwright.sync_api import expect


class TicketPage:
    def __init__(self, page):
        self.page = page
        self.table = page.get_by_test_id("admin-ticket-table")

    def open(self):
        self.page.goto("/tickets")
        expect(self.page.get_by_role("heading", name="票务管理", exact=True)).to_be_visible()

    def row(self, name):
        return self.table.get_by_role("row").filter(has=self.page.get_by_text(name, exact=True))

    def search(self, name, status):
        self.page.get_by_placeholder("搜索票务名称...", exact=True).fill(name)
        self.page.get_by_role("combobox").select_option(str(status))
        self.page.get_by_role("button", name="筛选", exact=True).click()
        expect(self.row(name)).to_be_visible()

    def show_detail(self, name):
        self.row(name).get_by_role("button").click()
        self.page.get_by_role("menuitem", name="查看", exact=True).click()
        expect(self.page.get_by_role("heading", name=name, exact=True)).to_be_visible()

    def take_off_sale(self, ticket_id, name):
        # Clear the filter so the just-modified row remains visible.
        self.page.get_by_role("combobox").select_option("")
        self.row(name).get_by_role("button").click()
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/tickets/{ticket_id}/status") and r.request.method == "PUT"
        ) as response:
            self.page.get_by_role("menuitem", name="下架", exact=True).click()
        if not response.value.ok or response.value.json().get("code") != 0:
            raise RuntimeError("admin ticket status request failed")
        expect(self.row(name).get_by_text("已下架", exact=True)).to_be_visible()
