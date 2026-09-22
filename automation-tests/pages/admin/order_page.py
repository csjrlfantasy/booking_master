"""Order search, detail and actual cancel/refund menu journeys."""

from urllib.parse import urlsplit
from playwright.sync_api import expect


class OrderPage:
    def __init__(self, page):
        self.page = page
        self.table = page.get_by_test_id("admin-order-table")

    def open(self):
        self.page.goto("/orders")
        expect(self.page.get_by_role("heading", name="订单管理", exact=True)).to_be_visible()

    def row(self, order_no):
        return self.table.get_by_role("row").filter(has=self.page.get_by_text(order_no, exact=True))

    def search(self, order_no, status):
        self.page.get_by_placeholder("搜索订单号...", exact=True).fill(order_no)
        self.page.get_by_role("combobox").select_option(str(status))
        self.page.get_by_role("button", name="筛选", exact=True).click()
        expect(self.row(order_no)).to_be_visible()

    def show_detail(self, order_no):
        self.row(order_no).get_by_role("button").click()
        self.page.get_by_role("menuitem", name="查看详情", exact=True).click()
        expect(self.page.get_by_role("heading", name=order_no, exact=True)).to_be_visible()

    def cancel(self, order_id, order_no):
        self._change(order_id, order_no, "cancel", "取消订单", "已取消")

    def refund(self, order_id, order_no):
        self._change(order_id, order_no, "refund", "强制退款", "已退款")

    def _change(self, order_id, order_no, action, label, status_text):
        self.page.get_by_role("combobox").select_option("")
        self.row(order_no).get_by_role("button").click()
        self.page.once("dialog", lambda dialog: dialog.accept())
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/orders/{order_id}/{action}") and r.request.method == "PUT"
        ) as response:
            self.page.get_by_role("menuitem", name=label, exact=True).click()
        if not response.value.ok or response.value.json().get("code") != 0:
            raise RuntimeError(f"admin order {action} request failed")
        expect(self.row(order_no).get_by_text(status_text, exact=True)).to_be_visible()
