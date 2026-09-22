"""Run-owned inventory/orders; commerce teardown proof remains mandatory."""

import pytest
from pages.admin.order_page import OrderPage

pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.write, pytest.mark.destructive_commerce]


def test_admin_ui_searches_reads_and_cancels_owned_order(logged_in_admin_page, booking_api, admin_token, admin_ui_order, temporary_customer):
    order = admin_ui_order
    orders = OrderPage(logged_in_admin_page)
    orders.open()
    orders.search(order["orderNo"], 1)
    orders.show_detail(order["orderNo"])
    detail = booking_api.get_admin_order(admin_token, order["orderId"])
    assert detail["userId"] == temporary_customer.user_id
    assert detail["payAmount"] == order["payAmount"]
    orders.open()
    orders.search(order["orderNo"], 1)
    orders.cancel(order["orderId"], order["orderNo"])
    assert booking_api.get_admin_order(admin_token, order["orderId"])["orderStatus"] == 4
    assert booking_api.get_order(temporary_customer.token, order["orderId"])["orderStatus"] == 4
    orders.open()
    orders.search(order["orderNo"], 4)


def test_admin_ui_refunds_owned_paid_order(logged_in_admin_page, booking_api, admin_token, paid_admin_ui_order, temporary_customer):
    order = paid_admin_ui_order
    orders = OrderPage(logged_in_admin_page)
    orders.open()
    orders.search(order["orderNo"], 2)
    orders.refund(order["orderId"], order["orderNo"])
    detail = booking_api.get_admin_order(admin_token, order["orderId"])
    assert (detail["orderStatus"], detail["payStatus"]) == (5, 2)
    customer_detail = booking_api.get_order(temporary_customer.token, order["orderId"])
    assert (customer_detail["orderStatus"], customer_detail["payStatus"]) == (5, 2)
    orders.open()
    orders.search(order["orderNo"], 5)
