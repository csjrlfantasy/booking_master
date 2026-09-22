"""Coupon forms have associated labels; all writes get persisted API readback."""

from urllib.parse import urlsplit
from playwright.sync_api import expect


class CouponPage:
    def __init__(self, page):
        self.page = page
        self.table = page.get_by_test_id("admin-coupon-table")

    def open(self):
        self.page.goto("/coupons")
        expect(self.page.get_by_role("heading", name="优惠券管理", exact=True)).to_be_visible()

    def row(self, name):
        return self.table.get_by_role("row").filter(has=self.page.get_by_text(name, exact=True))

    def search(self, name):
        self.page.get_by_placeholder("搜索优惠券名称", exact=True).fill(name)
        self.page.get_by_role("button", name="筛选", exact=True).click()
        expect(self.row(name)).to_be_visible()

    def create(self, name, registry):
        self.page.get_by_label("优惠券名称", exact=True).fill(name)
        self.page.get_by_label("券类型", exact=True).select_option("3")
        self.page.get_by_label("券值", exact=True).fill("5")
        self.page.get_by_label("最低消费", exact=True).fill("0")
        self.page.get_by_label("总库存", exact=True).fill("10")
        self.page.get_by_label("有效期规则", exact=True).select_option("2")
        self.page.get_by_label("有效天数", exact=True).fill("7")
        self.page.get_by_label("使用说明", exact=True).fill(name)
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/admin/coupons") and r.request.method == "POST"
        ) as created:
            self.page.get_by_role("button", name="创建优惠券", exact=True).click()
        response = created.value
        payload = response.json()
        coupon_id = payload.get("data")
        if type(coupon_id) is int and coupon_id > 0:
            registry.add_coupon(coupon_id)
        if not response.ok or payload.get("code") != 0 or type(coupon_id) is not int or coupon_id <= 0:
            raise RuntimeError("admin coupon creation failed")
        return coupon_id

    def change_value(self, coupon_id, name, value):
        self.row(name).get_by_role("button", name="编辑", exact=True).click()
        expect(self.page.get_by_role("heading", name="编辑优惠券", exact=True)).to_be_visible()
        self.page.get_by_label("券值", exact=True).fill(str(value))
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/coupons/{coupon_id}") and r.request.method == "PUT"
        ) as response:
            self.page.get_by_role("button", name="保存优惠券修改", exact=True).click()
        self._success(response.value)
        expect(self.page.get_by_role("heading", name="新建优惠券", exact=True)).to_be_visible()

    def end(self, coupon_id, name):
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/coupons/{coupon_id}/status") and r.request.method == "PUT"
        ) as response:
            self.row(name).get_by_role("button", name="结束", exact=True).click()
        self._success(response.value)
        expect(self.row(name).get_by_text("已结束", exact=True)).to_be_visible()

    def issue(self, coupon_id, name, user_id):
        self.row(name).get_by_role("button", name="发券", exact=True).click()
        expect(self.page.get_by_text(f"当前：{name}", exact=True)).to_be_visible()
        self.page.get_by_label("用户ID", exact=True).fill(str(user_id))
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith(f"/admin/coupons/{coupon_id}/issue") and r.request.method == "POST"
        ) as response:
            self.page.get_by_role("button", name="发送优惠券", exact=True).click()
        self._success(response.value)
        expect(self.page.get_by_role("status").get_by_text("定向发券成功", exact=True)).to_be_visible()

    @staticmethod
    def _success(response):
        if not response.ok or response.json().get("code") != 0:
            raise RuntimeError("admin coupon mutation failed")
