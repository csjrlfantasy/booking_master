"""【课堂演示用】受控失败用例：由 Jenkins 参数 DEMO_FAILURE 决定是否参与运行。

演示流程：Build with Parameters 勾选 DEMO_FAILURE → 构建变红 →
飞书收到 FAILURE → Allure 失败用例下载 Trace → 或点构建描述里的链接在线回放。

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
    """打开票多星首页，故意断言一个不存在的标题。"""
    page.goto(web_url)
    # 真实标题是"票多星，不卖黄牛票"——这里故意写错，expect 超时后失败，
    # 失败后 conftest 的钩子会把 trace.zip / 截图 / 录像挂进 Allure
    expect(page).to_have_title(re.compile("这是一个不存在的标题"))
