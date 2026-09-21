"""Run-owned customer order coverage using dynamically selected inventory."""

from __future__ import annotations

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [
    pytest.mark.api,
    pytest.mark.write,
    pytest.mark.destructive_commerce,
    pytest.mark.public,
]


def _teaching_only_id_card(run_id: str, variant: int) -> str:
    sequence = (int(run_id[-8:], 16) + variant) % 1000
    base = f"00000020000101{sequence:03d}"
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    checks = "10X98765432"
    return base + checks[sum(int(digit) * weight for digit, weight in zip(base, weights)) % 11]


def _saleable_order_payload(run_id, temporary_customer, booking_api):
    page = booking_api.list_tickets(page_num=1, page_size=100)
    inspected_ids = [ticket["id"] for ticket in page["list"]]
    identity = booking_api.create_identity(
        temporary_customer.token,
        {
            "realName": "TEACHING-ONLY-ORDER",
            "idCardNo": _teaching_only_id_card(run_id, 21),
            "phone": f"191{int(run_id[-8:], 16) % 100_000_000:08d}",
            "self": True,
            "defaultIdentity": True,
        },
    )
    for ticket_id in inspected_ids:
        detail = booking_api.get_ticket(ticket_id)
        sessions = detail.get("sessions", [])
        levels = detail.get("priceLevels", [])
        for session in sessions:
            if session.get("status") != 1 or not isinstance(
                session.get("availableCapacity"), int
            ) or session["availableCapacity"] <= 0:
                continue
            for level in levels:
                if (
                    not isinstance(level.get("id"), int)
                    or level["id"] <= 0
                    or not isinstance(level.get("price"), (int, float))
                    or level["price"] < 0
                    or not isinstance(level.get("availableStock"), int)
                    or level["availableStock"] <= 0
                ):
                    continue
                return {
                    "ticketId": ticket_id,
                    "sessionId": session["id"],
                    "items": [
                        {
                            "priceLevelId": level["id"],
                            "quantity": 1,
                            "unitPrice": level["price"],
                            "attendees": [{"identityId": identity["id"]}],
                        }
                    ],
                    "contactName": "TEACHING-ONLY-ORDER",
                    "contactPhone": f"191{int(run_id[-8:], 16) % 100_000_000:08d}",
                    "source": "course-test",
                    "userRemark": f"pw-course-{run_id}-customer-order",
                }
    pytest.fail(
        "precondition: no saleable ticket/session/price-level combination for "
        f"order creation; inspected ticket IDs={inspected_ids}"
    )


def _create_run_owned_order(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    payload = _saleable_order_payload(run_id, temporary_customer, booking_api)
    created = booking_api.create_order(temporary_customer.token, payload)
    cleanup_registry.add_order(created["orderId"])
    return created, payload


def _register_customer(booking_api, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.register_user(payload)
    cleanup_registry.add_user(user_id)
    return booking_api.login_user(payload["username"], payload["password"])["token"]


def _assert_denied(error: BaseException) -> None:
    if isinstance(error, ApiBusinessError):
        assert error.code in {100300, 100600}
        return
    assert isinstance(error, ApiHttpError)
    assert error.status_code in {403, 404}


def test_customer_can_create_list_and_read_a_run_owned_order(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    created, _ = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    orders = booking_api.list_orders(
        temporary_customer.token, status=1, page=1, page_size=100
    )
    detail = booking_api.get_order(temporary_customer.token, created["orderId"])

    assert any(order["id"] == created["orderId"] for order in orders["list"])
    assert detail["id"] == created["orderId"]
    assert detail["orderNo"] == created["orderNo"]
    assert detail["orderStatus"] == 1
    assert detail["payStatus"] == 0


def test_order_rejects_runtime_invalid_price_level_without_creating_order(
    run_id, temporary_customer, booking_api
):
    payload = _saleable_order_payload(run_id, temporary_customer, booking_api)
    payload["items"][0]["priceLevelId"] += 1_000_000_000

    with pytest.raises(ApiBusinessError) as invalid:
        booking_api.create_order(temporary_customer.token, payload)

    assert invalid.value.code == 100600


def test_customer_can_cancel_pending_order_but_cannot_cancel_it_twice(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    created, _ = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    booking_api.cancel_order(
        temporary_customer.token, created["orderId"], cancel_reason="course cleanup"
    )
    cancelled = booking_api.get_order(temporary_customer.token, created["orderId"])

    assert cancelled["orderStatus"] == 4
    assert cancelled["payStatus"] == 0
    with pytest.raises(ApiBusinessError) as repeated:
        booking_api.cancel_order(temporary_customer.token, created["orderId"])
    assert repeated.value.code == 100304


def test_customer_cannot_read_another_customers_order(
    run_id,
    temporary_customer,
    booking_api,
    cleanup_registry,
    test_data_factory,
):
    created, _ = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    other_token = _register_customer(booking_api, cleanup_registry, test_data_factory)

    with pytest.raises((ApiBusinessError, ApiHttpError)) as forbidden:
        booking_api.get_order(other_token, created["orderId"])
    _assert_denied(forbidden.value)
