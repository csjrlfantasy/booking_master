"""Run-owned customer coupon API coverage against the current controller contract."""

from __future__ import annotations

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.write, pytest.mark.public]


def _claimable_coupon(booking_api, token, *, require_minimum: bool = False):
    center = booking_api.get_coupon_center(token, page=1, page_size=100)
    candidates = [
        coupon
        for coupon in center["list"]
        if coupon.get("received") is False
        and isinstance(coupon.get("remainCount"), int)
        and coupon["remainCount"] > 0
        and (not require_minimum or float(coupon.get("minOrderAmount") or 0) > 0)
    ]
    if not candidates:
        pytest.fail(
            "precondition: coupon center contained no unreceived claimable coupon"
            f" (require_minimum={require_minimum}); observed coupon IDs="
            f"{[coupon.get('couponId') for coupon in center['list']]}"
        )
    return candidates[0]


def _receive_claimable_coupon(booking_api, token, *, require_minimum: bool = False):
    coupon = _claimable_coupon(
        booking_api, token, require_minimum=require_minimum
    )
    user_coupon_id = booking_api.receive_coupon(token, coupon["couponId"])
    return coupon, user_coupon_id


def _saleable_ticket_context(booking_api):
    page = booking_api.list_tickets(page_num=1, page_size=100)
    inspected_ids = [ticket["id"] for ticket in page["list"]]
    for ticket_id in inspected_ids:
        detail = booking_api.get_ticket(ticket_id)
        if not isinstance(detail.get("categoryId"), int) or detail["categoryId"] <= 0:
            continue
        for session in detail.get("sessions", []):
            if session.get("status") != 1 or session.get("availableCapacity", 0) <= 0:
                continue
            for level in detail.get("priceLevels", []):
                if level.get("availableStock", 0) <= 0 or not isinstance(
                    level.get("price"), (int, float)
                ):
                    continue
                return {
                    "ticketId": ticket_id,
                    "categoryId": detail["categoryId"],
                    "unitPrice": level["price"],
                }
    pytest.fail(
        "precondition: no saleable ticket with category ID for coupon validation; "
        f"inspected ticket IDs={inspected_ids}"
    )


def test_coupon_center_and_my_coupon_list_use_authenticated_customer(
    temporary_customer, booking_api
):
    center = booking_api.get_coupon_center(
        temporary_customer.token, page=1, page_size=100
    )
    mine = booking_api.list_my_coupons(
        temporary_customer.token, status=0, page=1, page_size=100
    )

    assert center["pageNum"] == 1
    assert center["pageSize"] == 100
    assert mine["list"] == []


def test_customer_can_receive_a_coupon_and_duplicate_receive_is_rejected(
    temporary_customer, booking_api
):
    coupon, user_coupon_id = _receive_claimable_coupon(
        booking_api, temporary_customer.token
    )
    mine = booking_api.list_my_coupons(
        temporary_customer.token, status=0, page=1, page_size=100
    )

    assert any(item["id"] == user_coupon_id for item in mine["list"])
    with pytest.raises(ApiBusinessError) as duplicate:
        booking_api.receive_coupon(temporary_customer.token, coupon["couponId"])
    assert duplicate.value.code == 100316


def test_received_coupon_validates_against_runtime_ticket_context(
    temporary_customer, booking_api
):
    coupon, user_coupon_id = _receive_claimable_coupon(
        booking_api, temporary_customer.token
    )
    ticket = _saleable_ticket_context(booking_api)
    amount = max(float(coupon.get("minOrderAmount") or 0), ticket["unitPrice"])

    validation = booking_api.validate_coupon(
        temporary_customer.token,
        {
            "userCouponId": user_coupon_id,
            "orderAmount": amount,
            "ticketId": ticket["ticketId"],
            "categoryId": ticket["categoryId"],
        },
    )

    assert validation["usable"] is True
    assert validation["userCouponId"] == user_coupon_id
    assert validation["couponId"] == coupon["couponId"]


def test_coupon_below_runtime_minimum_is_unavailable(
    temporary_customer, booking_api
):
    _, user_coupon_id = _receive_claimable_coupon(
        booking_api, temporary_customer.token, require_minimum=True
    )

    with pytest.raises(ApiBusinessError) as unavailable:
        booking_api.validate_coupon(
            temporary_customer.token,
            {"userCouponId": user_coupon_id, "orderAmount": 0},
        )

    assert unavailable.value.code == 100313


def test_coupon_center_rejects_unauthenticated_access(api_client):
    with pytest.raises((ApiBusinessError, ApiHttpError)) as error:
        api_client.get("/coupons/center")

    if isinstance(error.value, ApiBusinessError):
        assert error.value.code in {100603, 100605}
    else:
        assert error.value.status_code == 401
