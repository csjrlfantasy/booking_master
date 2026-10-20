"""框架版 Mock 测试：四个场景，每个只剩"装什么 Mock + 断言什么"。

对照单例 test_network_mock.py / test_mock_scenarios.py：
路由正则、handler 骨架、{code,message,data} 包装、哨兵变量全部收进
mocks/hot_tickets.py；这里的每一行都在说业务。

全部只读，不创建真实数据，可直接运行：
    python -m pytest framework/tests/test_hot_tickets_mock.py -q
"""

import re
from uuid import uuid4

from playwright.sync_api import Page, expect

from mocks.hot_tickets import (
    HOT_TICKETS_ROUTE,
    HotTicketsMock,
    install_abort,
    install_title_editor,
    make_hot_ticket,
)


def test_shop_renders_one_mocked_hot_ticket(page: Page, web_url: str) -> None:
    """固定数据：页面渲染出我们 Mock 的那张票。"""
    title = "课程模拟热门票-仅此一张"
    # 数据工厂给出齐全默认字段，用例只写"这条数据的重点是什么"
    mock = HotTicketsMock(data=[make_hot_ticket(title=title)]).install(page)  # 先注册

    page.goto(f"{web_url}/shop")  # 再打开页面

    expect(page.get_by_role("heading", name="热门推荐")).to_be_visible()
    expect(page.get_by_role("button", name=re.compile(re.escape(title)))).to_be_visible()
    assert mock.requested, "页面应请求热门票接口"  # 哨兵：证明拦截真的发生过


def test_mock_empty_hot_tickets(page: Page, web_url: str) -> None:
    """空数据：热门票一张都没有，"热门推荐"区块内显示空态文案。"""
    HotTicketsMock(data=[]).install(page)

    # 用 expect_response 等"热门票请求已完成"再断言（呼应第 7 课），
    # 避免页面慢加载时断言计时和渲染抢时间造成偶发失败。
    with page.expect_response(HOT_TICKETS_ROUTE) as resp_info:
        page.goto(f"{web_url}/shop")
    assert resp_info.value.ok, "被拦截的 Mock 响应本身应是 200"

    expect(page.get_by_role("heading", name="热门推荐")).to_be_visible()
    # 两个实测踩出来的坑（详见单例 test_mock_scenarios.py 的注释）：
    # 1. 页面多个区块都会显示"暂无票务"，裸文本定位撞 strict mode；
    # 2. shop-empty-result 属于"必看演出"区块，断言它属于断言错了对象。
    # 正确做法：用 has= 把范围缩到"热门推荐"所在 section 内部（呼应第 5 课）。
    hot_section = page.locator("section", has=page.get_by_role("heading", name="热门推荐"))
    expect(hot_section.get_by_text("暂无票务")).to_be_visible()


def test_mock_server_error_500(page: Page, web_url: str) -> None:
    """500：实测发现本项目前端静默降级——不报错，区块仍渲染。

    断言写的是真实观察到的行为（区块仍在 + Mock 哨兵不出现），
    而不是想当然地断言一个并不存在的"加载失败"提示。
    """
    sentinel = f"课程模拟-不应出现-{uuid4().hex[:6]}"
    HotTicketsMock(status=500).install(page)

    page.goto(f"{web_url}/shop")

    expect(page.get_by_role("heading", name="热门推荐")).to_be_visible()
    expect(page.get_by_text(sentinel)).not_to_be_visible()


def test_mock_network_failure_abort(page: Page, web_url: str) -> None:
    """断网：route.abort() 是网络层故障，与 500 走前端不同的 catch 分支。"""
    install_abort(page)

    page.goto(f"{web_url}/shop")

    # 与 500 相同：本项目前端对网络失败同样静默降级
    expect(page.get_by_role("heading", name="热门推荐")).to_be_visible()


def test_modify_real_response_title(page: Page, web_url: str) -> None:
    """进阶：不完全 Mock——真实响应改一个字段再给前端。"""
    unique_title = f"课程篡改标题-{uuid4().hex[:8]}"
    install_title_editor(page, unique_title)

    page.goto(f"{web_url}/shop")

    expect(
        page.get_by_role("button", name=re.compile(re.escape(unique_title)))
    ).to_be_visible()
