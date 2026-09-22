"""Owned coupon create/edit/status and explicitly gated customer issuance."""

import pytest
from pages.admin.coupon_page import CouponPage

pytestmark = [pytest.mark.ui, pytest.mark.regression]


@pytest.mark.write
def test_admin_ui_creates_updates_and_ends_coupon(logged_in_admin_page, booking_api, admin_token, cleanup_registry, run_id):
    name = f"pw-course-{run_id}-ui-created-coupon"
    coupons = CouponPage(logged_in_admin_page)
    coupons.open()
    coupon_id = coupons.create(name, cleanup_registry)
    created = booking_api.get_admin_coupon(admin_token, coupon_id)
    assert created["name"] == name
    assert created["couponValue"] == 5
    assert created["remainCount"] == 10
    coupons.search(name)
    coupons.change_value(coupon_id, name, 7)
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["couponValue"] == 7
    coupons.end(coupon_id, name)
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["status"] == 2
    coupons.open()
    coupons.search(name)


@pytest.mark.write
@pytest.mark.destructive_commerce
def test_admin_ui_issue_coupon_reaches_owned_customer(logged_in_admin_page, booking_api, admin_token, admin_ui_coupon, temporary_customer):
    coupon = admin_ui_coupon
    coupons = CouponPage(logged_in_admin_page)
    coupons.open()
    coupons.search(coupon["name"])
    coupons.issue(coupon["id"], coupon["name"], temporary_customer.user_id)
    received = booking_api.list_my_coupons(temporary_customer.token, page_size=100)
    owned = [item for item in received["list"] if item["couponId"] == coupon["id"]]
    assert len(owned) == 1
    assert owned[0]["status"] == 0
    assert booking_api.get_admin_coupon(admin_token, coupon["id"])["remainCount"] == 9
