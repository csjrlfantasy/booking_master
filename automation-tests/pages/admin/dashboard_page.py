"""Dashboard assertions use the browser's own API snapshot, avoiding count races."""

from urllib.parse import urlsplit
from playwright.sync_api import expect


class DashboardPage:
    def __init__(self, page):
        self.page = page

    def open(self):
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/admin/stats/overview") and r.request.method == "GET"
        ) as response:
            self.page.goto("/")
        payload = response.value.json()
        if not response.value.ok or payload.get("code") != 0:
            raise RuntimeError("dashboard overview API failed")
        expect(self.page.get_by_role("heading", name="数据概览", exact=True)).to_be_visible()
        return payload["data"]

    def expect_snapshot(self, snapshot):
        fields = {"今日销售额": "todaySales", "今日订单": "todayOrders",
                  "今日新增用户": "todayUsers", "今日退款": "todayRefunds"}
        for label, key in fields.items():
            value = snapshot[key]
            assert isinstance(value, (int, float)) and value >= 0, f"invalid overview field: {key}"
            formatted = f"¥{value:.2f}" if key == "todaySales" else f"{value:,}"
            expect(self.page.get_by_test_id(f"admin-stat-{label}").get_by_text(formatted, exact=True)).to_be_visible()
        for key in ("pendingRefundCount", "pendingOrderCount"):
            expect(self.page.get_by_test_id(f"admin-stat-{key}")).to_have_text(str(snapshot[key]))
