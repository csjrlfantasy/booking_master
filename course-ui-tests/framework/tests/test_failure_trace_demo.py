"""【课堂演示用】故意失败的用例：让 CI 红一次，看 Allure 里的 Trace 三件套。

演示流程：构建变红 → 飞书收到 FAILURE 通知 → 打开 Allure 报告 →
失败用例下面直接下载 Trace 回放包 → 拖到 trace.playwright.dev 逐帧看现场。

⚠️ 演示完删除本文件（或等仓库里的移除提交），否则之后每次构建都会红。
"""

import re

from playwright.sync_api import Page, expect


def test_demo_failure_for_trace_evidence(page: Page, web_url: str):
    """打开票多星首页，故意断言一个不存在的标题。"""
    page.goto(web_url)
    # 真实标题是"票多星，不卖黄牛票"——这里故意写错，expect 超时后失败，
    # 失败后 conftest 的钩子会把 trace.zip / 截图 / 录像挂进 Allure
    expect(page).to_have_title(re.compile("这是一个不存在的标题"))
