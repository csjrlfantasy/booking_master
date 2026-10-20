"""【课堂演示用】受控失败用例：由 Jenkins 参数 DEMO_FAILURE 决定是否参与运行。

演示流程：Build with Parameters 勾选 DEMO_FAILURE → 构建变红 →
飞书收到 FAILURE → Allure 失败用例下载 Trace → 或点构建描述里的链接在线回放。

用例本身是一条完整的买家浏览动线（首页 → 热门推荐 → 点进详情 → 滚动页面），
录屏和 Trace 里能看到真实操作过程；最后一步故意断言错误标题，触发失败证据链。

平时默认跳过，不影响全绿构建。
"""

import os
import re

import pytest
from playwright.sync_api import Page, expect


@pytest.mark.skipif(
    os.getenv("COURSE_DEMO_FAILURE") != "true",
    reason="演示失败开关未打开（Jenkins 参数 DEMO_FAILURE / 环境变量 COURSE_DEMO_FAILURE）",
)
def test_demo_failure_for_trace_evidence(page: Page, web_url: str):
    """买家浏览动线 + 故意错误的断言结尾。"""
    # 第一步：打开商城首页，确认"热门推荐"区块加载出来
    page.goto(f"{web_url}/shop")
    expect(page.get_by_role("heading", name="热门推荐")).to_be_visible()

    # 第二步：点开热门推荐里的第一张票（卡片是可点按钮），进详情页
    hot_section = page.locator("section", has=page.get_by_role("heading", name="热门推荐"))
    hot_section.get_by_role("button").first.click()
    expect(page.get_by_role("heading", level=1)).to_be_visible()  # 详情页主标题出现

    # 第三步：滚动页面看看票价、评论区（让录屏/Trace 里有连续的浏览动作）
    page.mouse.wheel(0, 600)
    page.wait_for_timeout(500)
    page.mouse.wheel(0, -300)
    page.wait_for_timeout(500)

    # 第四步：故意断言一个不存在的标题——真实标题是"票多星，不卖黄牛票"。
    # 这里失败，conftest 的钩子会把 trace.zip / 截图 / 录像挂进 Allure，
    # 流水线再把 trace.zip 发布到 nginx，构建描述里生成在线回放链接。
    expect(page).to_have_title(re.compile("这是一场不存在的演唱会"))
