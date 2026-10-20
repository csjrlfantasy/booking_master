"""框架版：API 造票 → UI 验证 → API 清理（对照单例 test_api_ui_flow.py）。

单例里近 100 行，框架版只剩骨架——接口细节全在 BookingApi 里：
    登录拿 token、创建票务的默认字段、{code,message,data} 解包，
    都不再用例层操心。

写入演示：直接运行即可；凭据在 config/settings.py（write_guard 读取）。
"""

from uuid import uuid4

from playwright.sync_api import Page

from api.booking_api import BookingApi
from pages.shop_pages import TicketDetailPage


def test_api_prepares_ticket_then_ui_shows_it(
    page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    credentials = write_guard
    run_id = uuid4().hex[:12]
    ticket_name = f"pw-course-{run_id}-framework"

    # ── API Prepare（接口细节全在客户端里）──────────────────
    token = api_client.admin_login(credentials["username"], credentials["password"])
    ticket_id = api_client.create_ticket(
        token, name=ticket_name, description="框架版：API 准备、UI 验证演示数据"
    )
    # 先拿到 ID，再执行任何可能失败的浏览器断言——finally 里才有东西可清

    try:
        # ── UI Verify（页面操作细节在 POM 里）────────────────
        TicketDetailPage(page, web_url).open(ticket_id).expect_title(ticket_name)
    finally:
        # ── API 清理（断言挂了也要清理；已配密钥则真实删除）──
        api_client.cleanup(run_id, ticket_ids=[ticket_id])
