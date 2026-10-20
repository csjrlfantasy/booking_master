"""优惠券下单全链路：造券 → 领券 → 下单用券 → API 核实优惠金额 → UI 验证 → 清理。

这是工作里最高频的"营销 + 交易"组合场景，链路比单点测试长，
但每一步都是前面例子里见过的积木——本例演示的是**组合**：

    create_coupon      造一张满 50 减 10 的券（管理员）
    receive_coupon     用户领券（券模板 → 用户手里的券实例）
    my_coupons         查出券实例 ID（下单要传的是实例 ID，不是模板 ID）
    create_order       带 userCouponId 下单
    get_order          API 核实：totalAmount / couponAmount / payAmount
    管理后台订单详情页   UI 核实：优惠金额、券名、实付金额都真实展示

教学要点
--------
1. 券系统常见建模：一个券模板发给 N 个用户就是 N 个**券实例**，
   下单引用的是实例 ID——分不清这两个 ID 是新人最常踩的坑；
2. 金额断言要算得出来：票 99 元，满 50 减 10 → 实付必须恰好 89，
   写"应付 89"比写"实付小于总价"强得多（能抓出错算、错用券）；
3. 每步的身份切换看清楚：造券用管理员 token，领券/下单/查券用用户 token。

数据说明：票、券、用户、订单全部真实创建，finally 里真实清理
（需在 config/settings.py 填入 COURSE_TEST_KEY，留空时退化为演示模式）。
"""

from uuid import uuid4

from playwright.sync_api import Page

from api.booking_api import BookingApi, make_phone
from pages.admin_pages import AdminOrderDetailPage

TICKET_PRICE = 99
COUPON_VALUE = 10
MIN_ORDER = 50


def test_order_with_coupon_deducts_correct_amount(
    admin_page: Page, api_client: BookingApi, write_guard: dict, web_url: str
) -> None:
    run_id = uuid4().hex[:12]

    # ── 第一步：API 准备（管理员造票 + 造券，用户领券）──────
    admin_token = api_client.admin_login(write_guard["username"], write_guard["password"])
    ticket_id = api_client.create_ticket(
        admin_token,
        name=f"pw-course-{run_id}-coupon-order",
        description="第 11 课框架版：优惠券下单全链路演示数据",
        price=TICKET_PRICE,
    )
    coupon_id = api_client.create_coupon(
        admin_token,
        name=f"pw-course-{run_id}-满{MIN_ORDER}减{COUPON_VALUE}",
        coupon_value=COUPON_VALUE,
        min_order_amount=MIN_ORDER,
    )

    username = f"pw-course-{run_id}-user"
    phone = make_phone(run_id)
    user_id = api_client.register_user(username, "Course@123456", phone)
    user_token = api_client.user_login(username, "Course@123456")

    api_client.receive_coupon(user_token, coupon_id)
    # 下单要传"券实例 ID"：从用户的券列表里找到刚领的那张
    user_coupons = api_client.my_coupons(user_token)
    user_coupon_id = next(
        c["id"] for c in user_coupons if c["couponId"] == coupon_id
    )
    # 四个 ID（票/券/用户/券实例）全部到手，再做可能失败的操作

    order = api_client.create_order(
        user_token, ticket_id, run_id, phone, user_coupon_id=user_coupon_id
    )
    order_id = order["orderId"]

    try:
        # ── 第二步：API 核实金额（精确的算术断言）──────────
        detail = api_client.get_order(order_id, user_token)
        assert detail["totalAmount"] == TICKET_PRICE, "订单总价应是票价"
        assert detail["couponAmount"] == COUPON_VALUE, "优惠金额应是券面额"
        assert detail["payAmount"] == TICKET_PRICE - COUPON_VALUE, (
            f"实付应为 {TICKET_PRICE}-{COUPON_VALUE}={TICKET_PRICE - COUPON_VALUE}，"
            f"实际 {detail['payAmount']}"
        )
        assert detail["couponName"].startswith(f"pw-course-{run_id}"), "订单应关联本次的券"

        # ── 第三步：UI 验证（管理后台订单详情页真实展示）────
        # 金额断言的页面知识（哪个字段渲染哪一行）收在 POM 里
        AdminOrderDetailPage(admin_page, web_url).open(order_id).expect_amounts(
            total="¥99.00", paid="¥89.00"
        )
    finally:
        # ── 第四步：真实清理（票 + 券 + 订单 + 用户）────────
        api_client.cleanup(
            run_id,
            ticket_ids=[ticket_id],
            order_ids=[order_id],
            user_ids=[user_id],
            coupon_ids=[coupon_id],
        )
