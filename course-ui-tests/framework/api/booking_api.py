"""BookingApi 客户端：本课所有"造数据 / 查数据 / 清数据"的唯一入口。

这个文件是干什么的
------------------
测试需要的数据（票、用户、订单、券、评论、Banner……）全部通过**接口**
真实创建——不开浏览器、不走 UI。UI 只出现在测试的"验证"环节，
造数据永远走 API：快、稳、不依赖页面细节。这就是第 11 课的核心原则：
**API 准备数据，UI 只做关键验证。**

和 mocks/ 目录有什么区别（学员最常问）
--------------------------------------
两者是一根网线的两端，方向相反：

- 本文件（api/）：请求**真实到达**服务端，数据库真实变化；
- mocks/：请求被 Playwright 在网络层**拦下伪造**，服务端毫无知觉。

判断一个新方法该放哪：会让数据库变化的 → 本文件；
只是给浏览器看假象的 → mocks/。

怎么读这个文件
--------------
1. 先看文件下面的 `unwrap()`：全项目统一的响应结构是 {code, message, data}，
   HTTP 200 ≠ 业务成功，所以每个接口调用都要过两层断言——这个函数就是
   全课反复强调的"两层断言"的唯一实现，所有方法都经过它；
2. 再看类的设计约定：**不持有 token**。每个方法显式接收 token 参数——
   谁在调用、用什么身份（管理员还是买家），在调用处一目了然，
   身份切换出错一眼就能发现；
3. 然后按注释区块找你要的方法：账号 / 票务 / 订单 / 支付沙箱 /
   评论 / Banner / 优惠券 / 用户资产 / 购物车 / 秒杀 / 清理。

造数据的通用三步（每个 create_* 方法都在执行）
----------------------------------------------
① 调接口创建 → ② 立刻拿到并登记返回的 ID → ③ finally 里按 ID 清理。
名字统一带 pw-course-{runId}- 前缀，标记"这是哪次测试运行的数据"，
清理接口据此校验归属，防止误删别人的数据。

每个接口"为什么这么调"（哪个字段是文档写的、哪个是实测踩出来的），
讲稿第三节"造数据的接口清单"有逐条说明；在线接口文档：
http://127.0.0.1:8088/api/swagger-ui/index.html（机读 JSON：/api/v3/api-docs）。
"""

from datetime import date, timedelta

from playwright.sync_api import APIRequestContext, APIResponse

from config.settings import COURSE_TEST_KEY


def unwrap(response: APIResponse) -> object:
    """统一响应解包：先 HTTP 层，再业务层。

    BookingApi 的响应结构是 {code, message, data}——
    HTTP 200 ≠ 业务成功，两层都要断言，这是全课反复强调的一点。
    """
    assert response.ok, f"HTTP 请求失败：{response.status}，响应体：{response.text()[:500]}"
    payload = response.json()
    assert isinstance(payload, dict) and payload.get("code") == 0, (
        f"BookingApi 业务响应失败：{payload}"
    )
    return payload["data"]


def make_idcard() -> str:
    """生成一个通过校验位验证的演示身份证号。

    接口文档没写这条规则，是实测创建订单被 100600 拒绝后才发现的：
    观演人 idCardNo 必须是合法身份证号（含校验位）。
    教学点：**接口文档不一定完整，以服务的实际行为为准。**
    """
    base = "11010119900101123"
    weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
    codes = "10X98765432"
    return base + codes[sum(int(b) * w for b, w in zip(base, weights)) % 11]


def make_phone(run_id: str) -> str:
    """由 runId 派生一个每次运行唯一的手机号（1 开头 11 位）。"""
    return "139" + f"{int(run_id, 16) % 10**8:08d}"


