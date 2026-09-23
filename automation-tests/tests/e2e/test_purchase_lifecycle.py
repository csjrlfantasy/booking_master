"""API setup -> browser purchase/payment/refund -> persisted API state."""

import pytest

from pages.customer.checkout_panel import CheckoutPanel
from pages.customer.order_panel import OrderPanel
from pages.customer.shop_page import ShopPage

pytestmark = [pytest.mark.e2e, pytest.mark.chromium, pytest.mark.write,
              pytest.mark.destructive_commerce, pytest.mark.xdist_group("booking_e2e_commerce")]


def test_purchase_payment_and_refund_request(
    logged_in_customer_page, temporary_customer, customer_order_data, customer_display_name,
    booking_api, cleanup_registry, e2e_evidence,
):
    data = customer_order_data
    shop = ShopPage(logged_in_customer_page)
    shop.open_detail(data["ticket"]["id"], data["ticket"]["name"])
    checkout = CheckoutPanel(logged_in_customer_page)
    checkout.select_inventory(data["session"], data["level"])
    checkout.fill_attendee_and_contact(data["identity"]["id"], data["payload"]["contactName"],
                                      data["payload"]["contactPhone"], data["payload"]["userRemark"])
    created = checkout.submit(cleanup_registry)
    token = temporary_customer.token
    order = booking_api.get_order(token, created["orderId"])
    e2e_evidence("created: pending payment", **{**order, "orderId": created["orderId"]})
    assert order["orderStatus"] == 1 and order["payStatus"] == 0
    assert order["ticketId"] == data["ticket"]["id"]
    assert order["sessionId"] == data["session"]["id"]
    assert order["userId"] == temporary_customer.user_id
    assert order["items"][0]["priceLevelId"] == data["level"]["id"]
    shop.open()
    panel = OrderPanel(logged_in_customer_page)
    panel.open(customer_display_name, created["orderNo"])
    panel.pay()
    paid = booking_api.get_order(token, created["orderId"])
    e2e_evidence("payment persisted", **{**paid, "orderId": created["orderId"]})
    assert paid["orderStatus"] == 2 and paid["payStatus"] == 1
    assert booking_api.get_payment_status(token, created["orderNo"])["status"] == "SUCCESS"
    panel.refresh_and_expect(customer_display_name, created["orderNo"], "已支付")
    panel.refund()
    final = booking_api.get_order(token, created["orderId"])
    refund = booking_api.get_refund_status(token, created["orderNo"])
    e2e_evidence("refund request persisted", **{**final, "orderId": created["orderId"], "refundStatus": refund["status"]})
    assert final["orderStatus"] == 6
    assert refund["status"] == "PROCESSING"  # No automatic settlement exists in this product.
    panel.refresh_and_expect(customer_display_name, created["orderNo"], "退款中")
