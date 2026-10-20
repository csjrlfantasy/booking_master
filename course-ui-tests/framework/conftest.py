"""framework 的共享 fixture：环境配置、API 客户端、写入凭据、登录态。

单例代码里每个写入用例开头都有一大段相同的配置读取，
这里收编成 fixture——用例声明 write_guard 即获得管理员凭据，
声明 api_client 即获得一个用完自动 dispose 的 BookingApi 客户端，
声明 admin_page 即获得一个已登录的管理后台页面。

配置来源只有一个：config/settings.py（Python 常量，import 即用）。
"""

import re
from pathlib import Path

import allure
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


# ── 失败证据：自动挂进 Allure（第 13 课证据链）──────────────
# Allure 负责"哪条挂了、整体通过率"，Trace 负责"这条到底怎么挂的"。
# 下面两个钩子把失败用例的 trace.zip / 截图 / 录像挂到 Allure 报告对应
# 用例下面——报告页直接下载 trace.zip，拖到 https://trace.playwright.dev
# 就能逐帧回放 DOM、网络请求、console 日志，不用登服务器翻文件。


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_makereport(item):
    """暂存 call 阶段的结论，teardown 阶段据此判断要不要留证据。"""
    outcome = yield
    report = outcome.get_result()
    if report.when == "call":
        item.rep_call = report


def _norm(text: str) -> str:
    """只留字母数字，用于把 nodeid 和 pytest-playwright 的产物目录名做宽松匹配。"""
    return re.sub(r"[^0-9a-z一-鿿]+", "", text.lower())


@pytest.hookimpl(hookwrapper=True)
def pytest_runtest_teardown(item):
    """失败用例的 trace/截图/录像挂进 Allure。

    为什么放在 teardown：pytest-playwright 要等浏览器上下文关闭
    （fixture 收尾）才把 trace.zip 写盘；hookwrapper 的 yield 之后，
    本用例的 fixture 都已拆除，产物文件齐了。
    """
    yield
    report = getattr(item, "rep_call", None)
    if report is None or not report.failed:
        return  # PASS 不留证据：全挂的话磁盘和报告都吃不消

    # pytest-playwright 按用例名建目录（test-results/<nodeid 变体>/），
    # 找到本用例的产物目录；找不到说明是纯 API 用例，没有浏览器产物，跳过
    target = _norm(item.nodeid)
    candidates = sorted(
        (
            p
            for p in Path("test-results").rglob("trace.zip")
            if target.endswith(_norm(p.parent.name)) and p.stat().st_mtime >= report.start - 1
        ),
        key=lambda p: p.stat().st_mtime,
    )
    if not candidates:
        print(f"\n[证据] {item.name} 没有浏览器产物（纯 API 用例属正常）")
        return
    artifact_dir = candidates[-1].parent

    allure.attach.file(
        str(artifact_dir / "trace.zip"),
        name="Trace 回放包（下载后拖到 trace.playwright.dev 逐帧看现场）",
        extension="zip",
    )
    # pytest-playwright 失败截图命名为 test-failed-1.png（多页面对话框会递增）
    for screenshot in sorted(artifact_dir.glob("*.png")):
        allure.attach.file(str(screenshot), name="失败现场截图", extension="png")
    for video in sorted(artifact_dir.glob("video*.webm")):
        allure.attach.file(str(video), name="操作过程录像", extension="webm")
    print(f"\n[证据] 已挂入 Allure：{artifact_dir.name}/（trace + 截图 + 录像）")
