"""Banner：真实创建 → /shop 首页可见 → 真实删除（不依赖 test-support）。

教学要点
--------
1. 又一款"API 准备 → UI 验证"：Banner 由管理接口真实创建，
   买家首页的轮播大图（h1 标题）真实渲染出来；
2. **清理方式的对比**：票/订单/用户要靠后端专门的 test-support/cleanup
   接口清理，而 Banner 这类后台配置型资源自带公开的 DELETE 接口——
   有公开删除接口的资源，用它清理是最自然的选择；
3. finally 里删除：即使 UI 断言失败，Banner 也不会留在首页上。

数据说明：Banner 真实创建、真实删除，全程无残留。
"""

from uuid import uuid4

from playwright.sync_api import Page, expect

from api.booking_api import BookingApi


def test_api_created_banner_shows_on_shop_home(
    page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    run_id = uuid4().hex[:12]
    admin_token = api_client.admin_login(write_guard["username"], write_guard["password"])

    title = f"pw-course-{run_id}-banner"
    # 图片用占位图服务：img 加载成功与否不影响标题渲染
    banner_id = api_client.create_banner(
        admin_token, title, image_url="https://picsum.photos/seed/course/1200/400"
    )

    try:
        page.goto(f"{web_url}/shop")
        # 首页轮播当前帧的 Banner 标题渲染为 h1；
        # 新 Banner 排在列表最前（接口按创建时间倒序），打开即当前帧。
        expect(page.get_by_role("heading", level=1, name=title)).to_be_visible()
    finally:
        api_client.delete_banner(admin_token, banner_id)
