"""The web journey is direct checkout; cart is verified separately through API."""

import pytest

from pages.customer.checkout_panel import CheckoutPanel
from pages.customer.shop_page import ShopPage


pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.chromium,
              pytest.mark.public, pytest.mark.write, pytest.mark.destructive_commerce]


def test_customer_direct_checkout_persists_selected_inventory_and_contact(
    logged_in_customer_page, temporary_customer, booking_api, cleanup_registry, customer_order_data
):
    data = customer_order_data
    ShopPage(logged_in_customer_page).open_detail(data["ticket"]["id"], data["ticket"]["name"])
    checkout = CheckoutPanel(logged_in_customer_page)
    checkout.select_inventory(data["session"], data["level"])
    checkout.fill_attendee_and_contact(data["identity"]["id"], data["payload"]["contactName"],
                                      data["payload"]["contactPhone"], data["payload"]["userRemark"])
    created = checkout.submit(cleanup_registry)
    order = booking_api.get_order(temporary_customer.token, created["orderId"])
    assert order["ticketId"] == data["ticket"]["id"]
    assert order["sessionId"] == data["session"]["id"]
    assert order["orderStatus"] == 1 and order["payStatus"] == 0
    assert order["contactPhone"] == data["payload"]["contactPhone"]
    assert order["contactName"] == data["payload"]["contactName"]
    assert order["items"][0]["priceLevelId"] == data["level"]["id"]
    assert order["items"][0]["quantity"] == 1
    assert created["payAmount"] == data["level"]["price"]
