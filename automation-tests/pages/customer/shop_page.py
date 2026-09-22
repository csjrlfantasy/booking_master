"""Catalog browsing with semantic locators and the existing catalog test IDs."""

import re
from urllib.parse import parse_qs, urlsplit

from playwright.sync_api import expect


class ShopPage:
    def __init__(self, page):
        self.page = page
        self.grid = page.get_by_test_id("shop-ticket-grid")

    def open(self):
        self.page.goto("/shop")
        expect(self.page.get_by_placeholder("搜索明星、演出、体育赛事")).to_be_visible()

    def search(self, keyword):
        self.page.get_by_placeholder("搜索明星、演出、体育赛事").fill(keyword)
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/tickets")
            and parse_qs(urlsplit(r.url).query).get("keyword") == [keyword]
        ) as result:
            self.page.get_by_role("button", name="搜索", exact=True).click()
        assert result.value.ok, "catalog search failed"

    def expect_ticket(self, title):
        card = self.grid.get_by_role("button").filter(has=self.page.get_by_text(title, exact=True))
        expect(card).to_have_count(1)
        expect(card).to_be_visible()

    def open_ticket(self, ticket_id, title):
        self.grid.get_by_role("button").filter(has=self.page.get_by_text(title, exact=True)).click()
        expect(self.page).to_have_url(re.compile(rf"/shop/tickets/{ticket_id}$"))
        expect(self.page.get_by_role("heading", name=title, exact=True, level=1)).to_be_visible()

    def open_detail(self, ticket_id, title):
        self.page.goto(f"/shop/tickets/{ticket_id}")
        expect(self.page.get_by_role("heading", name=title, exact=True, level=1)).to_be_visible()

    def select_category(self, category_id, display_name):
        with self.page.expect_response(
            lambda r: urlsplit(r.url).path.endswith("/tickets")
            and parse_qs(urlsplit(r.url).query).get("categoryId") == [str(category_id)]
        ) as result:
            self.page.get_by_role("button", name=display_name, exact=True).click()
        assert result.value.ok, "category filter failed"
        expect(self.page.get_by_role("heading", name="分类结果", exact=True)).to_be_visible()

    def expect_empty(self):
        expect(self.page.get_by_test_id("shop-empty-result")).to_have_text("暂无票务")
        expect(self.grid).to_have_count(0)

    def expect_anonymous(self):
        expect(self.page.get_by_role("button", name="登录", exact=True)).to_be_visible()

    def expect_authenticated(self, name):
        expect(self.page.get_by_role("button", name=name, exact=True)).to_be_visible()

    def open_orders(self, customer_name):
        # Desktop header opens the profile; the mobile navigation opens orders directly.
        if self.page.viewport_size and self.page.viewport_size["width"] < 768:
            self.page.get_by_role("button", name="订单", exact=True).click()
        else:
            self.page.get_by_role("button", name=customer_name, exact=True).click()
            self.page.get_by_role("button", name=re.compile(r"^我的订单")).click()

    def expect_mobile_navigation(self):
        expect(self.page.get_by_role("button", name="订单", exact=True)).to_be_visible()
        expect(self.page.get_by_role("button", name="我的", exact=True)).to_be_visible()
