"""framework 的共享 fixture：环境配置、API 客户端、写入凭据、登录态。

单例代码里每个写入用例开头都有一大段相同的配置读取，
这里收编成 fixture——用例声明 write_guard 即获得管理员凭据，
声明 api_client 即获得一个用完自动 dispose 的 BookingApi 客户端，
声明 admin_page 即获得一个已登录的管理后台页面。

配置来源只有一个：config/settings.py（Python 常量，import 即用）。
"""

import pytest
from playwright.sync_api import Browser, Page, Playwright

from api.booking_api import BookingApi
from config.settings import ADMIN_PASSWORD, ADMIN_USERNAME, API_URL, WEB_URL
from pages.admin_pages import AdminLoginPage


@pytest.fixture(scope="session")
def web_url() -> str:
    return WEB_URL


@pytest.fixture(scope="session")
def api_url() -> str:
    """必须含 /api（与 lesson11 README 的约定一致）。"""
    return API_URL


@pytest.fixture
def api_client(playwright: Playwright, api_url: str) -> BookingApi:
    """一个独立的 APIRequestContext，用完自动 dispose。

    与浏览器完全隔离：不共享 Cookie/登录态——API 的身份靠显式 token，
    这正是"API 准备、UI 验证"模式能成立的前提。
    """
    context = playwright.request.new_context(base_url=f"{api_url}/")
    yield BookingApi(context)
    context.dispose()


@pytest.fixture
def write_guard() -> dict:
    """写入演示的凭据来源：config/settings.py 里的管理员账号。

    要换环境（地址/账号变了）直接改 settings.py，无需动任何测试代码。
    """
    return {
        "username": ADMIN_USERNAME,
        "password": ADMIN_PASSWORD,
    }


@pytest.fixture(scope="session")
def admin_storage_state(browser: Browser, web_url: str) -> dict:
    """整个测试会话只 UI 登录一次，把登录态（Cookie/localStorage）存成 dict。

    这就是第 10 课 storage_state 的"内存版"：不落盘成 admin.json，
    而是登录一次、把状态字典分给后面每个用例的 Context——
    每条用例得到一个"一出生就已登录"的浏览器，登录那 2 秒只花一次。

    注意：JWT 约 24 小时过期。这个 fixture 每次启动测试会话都重新登录，
    天然不会用到过期状态——内存版比文件版少一个"文件过期"的坑。
    """
    context = browser.new_context()
    page = context.new_page()
    AdminLoginPage(page, web_url).open().login(ADMIN_USERNAME, ADMIN_PASSWORD)
    state = context.storage_state()  # 导出此刻的全部登录凭证
    context.close()
    return state


@pytest.fixture
def admin_page(browser: Browser, admin_storage_state: dict, web_url: str) -> Page:
    """已在管理后台登录的页面——管理员 UI 操作用例声明即用。

    用例拿到的是加载了登录态的全新 Context 里开出的页面，
    各用例之间 Cookie 互不污染（隔离），但登录态共享（复用）。
    """
    context = browser.new_context(storage_state=admin_storage_state, base_url=web_url)
    page = context.new_page()
    yield page
    context.close()  # 显式关闭，避免每个用例泄漏一个浏览器上下文
