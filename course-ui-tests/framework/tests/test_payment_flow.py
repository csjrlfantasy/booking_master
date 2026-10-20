"""框架版支付全链路：API 造"已支付"状态 → UI 验证 → API 复核 → 真实清理。

对照单例 test_mock_pay_flow.py（210 行）：接口样板全部收进 BookingApi
和 admin_page 夹具，这里只剩业务主线，每一行都在讲流程。

教学要点（本例的核心问题）
------------------------
"想验证已支付订单的展示，难道每次都要真的走一遍支付吗？"
——不用。第三方支付是**外部依赖**，测试环境不碰真实支付渠道；后端提供了
沙箱接口 POST /payment/mock-pay，把订单状态机真实推进到"已支付"（写数据库），
秒级完成。注意区分两层"Mock"：

- mock-pay：服务端的假支付，真实写库、真实流转状态（本例用它）；
- page.route：Playwright 在网络层拦截，服务端完全不知情（见 test_hot_tickets_mock.py）。

数据说明：票、用户、订单全部真实创建，finally 里经 test-support/cleanup
真实删除（需在 config/settings.py 填入 COURSE_TEST_KEY，留空时退化为演示模式）。
"""

from uuid import uuid4

from playwright.sync_api import Page, expect

from api.booking_api import BookingApi, make_phone
from pages.admin_pages import AdminOrdersPage


def test_paid_order_shows_in_admin_ui(
    admin_page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    run_id = uuid4().hex[:12]

    # ── 第一步：API 准备完整业务状态（浏览器全程未动）─────
    admin_token = api_client.admin_login(write_guard["username"], write_guard["password"])
    ticket_id = api_client.create_ticket(
        admin_token,
        name=f"pw-course-{run_id}-pay",
        description="第 11 课框架版：Mock 支付全链路演示数据",
        price=99,
    )

    username = f"pw-course-{run_id}-user"  # 带 runId 前缀：清理接口按此前缀校验归属
    phone = make_phone(run_id)
    user_id = api_client.register_user(username, "Course@123456", phone)
    user_token = api_client.user_login(username, "Course@123456")

    order = api_client.create_order(user_token, ticket_id, run_id, phone)
    order_id, order_no = order["orderId"], order["orderNo"]
    # 三个 ID 全部登记完毕，再做任何可能失败的操作——finally 里才有东西可清

    # Mock 支付：必须显式调用——没有这一步订单永远是"待支付"。
    api_client.mock_pay(user_token, order_no, amount=99)

    try:
        # ── 第二步：UI 验证（admin_page 进来就是已登录的后台）──────
        orders = AdminOrdersPage(admin_page, web_url)
        orders.open().search(order_no)

        row = orders.row(order_no)
        expect(row).to_be_visible()
        expect(row.get_by_text("已支付")).to_be_visible()

        # ── 第三步：API 复核（UI 文案之外，支付网关视角再查一次）──
        pay_status = api_client.payment_status(user_token, order_no)
        assert pay_status["status"] == "SUCCESS"
        assert pay_status["statusText"] == "支付成功"
    finally:
        # ── 第四步：真实清理（断言挂了也要执行）─────────────
        api_client.cleanup(
            run_id,
            ticket_ids=[ticket_id], order_ids=[order_id], user_ids=[user_id],
        )
