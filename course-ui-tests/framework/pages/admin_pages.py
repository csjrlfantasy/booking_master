"""管理后台的页面对象：登录页、订单管理页、订单详情页。

定位器全部用语义角色 / test-id / 占位符，不碰 CSS 类名——
前端改样式不会震碎测试，改文案才会（那本来就该红）。
"""

import re

from playwright.sync_api import Page, expect


class AdminLoginPage:
    """管理员登录页。整个测试会话只真正登录一次（见 conftest 的 admin_storage_state）。"""

    def __init__(self, page: Page, base_url: str) -> None:
        self.page = page
        self.base_url = base_url

    def open(self) -> "AdminLoginPage":
        self.page.goto(f"{self.base_url}/login")
        return self

    def login(self, username: str, password: str) -> None:
        # 表单用 test-id 定位：最稳定，与文案、样式都解耦
        self.page.get_by_test_id("admin-username").fill(username)
        self.page.get_by_test_id("admin-password").fill(password)
        self.page.get_by_test_id("admin-login-submit").click()
        # 登录成功的标志是跳回后台首页；等 URL 而不是固定 sleep
        self.page.wait_for_url(f"{self.base_url}/")


class AdminOrdersPage:
    """订单管理列表页。"""

    def __init__(self, page: Page, base_url: str) -> None:
        self.page = page
        self.base_url = base_url

    def open(self) -> "AdminOrdersPage":
        self.page.goto(f"{self.base_url}/orders")
        return self

    def search(self, order_no: str) -> None:
        """按订单号搜索——避免翻页和排序的干扰。"""
        self.page.get_by_placeholder("搜索订单号...").fill(order_no)
        self.page.get_by_role("button", name="筛选").click()

    def row(self, order_no: str):
        """定位某一订单行，供用例做行内断言。"""
        return self.page.get_by_role("row", name=re.compile(order_no))

    def cancel_order(self, order_no: str) -> None:
        """取消一笔订单：行菜单 → 取消订单 → confirm 对话框 → toast。

        两个实测出来的页面知识都收在这里：
        1. 浏览器原生 confirm() 对话框要先注册自动接受，再触发点击；
        2. 操作列是"更多"图标菜单，菜单项是 <button role="menuitem">。
        """
        self.page.on("dialog", lambda dialog: dialog.accept())
        self.row(order_no).get_by_role("button").first.click()
        self.page.get_by_role("menuitem", name="取消订单").click()
        expect(self.page.get_by_text("取消成功")).to_be_visible()


class AdminOrderDetailPage:
    """订单详情页（管理后台视角）。"""

    def __init__(self, page: Page, base_url: str) -> None:
        self.page = page
        self.base_url = base_url

    def open(self, order_id: int) -> "AdminOrderDetailPage":
        self.page.goto(f"{self.base_url}/orders/{order_id}")
        return self

    def expect_amounts(self, total: str, paid: str) -> None:
        """金额断言入口：订单金额与实付金额。

        两个实测出来的坑（值得一讲）：
        1. "优惠金额"行只在 discountAmount>0 时渲染，纯券优惠记的是
           couponAmount——照抄 API 字段名会踩空；
        2. 管理端订单详情接口不返回券字段，页面渲染不出"优惠券"行——
           券明细要用用户视角的 API 断言核实，UI 这里只验证最终金额。
        """
        expect(self.page.get_by_text("订单金额")).to_be_visible()
        expect(self.page.get_by_text(total).first).to_be_visible()
        expect(self.page.get_by_text("实付金额")).to_be_visible()
        expect(self.page.get_by_text(paid)).to_be_visible()
