"""Customer order state transitions and refresh persistence."""

from urllib.parse import urlsplit

from playwright.sync_api import expect

from .shop_page import ShopPage


class OrderPanel:
    def __init__(self, page):
        self.page = page

    def open(self, customer_name, order_no):
        ShopPage(self.page).open_orders(customer_name)
        self.page.get_by_role("button").filter(has_text=order_no).click()
        expect(self.page.get_by_role("heading", level=2)).to_be_visible()
        expect(self.page.get_by_text(f"订单号：{order_no}", exact=True).first).to_be_visible()

    def expect_status(self, order_no, status):
        expect(self.page.get_by_role("button").filter(has_text=order_no)).to_contain_text(status)

    def _mutate(self, name, suffix, method):
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(suffix) and r.request.method == method
        ) as changed:
            self.page.get_by_role("button", name=name, exact=True).click()
        response = changed.value
        assert response.ok and response.json().get("code") == 0, "order mutation failed"

    def cancel(self, order_id):
        self._mutate("取消订单", f"/orders/{order_id}/cancel", "PUT")

    def pay(self):
        self._mutate("支付宝", "/payment/mock-pay", "POST")

    def refund(self):
        self._mutate("申请退款", "/payment/refund", "POST")

    def refresh_and_expect(self, customer_name, order_no, status):
        self.page.reload()
        self.open(customer_name, order_no)
        self.expect_status(order_no, status)
