"""Run-owned admin coupon -> customer browser checkout -> exact persisted discount."""

from decimal import Decimal

import pytest

from pages.customer.checkout_panel import CheckoutPanel
from pages.customer.shop_page import ShopPage

pytestmark = [pytest.mark.e2e, pytest.mark.chromium, pytest.mark.write,
              pytest.mark.destructive_commerce, pytest.mark.xdist_group("booking_e2e_commerce")]


def test_owned_coupon_is_applied_to_browser_purchase(
    logged_in_customer_page, temporary_customer, customer_order_data, e2e_issued_coupon,
    booking_api, cleanup_registry, e2e_evidence,
):
    data, coupon = customer_order_data, e2e_issued_coupon
    total = Decimal(str(data["level"]["price"]))
    discount = min(total, Decimal(str(coupon["couponValue"])))
    ShopPage(logged_in_customer_page).open_detail(data["ticket"]["id"], data["ticket"]["name"])
    checkout = CheckoutPanel(logged_in_customer_page)
    checkout.select_inventory(data["session"], data["level"])
    checkout.fill_attendee_and_contact(data["identity"]["id"], data["payload"]["contactName"],
                                      data["payload"]["contactPhone"], data["payload"]["userRemark"])
    checkout.apply_coupon(coupon["id"], coupon["couponName"])
    checkout.expect_coupon_discount(discount)
    created = checkout.submit(cleanup_registry)
    order = booking_api.get_order(temporary_customer.token, created["orderId"])
    e2e_evidence("coupon purchase persisted", **{**order, "orderId": created["orderId"]})
    assert order["userId"] == temporary_customer.user_id
    assert order["couponId"] == coupon["couponId"]
    assert order["userCouponId"] == coupon["id"]
    assert Decimal(str(order["totalAmount"])) == total
    assert Decimal(str(order["discountAmount"])) == discount
    assert Decimal(str(order["payAmount"])) == total - discount
    assert order["orderStatus"] == 1 and order["payStatus"] == 0
