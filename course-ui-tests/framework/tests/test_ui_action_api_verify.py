"""框架版：UI Action → API Verify（对照单例 test_ui_action_api_verify.py）。

链路：API 准备（造票 + 注册用户 + 下单）
    → 管理员 UI 取消订单
    → API 核实 orderStatus == 4（已取消）
    → API 清理

单例 220 行里约 150 行是接口调用样板；框架版把它们交给 BookingApi，
留下来的就是要讲的主线：toast 只证明前端显示了提示，服务端状态必须 API 复核。

写入演示：直接运行即可；凭据在 config/settings.py（write_guard 读取）。
"""

from uuid import uuid4

from playwright.sync_api import Page

from api.booking_api import BookingApi, make_phone
from pages.admin_pages import AdminOrdersPage


def test_admin_cancels_order_in_ui_then_api_confirms(
    admin_page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    credentials = write_guard
    run_id = uuid4().hex[:12]

    # ── 第一步：API Prepare ──────────────────────────────────
    admin_token = api_client.admin_login(credentials["username"], credentials["password"])
    ticket_id = api_client.create_ticket(
        admin_token,
        name=f"pw-course-{run_id}-framework-order",
        description="第 11 课框架版：UI Action → API Verify 演示数据",
    )

    # 注册本次专用用户（用户名必须带 pw-course-{runId}- 前缀，否则清理被拒绝）
    username = f"pw-course-{run_id}-user"
    phone = make_phone(run_id)  # 每次运行唯一
    user_id = api_client.register_user(username, "Course@123456", phone)
    user_token = api_client.user_login(username, "Course@123456")

    order = api_client.create_order(user_token, ticket_id, run_id, phone)
    order_id, order_no = order["orderId"], order["orderNo"]
    # 三个 ID 全部登记完毕，再做任何可能失败的 UI 操作

    try:
        # ── 第二步：UI Action（管理员在后台取消这笔订单）──────
        # admin_page 夹具已带登录态；页面操作细节全部在 POM 里
        orders = AdminOrdersPage(admin_page, web_url)
        orders.open().search(order_no)
        orders.cancel_order(order_no)  # 内部已含 confirm 对话框与"取消成功"toast 断言

        # ── 第三步：API Verify（服务端状态真的变了吗？）──────
        # toast 只证明前端显示了提示；真实状态必须 API 复核
        order_after = api_client.get_order(order_id, user_token)
        assert order_after["orderStatus"] == 4, (
            f"UI 显示取消成功，但服务端 orderStatus={order_after['orderStatus']}（期望 4=已取消）"
        )
        assert order_after["orderStatusText"] == "已取消"
    finally:
        # ── 第四步：清理（票 + 订单 + 用户；已配密钥则真实删除）──
        api_client.cleanup(
            run_id,
            ticket_ids=[ticket_id], order_ids=[order_id], user_ids=[user_id],
        )