class BookingApi:
    """票多星 BookingApi 的测试专用客户端。

    只包一层薄薄的"接口知识"：URL、字段名、鉴权头、两层断言。
    业务编排（先造什么再造什么）留在测试里——客户端越薄，测试越好读。

    设计约定：不持有 token，每个方法显式接收（见文件头"怎么读"第 2 点）。
    """

    def __init__(self, context: APIRequestContext) -> None:
        # APIRequestContext 由 conftest.py 的 api_client 夹具创建（用完自动 dispose），
        # 与浏览器完全隔离——API 的身份靠显式 token，不共享任何 Cookie。
        self._context = context

    @staticmethod
    def _auth(token: str) -> dict:
        """拼 Bearer 鉴权头。本项目是 Token 鉴权（不是 Cookie），每个需要登录的接口都带它。"""
        return {"Authorization": f"Bearer {token}"}

    # ---------- 账号 ----------

    def admin_login(self, username: str, password: str) -> str:
        """管理员登录，返回 Bearer token（本项目是 Token 鉴权，不是 Cookie）。"""
        data = unwrap(
            self._context.post("admin/login", data={"username": username, "password": password})
        )
        assert isinstance(data, dict) and isinstance(data.get("token"), str)
        return data["token"]

    def register_user(self, username: str, password: str, phone: str) -> int:
        """注册真实用户，返回用户 ID。

        注意：清理接口按"用户名以 pw-course-{runId}- 开头"校验归属，
        调用方传入的 username 必须带此前缀，否则清理会被拒绝。
        """
        data = unwrap(
            self._context.post(
                "user/register",
                data={"username": username, "password": password, "phone": phone},
            )
        )
        assert isinstance(data, int) and data > 0
        return data

    def user_login(self, username: str, password: str) -> str:
        data = unwrap(
            self._context.post("user/login", data={"username": username, "password": password})
        )
        assert isinstance(data, dict) and isinstance(data.get("token"), str)
        return data["token"]

    # ---------- 票务 ----------

    def create_ticket(
        self,
        token: str,
        name: str,
        description: str = "API 准备的测试数据",
        show_time: str = "19:30",
        price: int = 99,
        stock: int = 10,
        status: int = 2,  # 售票中
    ) -> int:
        """创建一张票务，返回票务 ID。演出日期默认 60 天后，避免撞上历史数据。"""
        data = unwrap(
            self._context.post(
                "admin/tickets",
                headers=self._auth(token),
                data={
                    "name": name,
                    "description": description,
                    "showDate": (date.today() + timedelta(days=60)).isoformat(),
                    "showTime": show_time,
                    "priceMin": price,
                    "priceMax": price,
                    "totalStock": stock,
                    "status": status,
                },
            )
        )
        assert type(data) is int and data > 0, "创建接口必须返回票务 ID"
        return data

    def ticket_detail(self, ticket_id: int) -> dict:
        """公开的票务详情接口（无需鉴权）。下单需要的场次 ID、票档 ID 从这里读。"""
        data = unwrap(self._context.get(f"tickets/{ticket_id}"))
        assert isinstance(data, dict)
        return data

    # ---------- 订单 ----------

    def create_order(
        self,
        user_token: str,
        ticket_id: int,
        run_id: str,
        contact_phone: str,
        user_coupon_id: int | None = None,
    ) -> dict:
        """以用户身份下单（待支付），返回 {orderId, orderNo}。

        两个实测强制、文档未写的规则（与单例代码一致）：
        1. 每个订单项必须带 attendees 观演人（每票一人），且身份证过校验位；
        2. userRemark 必须带 pw-course-{runId}- 前缀，清理接口据此校验订单归属。

        user_coupon_id：用户已领取的券实例 ID（不是券模板 ID）——
        从 my_coupons() 的返回里取 id 字段。
        """
        detail = self.ticket_detail(ticket_id)
        session_id = detail["sessions"][0]["id"]
        price_level = detail["priceLevels"][0]
        payload = {
            "ticketId": ticket_id,
            "sessionId": session_id,
            "items": [
                {
                    "priceLevelId": price_level["id"],
                    "priceLevelName": price_level["name"],
                    "quantity": 1,
                    "unitPrice": price_level["price"],
                    "attendees": [
                        {"realName": "课程演示", "idCardNo": make_idcard()}
                    ],
                }
            ],
            "contactName": "课程演示",
            "contactPhone": contact_phone,
            "userRemark": f"pw-course-{run_id}-order",
            "source": "app",
            "usePoints": False,
        }
        if user_coupon_id is not None:
            payload["userCouponId"] = user_coupon_id
        data = unwrap(
            self._context.post("orders", headers=self._auth(user_token), data=payload)
        )
        assert isinstance(data, dict) and data["orderId"] and data["orderNo"]
        return data

    def get_order(self, order_id: int, user_token: str) -> dict:
        """以用户视角查询订单（验证服务端状态，而不是只看 UI toast）。"""
        data = unwrap(self._context.get(f"orders/{order_id}", headers=self._auth(user_token)))
        assert isinstance(data, dict)
        return data

    # ---------- 支付（沙箱） ----------

    def mock_pay(self, user_token: str, order_no: str, amount: int, channel: str = "alipay") -> None:
        """Mock 支付：服务端提供的假支付，订单状态机真实走到"已支付"。

        注意区分两层"Mock"（课堂必讲）：
        - mock-pay：服务端的沙箱接口，真实写数据库、真实流转订单状态；
        - page.route：Playwright 在网络层拦截，服务端完全不知情。
        想造"已支付"的订单，用前者——不花钱、不调支付宝，秒级完成。
        """
        data = unwrap(
            self._context.post(
                "payment/mock-pay",
                headers=self._auth(user_token),
                data={"orderNo": order_no, "amount": amount, "payChannel": channel},
            )
        )
        assert isinstance(data, dict) and data.get("status") == "SUCCESS", f"Mock 支付未成功：{data}"

    def payment_status(self, user_token: str, order_no: str) -> dict:
        """支付网关视角的状态查询（API 复核用，不只信 UI 文案）。"""
        data = unwrap(
            self._context.get(f"payment/status/{order_no}", headers=self._auth(user_token))
        )
        assert isinstance(data, dict)
        return data

    # ---------- 评论 ----------

    def create_comment(self, user_token: str, ticket_id: int, content: str, rating: int = 5) -> None:
        """以用户身份给票发评论（真实入库，UI 详情页评论区可见）。"""
        unwrap(
            self._context.post(
                f"tickets/{ticket_id}/comments",
                headers=self._auth(user_token),
                data={"rating": rating, "content": content},
            )
        )

    # ---------- Banner（自带公开删除接口的资源） ----------

    def create_banner(self, token: str, title: str, image_url: str) -> int:
        """创建一张 web 端首页 Banner，返回 banner ID。"""
        data = unwrap(
            self._context.post(
                "admin/banners",
                headers=self._auth(token),
                data={
                    "title": title,
                    "imageUrl": image_url,
                    "platform": "web",
                    "status": 1,  # 启用
                    "sortOrder": 10,
                },
            )
        )
        assert isinstance(data, int) and data > 0, "创建 Banner 必须返回 ID"
        return data

    def delete_banner(self, token: str, banner_id: int) -> None:
        """删除 Banner——这类资源自带公开删除接口，清理不必依赖 test-support。"""
        unwrap(self._context.delete(f"admin/banners/{banner_id}", headers=self._auth(token)))

    # ---------- 优惠券（创建 → 发放 → 用户领取，三步是三种造法） ----------

    def create_coupon(
        self,
        token: str,
        name: str,
        coupon_value: int = 10,
        min_order_amount: int = 50,
        total_count: int = 100,
        valid_days: int = 30,
    ) -> int:
        """创建一张满减券，返回优惠券 ID。

        字段为什么这么填（对照 Swagger 的 AdminCouponCreateRequest）：
        - couponType=1（满减）：最直观的券种，couponValue 就是减多少元；
        - receiveType=1（手动领取）：用户侧还要走"领券"动作，链路完整；
        - validityType=1（固定日期）+ 显式 validStartDate/validEndDate：
          这是实测追源码定位出来的坑——validityType=2（领取后 N 天）时，
          后端把"领取的这一刻"（含毫秒）作为券实例的生效时间存进 MySQL；
          DATETIME 按整秒存储会发生进位，生效时间可能落到"下一秒"，
          紧接着下单就有约 50% 概率被 100313"优惠券尚未生效"拒绝。
          固定日期的生效时间是当天 00:00:00，永远在过去，测试确定性通过；
        - receiveStartTime / receiveEndTime 同理：领取窗口显式覆盖当前时间；
        - applicableScope=1（全场通用）：不绑具体票，下单任意票都能用。
        清理：test-support/cleanup 的 couponIds 支持删券。
        """
        today = date.today()
        data = unwrap(
            self._context.post(
                "admin/coupons",
                headers=self._auth(token),
                data={
                    "name": name,
                    "couponType": 1,  # 满减券
                    "couponValue": coupon_value,
                    "minOrderAmount": min_order_amount,
                    "totalCount": total_count,
                    "remainCount": total_count,
                    "perLimit": 1,  # 每人限领 1 张
                    "receiveType": 1,  # 手动领取
                    "validityType": 1,  # 固定日期（为什么不用"领取后 N 天"见 docstring）
                    "validDays": valid_days,  # 备用字段，固定日期模式下后端不读它
                    "validStartDate": (today - timedelta(days=1)).isoformat(),
                    "validEndDate": (today + timedelta(days=valid_days)).isoformat(),
                    "receiveStartTime": f"{(today - timedelta(days=1)).isoformat()}T00:00:00",
                    "receiveEndTime": f"{(today + timedelta(days=valid_days)).isoformat()}T23:59:59",
                    "applicableScope": 1,  # 全场通用
                    "description": "课程演示数据",
                    "status": 1,  # 启用
                },
            )
        )
        assert isinstance(data, int) and data > 0, "创建优惠券必须返回 ID"
        return data

    def issue_coupon(self, token: str, coupon_id: int, user_ids: list[int]) -> None:
        """管理员直接把券发给指定用户（后台发放，用户无需领取）。"""
        unwrap(
            self._context.post(
                f"admin/coupons/{coupon_id}/issue",
                headers=self._auth(token),
                data={"userIds": user_ids, "issueMode": "direct"},
            )
        )

    def receive_coupon(self, user_token: str, coupon_id: int) -> None:
        """用户主动领券（前台链路）。

        注意：couponId 在 **query 参数**里，不是请求体——Swagger 的
        parameters 一栏写得很清楚，想当然放 body 会被 400 拒绝
        （"缺少必填参数: couponId"）。看接口文档要看全：路径参数、
        query 参数、请求体，三个位置都可能藏参数。
        """
        unwrap(
            self._context.post(
                "coupons/receive",
                headers=self._auth(user_token),
                params={"couponId": coupon_id},
            )
        )

    def my_coupons(self, user_token: str) -> list[dict]:
        """查询用户已领取的券（GET /coupons/list）。

        下单要传的是这里面的 `id`（券实例 ID），不是券模板 ID——
        一个模板发 N 个用户就有 N 个实例，这是券系统的常见建模。
        """
        data = unwrap(self._context.get("coupons/list", headers=self._auth(user_token)))
        # 兼容两种返回：直接是数组，或分页对象里的 list
        if isinstance(data, dict):
            data = data.get("list") or []
        assert isinstance(data, list)
        return data

    # ---------- 用户资产 ----------

    def add_balance(self, token: str, user_id: int, amount: int, channel: str = "wechat") -> None:
        """管理员给用户充余额（对应 Swagger 的 POST /admin/users/{id}/balance）。

        适合造"有钱的用户"这种前置状态，比如测余额支付。
        注意：余额没有"撤回"接口——数据真实进了账户，教学环境用专用测试用户即可，
        用户本身会随 test-support/cleanup 删除。
        """
        unwrap(
            self._context.post(
                f"admin/users/{user_id}/balance",
                headers=self._auth(token),
                data={"channel": channel, "amount": amount},
            )
        )

    def create_identity(
        self, user_token: str, real_name: str = "课程演示", id_card_no: str | None = None
    ) -> int:
        """给用户添加常用观演人，返回观演人 ID。

        idCardNo 不传就用 make_idcard() 生成合法校验位的号码——
        和下单 attendees 一样，这个字段实测要过校验位验证。
        清理：DELETE /user/identities/{id} 是公开接口，可真实删除。
        """
        data = unwrap(
            self._context.post(
                "user/identities",
                headers=self._auth(user_token),
                data={
                    "realName": real_name,
                    "idCardNo": id_card_no or make_idcard(),
                    "defaultIdentity": False,
                },
            )
        )
        assert isinstance(data, dict) and data.get("id"), "创建观演人必须返回 ID"
        return int(data["id"])

    def delete_identity(self, user_token: str, identity_id: int) -> None:
        """删除常用观演人（公开 DELETE 接口）。"""
        unwrap(
            self._context.delete(f"user/identities/{identity_id}", headers=self._auth(user_token))
        )

    # ---------- 购物车与秒杀 ----------

    def add_to_cart(self, user_token: str, ticket_id: int, quantity: int = 1) -> int:
        """把票加入用户购物车，返回购物车项 ID。

        和下单一样需要场次 ID、票档 ID——这些子资源 ID 只能从
        GET /tickets/{id} 的详情里读（"创建返回的 ID 只是入口"）。
        清理：DELETE /cart/{id} 公开接口可真实删除。
        """
        detail = self.ticket_detail(ticket_id)
        session_id = detail["sessions"][0]["id"]
        price_level = detail["priceLevels"][0]
        data = unwrap(
            self._context.post(
                "cart/add",
                headers=self._auth(user_token),
                data={
                    "ticketId": ticket_id,
                    "sessionId": session_id,
                    "priceLevelId": price_level["id"],
                    "priceLevelName": price_level["name"],
                    "quantity": quantity,
                    "unitPrice": price_level["price"],
                },
            )
        )
        assert isinstance(data, (int, dict)), "加购必须返回购物车项 ID"
        return int(data if isinstance(data, int) else data["id"])

    def remove_from_cart(self, user_token: str, cart_id: int) -> None:
        """删除购物车项（公开 DELETE 接口）。"""
        unwrap(self._context.delete(f"cart/{cart_id}", headers=self._auth(user_token)))

    def create_flash_sale(
        self,
        token: str,
        ticket_id: int,
        flash_price: int = 9,
        flash_stock: int = 5,
        days: int = 7,
    ) -> int:
        """给一张票创建秒杀活动，返回活动 ID。

        造"限时特价"这种运营状态：前台 /tickets/flash-sale 和首页秒杀区
        会真实展示。startTime/endTime 用相对当前时间的窗口，
        保证任何时间跑测试活动都在进行中。
        注意：秒杀没有删除接口，只有启停（PUT /admin/flash-sales/{id}/status），
        清理方式就是停用 + 随票一起清。
        """
        from datetime import datetime

        now = datetime.now()
        data = unwrap(
            self._context.post(
                "admin/flash-sales",
                headers=self._auth(token),
                data={
                    "ticketId": ticket_id,
                    "flashPrice": flash_price,
                    "flashStock": flash_stock,
                    "startTime": (now - timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%S"),
                    "endTime": (now + timedelta(days=days)).strftime("%Y-%m-%dT%H:%M:%S"),
                    "status": 1,  # 启用
                },
            )
        )
        assert isinstance(data, int) and data > 0, "创建秒杀活动必须返回 ID"
        return data

    def set_flash_sale_status(self, token: str, flash_sale_id: int, status: int) -> None:
        """启停秒杀活动（status: 1 启用 / 0 停用）——秒杀没有删除接口，清理即停用。"""
        unwrap(
            self._context.put(
                f"admin/flash-sales/{flash_sale_id}/status",
                headers=self._auth(token),
                data={"status": status},
            )
        )

    # ---------- 清理 ----------

    def cleanup(
        self,
        run_id: str,
        ticket_ids: list[int] | None = None,
        order_ids: list[int] | None = None,
        user_ids: list[int] | None = None,
        coupon_ids: list[int] | None = None,
    ) -> None:
        """清理本次运行创建的数据。三种结果：

        - **真实清理**：`config/settings.py` 里填了 `COURSE_TEST_KEY` 时，
          真实调用后端 `POST /test-support/cleanup` 把登记的 ID 全部删除，
          测试数据真实创建、真实删除，数据库不留残留；
        - **演示清理**：没配密钥时退化为只打印 ID 不删除——
          不配密钥也能跑通流程，演示数据带 pw-course- 前缀留在本地库；
        - **降级**：真实清理接口偶发 500（服务端的"系统繁忙"是兜底包装，
          真实堆栈在后端控制台）时打印警告、不阻塞测试结果——
          清理是善后动作，它失败不该掩盖测试本身的结论。

        无论哪种结果，教学结构都一样：清理放 finally、只删自己登记的 ID、
        数据名带 runId 标记归属。
        """
        course_key = COURSE_TEST_KEY.strip()
        if not course_key:
            print(
                f"\n[演示清理] runId={run_id} → 应删除 ticket_ids={ticket_ids or []}, "
                f"order_ids={order_ids or []}, user_ids={user_ids or []}"
                "（未配置 COURSE_TEST_KEY，不实际删除）"
            )
            return

        try:
            unwrap(
                self._context.post(
                    "test-support/cleanup",
                    headers={"X-Course-Test-Key": course_key},
                    data={
                        "runId": run_id,
                        "ticketIds": ticket_ids or [],
                        "orderIds": order_ids or [],
                        "userIds": user_ids or [],
                        "couponIds": coupon_ids or [],
                    },
                )
            )
        except Exception as exc:
            # 清理失败不该让测试结果变红：测试本身已经通过/失败，
            # 清理只是善后。真实清理接口偶发 500 时降级为打印，
            # 并提示去后端控制台看堆栈（"系统繁忙"是兜底包装）。
            print(
                f"\n[真实清理失败，已降级] runId={run_id} → {type(exc).__name__}: {exc}\n"
                f"  数据带 pw-course-{run_id}- 前缀留在库中；"
                f"排查请去看 BookingApi 控制台里「系统异常:」开头的堆栈。"
            )
            return
        print(
            f"\n[真实清理] runId={run_id} 已删除 ticket_ids={ticket_ids or []}, "
            f"order_ids={order_ids or []}, user_ids={user_ids or []}, "
            f"coupon_ids={coupon_ids or []}"
        )

    # ---------- 单个删除（DELETE 接口，粒度更细） ----------

    def delete_ticket(self, ticket_id: int, run_id: str) -> None:
        """真实删除一张票：DELETE /test-support/tickets/{id}?runId=...

        归属校验在服务端：票名必须带 pw-course-{runId}- 前缀，
        否则计入 rejected 不删除——测试代码删不动别人的数据。
        """
        self._delete_one("tickets", ticket_id, run_id)

    def delete_order(self, order_id: int, run_id: str) -> None:
        """真实删除一笔订单（含订单项、支付、退款记录，自动回补库存）。"""
        self._delete_one("orders", order_id, run_id)

    def delete_user(self, user_id: int, run_id: str) -> None:
        """真实删除一个买家账户（含身份、地址、购物车、持有的券）。"""
        self._delete_one("users", user_id, run_id)

    def delete_coupon(self, coupon_id: int, run_id: str) -> None:
        """真实删除一张优惠券（含已发放给用户的券）。"""
        self._delete_one("coupons", coupon_id, run_id)

    def _delete_one(self, kind: str, resource_id: int, run_id: str) -> None:
        """单个删除的统一实现：密钥/降级策略与 cleanup() 完全一致。"""
        course_key = COURSE_TEST_KEY.strip()
        if not course_key:
            print(
                f"\n[演示删除] {kind}/{resource_id} runId={run_id}"
                "（未配置 COURSE_TEST_KEY，不实际删除）"
            )
            return
        try:
            unwrap(
                self._context.delete(
                    f"test-support/{kind}/{resource_id}?runId={run_id}",
                    headers={"X-Course-Test-Key": course_key},
                )
            )
        except Exception as exc:
            # 与 cleanup() 同一约定：删除失败降级为警告，不阻塞测试结果
            print(
                f"\n[真实删除失败，已降级] {kind}/{resource_id} → "
                f"{type(exc).__name__}: {exc}"
            )
