"""API 写评论 → 买家 UI 详情页可见（又一款"API 准备 → UI 验证"）。

这是真实数据演示：评论通过真实接口写入数据库，
UI 断言看到的是服务端渲染出来的真数据，不是 Mock。

教学要点
--------
1. "API 准备"不只限于造票造单——任何有写入接口的资源都可以这样准备，
   浏览器只负责最后一步"用户真的能看到吗"；
2. 评论接口需要用户身份（Bearer token），又一次用到
   "注册 → 接口登录拿 token"这条熟路；
3. 清理只删票和用户：评论挂在票上，票被 test-support/cleanup 删除后
   评论区即不可见。

数据说明：票、用户、评论全部真实创建，finally 里真实清理
（需在 config/settings.py 填入 COURSE_TEST_KEY，留空时退化为演示模式）。
"""

from uuid import uuid4

from playwright.sync_api import Page

from api.booking_api import BookingApi, make_phone
from pages.shop_pages import TicketDetailPage


def test_api_posted_comment_shows_on_ticket_page(
    page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    run_id = uuid4().hex[:12]

    # ── API 准备：票 + 用户 + 评论（浏览器还没打开）────────
    admin_token = api_client.admin_login(write_guard["username"], write_guard["password"])
    ticket_id = api_client.create_ticket(
        admin_token,
        name=f"pw-course-{run_id}-comment",
        description="第 11 课框架版：API 写评论 → UI 验证演示数据",
    )

    username = f"pw-course-{run_id}-user"
    user_id = api_client.register_user(username, "Course@123456", make_phone(run_id))
    user_token = api_client.user_login(username, "Course@123456")

    # 评论内容带 runId：既是归属标记，也让断言的目标文本全局唯一
    content = f"pw-course-{run_id} 这条评论是 API 直接写入的"
    api_client.create_comment(user_token, ticket_id, content, rating=5)

    try:
        # ── UI 验证：任何人打开详情页都应看到这条评论 ────────
        TicketDetailPage(page, web_url).open(ticket_id).expect_comment(content)
    finally:
        api_client.cleanup(run_id, ticket_ids=[ticket_id], user_ids=[user_id])
