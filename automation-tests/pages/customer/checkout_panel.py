"""Real storefront direct checkout (the web client has no cart screen)."""

import re
from urllib.parse import urlsplit

from playwright.sync_api import expect


class CheckoutPanel:
    def __init__(self, page):
        self.page = page
        self.panel = page.get_by_role("complementary")

    def select_inventory(self, session, level):
        session_title = f"{session.get('showDate') or '日期待定'} {session.get('showTime') or ''}".strip()
        self.panel.get_by_role("button", name=re.compile(rf"^{re.escape(session_title)}\s+余票")).click()
        self.panel.get_by_role("button", name=re.compile(rf"^{re.escape(level['name'])}\s+¥")).click()

    def fill_attendee_and_contact(self, identity_id, contact_name, phone, remark):
        self.panel.get_by_role("combobox").filter(
            has=self.page.get_by_role("option", name="选择第 1 位观演人身份证", exact=True)
        ).select_option(str(identity_id))
        self.panel.get_by_placeholder("联系人", exact=True).fill(contact_name)
        self.panel.get_by_placeholder("联系电话", exact=True).fill(phone)
        self.panel.get_by_placeholder("备注", exact=True).fill(remark)

    def apply_coupon(self, coupon_id, coupon_name):
        self.panel.get_by_role("combobox").filter(
            has=self.page.get_by_role("option", name=re.compile(re.escape(coupon_name)))
        ).select_option(str(coupon_id))

    def submit(self, cleanup_registry):
        button = self.panel.get_by_role("button", name="提交订单", exact=True)
        expect(button).to_be_enabled()
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/orders") and r.request.method == "POST"
        ) as created:
            button.click()
        response = created.value
        payload = response.json()
        order = payload.get("data")
        if isinstance(order, dict) and type(order.get("orderId")) is int:
            cleanup_registry.add_order(order["orderId"])
        assert response.ok and payload.get("code") == 0, "UI order creation failed"
        assert isinstance(order, dict) and order.get("orderId"), "missing created order ID"
        expect(self.panel.get_by_text(f"订单号：{order['orderNo']}", exact=True)).to_be_visible()
        return order

    def expect_coupon_discount(self, amount):
        """Wait for the asynchronously calculated, user-visible coupon amount."""
        expect(self.panel.get_by_text(f"-¥{amount:.2f}", exact=True)).to_be_visible()
