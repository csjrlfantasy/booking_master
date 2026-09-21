"""Coupon management uses owned templates and owned recipients exclusively."""

import pytest


pytestmark = [pytest.mark.api, pytest.mark.regression]


def _create_coupon(booking_api, admin_token, cleanup_registry, run_id):
    payload = {
        "name": f"pw-course-{run_id}-admin-coupon", "couponType": 3, "couponValue": 5,
        "minOrderAmount": 0, "totalCount": 10, "remainCount": 10, "perLimit": 1,
        "receiveType": 2, "validityType": 2, "validDays": 7, "applicableScope": 1,
        "description": f"pw-course-{run_id}-coupon-demo", "status": 1,
    }
    coupon_id = booking_api.create_admin_coupon(admin_token, payload)
    cleanup_registry.add_coupon(coupon_id)
    return coupon_id, payload


def test_admin_coupon_list_is_paginated(booking_api, admin_token):
    assert len(booking_api.list_admin_coupons(admin_token, page=1, page_size=10)["list"]) <= 10


@pytest.mark.write
def test_admin_can_create_update_and_disable_own_coupon(booking_api, admin_token, cleanup_registry, run_id):
    coupon_id, payload = _create_coupon(booking_api, admin_token, cleanup_registry, run_id)
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["name"] == payload["name"]
    payload["couponValue"] = 7
    booking_api.update_admin_coupon(admin_token, coupon_id, payload)
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["couponValue"] == 7
    booking_api.update_admin_coupon_status(admin_token, coupon_id, 0)
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["status"] == 0
    filtered = booking_api.list_admin_coupons(admin_token, keyword=payload["name"], status=0)
    assert [item["id"] for item in filtered["list"]] == [coupon_id]


@pytest.mark.write
@pytest.mark.destructive_commerce
def test_admin_issue_reaches_customer_and_respects_per_user_limit(booking_api, admin_token, cleanup_registry, run_id, temporary_customer):
    coupon_id, _ = _create_coupon(booking_api, admin_token, cleanup_registry, run_id)
    for _ in range(2):
        booking_api.issue_admin_coupon(admin_token, coupon_id, [temporary_customer.user_id], issue_mode="course-test")
    received = booking_api.list_my_coupons(temporary_customer.token, page_size=100)
    owned = [item for item in received["list"] if item["couponId"] == coupon_id]
    assert len(owned) == 1
    assert owned[0]["status"] == 0
    assert booking_api.get_admin_coupon(admin_token, coupon_id)["remainCount"] == 9
