"""framework 的共享 fixture：环境配置、API 客户端、写入凭据、登录态。

单例代码里每个写入用例开头都有一大段相同的配置读取，
这里收编成 fixture——用例声明 write_guard 即获得管理员凭据，
声明 api_client 即获得一个用完自动 dispose 的 BookingApi 客户端，
声明 admin_page 即获得一个已登录的管理后台页面。

配置来源只有一个：config/settings.py（Python 常量，import 即用）。
"""

import os
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
    """暂存 call 阶段的结论；teardown 报告生成时把失败证据挂进 Allure。

    为什么是 makereport 的 teardown 阶段而不是 pytest_runtest_teardown：
    1. 时机够晚——fixture 已全部拆除，pytest-playwright 的 trace.zip
       已经写盘，产物文件齐了；
    2. 时机又够早——Allure 此时还在写本条用例的报告（它自己的
       stdout 附件也是这个阶段挂的），附件和链接能落到报告里。
    用 pytest_runtest_teardown 钩子会错过 Allure 的写入窗口，
    附件被静默丢弃（报告里什么都看不到），这是实测踩出来的坑。
    """
    outcome = yield
    report = outcome.get_result()
    if report.when == "call":
        item.rep_call = report
    elif report.when == "teardown":
        _attach_failed_evidence(item)


def _norm(text: str) -> str:
    """只留字母数字，用于把 nodeid 和 pytest-playwright 的产物目录名做宽松匹配。"""
    return re.sub(r"[^0-9a-z一-鿿]+", "", text.lower())


def _attach_failed_evidence(item):
    """失败用例的 trace/截图/录像/在线回放链接挂进 Allure。"""
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

    # 在线回放链接：trace.zip 的下载地址在测试运行时就能算出来——
    # 流水线会在测试跑完后把它发布到 <TRACE_PUBLIC_BASEURL>/<构建号>/<产物目录名>.zip，
    # 目录名就是这里的 artifact_dir.name。用 allure.dynamic.link 挂到用例的
    # Links 区，报告里点一下就进官方在线 Trace Viewer，不用下载再拖文件。
    # 本地跑没有 BUILD_NUMBER，跳过（本地想看用 playwright show-trace）。
    build_number = os.getenv("BUILD_NUMBER")
    if build_number:
        public_base = os.getenv("TRACE_PUBLIC_BASEURL", "http://154.12.20.250/ci-traces")
        trace_url = f"{public_base}/{build_number}/{artifact_dir.name}.zip"
        allure.dynamic.link(
            f"https://trace.playwright.dev/?trace={trace_url}",
            name="在线回放 Trace（免下载，点开直接看现场）",
        )
    print(f"\n[证据] 已挂入 Allure：{artifact_dir.name}/（trace + 截图 + 录像）")
