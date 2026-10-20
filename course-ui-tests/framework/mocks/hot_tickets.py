"""热门票接口 Mock 助手：给浏览器看"假数据"的安装器（不创建任何真实数据）。

这个文件是干什么的
------------------
用 page.route() 在**网络层**拦截浏览器发往 /api/tickets/hot 的请求，
直接塞回我们编造的响应——后端从头到尾不知情，数据库零变化。
它解决的是"真实数据造不出来"的场景：空列表（总不能把全系统的票删光）、
500 服务器错误（总不能把后端搞崩）、断网（API 层面根本没有这个操作）。

和 api/booking_api.py 的区别：那是造**真**数据（请求真实到达服务端），
这里是造**假**数据（请求被半路拦下）。一根网线的两端，谁也替代不了谁。

成员清单：每个是干什么的、被哪条用例使用
------------------------------------------
使用者是 framework/tests/test_hot_tickets_mock.py（共 5 条用例）：

| 成员 | 干什么 | 谁在用 |
| --- | --- | --- |
| `HOT_TICKETS_ROUTE` | 路由正则：精确匹配热门票接口、允许 ?limit=10 查询参数——拦错了会误伤其他接口 | 全部被测用例间接使用 |
| `booking_body()` | 把数据包成 BookingApi 的统一响应结构 {code, message, data}——假响应必须和真接口结构一致，否则测出来的前端表现是假的 | HotTicketsMock 内部 |
| `make_hot_ticket()` | 数据工厂：字段默认值齐全，用例只写自己关心的差异（如 title） | 用例 1 |
| `HotTicketsMock` | 核心安装器：注册拦截 + requested 哨兵（断言"拦截真的发生过"，否则路由写错了测试也会"绿"）；status 参数可造 500 | 用例 1（固定数据）、2（空数据）、3（500） |
| `install_abort()` | 网络层直接掐断连接——和 500 是两种故障（网络层 vs 响应层），前端走不同 catch 分支 | 用例 4（断网） |
| `install_title_editor()` | 不完全 Mock：route.fetch() 先拿**真实响应**，只改一个字段再给前端——真实链路全程参与 | 用例 5（篡改真实响应） |

三个来自实测的关键约束：
1. 路由正则不跨主机边界、允许 ?limit=10 查询参数——宽泛拦截会误伤其他接口；
2. Mock 的响应结构必须是 {code, message, data}，和真实接口一致，
   否则测出来的"前端表现"是假的；
3. 500 和 abort 是两种不同故障（响应层 vs 网络层），前端走不同 catch 分支。
"""

import json
import re

from playwright.sync_api import Page, Route

# 精确匹配热门票接口：主机部分不允许跨 ?，避免误伤其他接口；
# 末尾允许 ?limit=10 这类查询参数（前端真实请求就带着它）。
HOT_TICKETS_ROUTE = re.compile(r"^https?://[^/?#]+/api/tickets/hot(?:\?[^#]*)?$")


def booking_body(data: object) -> str:
    """把数据包进 BookingApi 的统一响应结构 {code, message, data}。"""
    return json.dumps({"code": 0, "message": "success", "data": data}, ensure_ascii=False)


def make_hot_ticket(**overrides) -> dict:
    """造一张热门票的数据，字段默认值齐全，用例只写自己关心的差异。

    数据工厂的价值：用例里不再堆 7 行字段字面量——
    `make_hot_ticket(title="xxx")` 一行说清"这张票只有标题是重点"。
    前端加了必填字段也只改这里，所有用例不受影响。
    """
    ticket = {
        "id": 900001,
        "title": "课程模拟热门票-仅此一张",
        "price": 99,
        "venue": "教学模拟场馆",
        "date": "2026-12-31",
        "category": "演唱会",
        "status": 2,  # 售票中
    }
    ticket.update(overrides)
    return ticket


class HotTicketsMock:
    """热门票接口的 Mock 安装器 + 哨兵。

    用法：

        mock = HotTicketsMock(data=[{...}]).install(page)
        page.goto(...)
        assert mock.requested, "页面应请求热门票接口"

    requested 哨兵解决单例代码里 seen_get 变量的重复：
    断言"拦截真的发生过"，否则路由写错了测试也会"绿"。
    """

    def __init__(self, data: object = None, status: int = 200) -> None:
        self._data = data
        self._status = status
        self.requested = False

    def install(self, page: Page) -> "HotTicketsMock":
        """注册路由并返回自身，方便链式写。必须先 install 再 goto。"""

        def handler(route: Route) -> None:
            if route.request.method != "GET":
                route.continue_()  # 只拦 GET，其他方法放行给真实后端
                return
            self.requested = True
            if self._status == 200:
                route.fulfill(
                    status=200,
                    content_type="application/json",
                    body=booking_body(self._data),
                )
            else:
                route.fulfill(
                    status=self._status,
                    content_type="application/json",
                    body=json.dumps(
                        {"code": self._status, "message": "Internal Server Error", "data": None},
                        ensure_ascii=False,
                    ),
                )

        page.route(HOT_TICKETS_ROUTE, handler)
        return self


def install_abort(page: Page) -> None:
    """网络层直接断掉（连接被掐断）。

    与 500（服务器回了错误响应）是两种不同故障，前端走不同的 catch 分支，
    值得分开测——所以它是独立的安装器，而不是 HotTicketsMock 的一个参数。
    """
    page.route(HOT_TICKETS_ROUTE, lambda route: route.abort())


def install_title_editor(page: Page, new_title: str) -> None:
    """不完全 Mock：route.fetch() 先拿真实响应，改第一张票的标题再给前端。

    真实链路（鉴权、数据结构、其他字段）全部参与测试，只有标题被替换；
    适合"真实数据基本可用，只想改一个字段验证前端展示"的场景。
    """

    def handler(route: Route) -> None:
        if route.request.method != "GET":
            route.continue_()
            return
        response = route.fetch()
        payload = response.json()
        assert payload.get("code") == 0 and payload["data"], "需要后端至少有一张热门票"
        payload["data"][0]["title"] = new_title
        route.fulfill(response=response, json=payload)

    page.route(HOT_TICKETS_ROUTE, handler)
