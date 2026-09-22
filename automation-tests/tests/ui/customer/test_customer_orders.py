"""Run-owned orders: browser mutation, API readback, browser refresh."""

import pytest

from pages.customer.order_panel import OrderPanel
from pages.customer.shop_page import ShopPage


pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.chromium,
              pytest.mark.public, pytest.mark.write, pytest.mark.destructive_commerce]


def _open(page, customer_name, order):
    ShopPage(page).open()
    panel = OrderPanel(page)
    panel.open(customer_name, order["orderNo"])
    return panel


def test_customer_order_list_detail_survives_refresh(logged_in_customer_page, customer_display_name, customer_ui_order, temporary_customer, booking_api):
    panel = _open(logged_in_customer_page, customer_display_name, customer_ui_order)
    panel.expect_status(customer_ui_order["orderNo"], "待支付")
    panel.refresh_and_expect(customer_display_name, customer_ui_order["orderNo"], "待支付")
    assert booking_api.get_order(temporary_customer.token, customer_ui_order["orderId"])["orderStatus"] == 1


def test_customer_cancellation_persists(logged_in_customer_page, customer_display_name, customer_ui_order, temporary_customer, booking_api):
    panel = _open(logged_in_customer_page, customer_display_name, customer_ui_order)
    panel.cancel(customer_ui_order["orderId"])
    order = booking_api.get_order(temporary_customer.token, customer_ui_order["orderId"])
    assert order["orderStatus"] == 4 and order["payStatus"] == 0
    panel.refresh_and_expect(customer_display_name, customer_ui_order["orderNo"], "已取消")


def test_customer_mock_payment_persists(logged_in_customer_page, customer_display_name, customer_ui_order, temporary_customer, booking_api):
    panel = _open(logged_in_customer_page, customer_display_name, customer_ui_order)
    panel.pay()
    order = booking_api.get_order(temporary_customer.token, customer_ui_order["orderId"])
    assert order["orderStatus"] == 2 and order["payStatus"] == 1
    assert booking_api.get_payment_status(temporary_customer.token, customer_ui_order["orderNo"])["status"] == "SUCCESS"
    panel.refresh_and_expect(customer_display_name, customer_ui_order["orderNo"], "已支付")


def test_customer_refund_request_persists_as_processing(logged_in_customer_page, customer_display_name, customer_ui_order, temporary_customer, booking_api):
    booking_api.mock_pay(temporary_customer.token, {
        "orderNo": customer_ui_order["orderNo"], "amount": customer_ui_order["payAmount"],
        "payChannel": "alipay", "paymentScene": "H5",
    })
    panel = _open(logged_in_customer_page, customer_display_name, customer_ui_order)
    panel.refund()
    assert booking_api.get_refund_status(temporary_customer.token, customer_ui_order["orderNo"])["status"] == "PROCESSING"
    assert booking_api.get_order(temporary_customer.token, customer_ui_order["orderId"])["orderStatus"] == 6
    panel.refresh_and_expect(customer_display_name, customer_ui_order["orderNo"], "退款中")
