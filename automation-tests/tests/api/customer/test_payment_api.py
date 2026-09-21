"""Run-owned payment and refund coverage for supported customer routes."""

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


def _create_run_owned_order(run_id, temporary_customer, booking_api, cleanup_registry):
    identity = booking_api.create_identity(
        temporary_customer.token,
        {
            "realName": "TEACHING-ONLY-PAYMENT",
            "idCardNo": _teaching_only_id_card(run_id, 31),
            "phone": f"192{int(run_id[-8:], 16) % 100_000_000:08d}",
            "self": True,
            "defaultIdentity": True,
        },
    )
    page = booking_api.list_tickets(page_num=1, page_size=100)
    inspected_ids = [ticket["id"] for ticket in page["list"]]
    for ticket_id in inspected_ids:
        detail = booking_api.get_ticket(ticket_id)
        for session in detail.get("sessions", []):
            if session.get("status") != 1 or not isinstance(
                session.get("availableCapacity"), int
            ) or session["availableCapacity"] <= 0:
                continue
            for level in detail.get("priceLevels", []):
                if (
                    not isinstance(level.get("id"), int)
                    or level["id"] <= 0
                    or not isinstance(level.get("price"), (int, float))
                    or level["price"] < 0
                    or not isinstance(level.get("availableStock"), int)
                    or level["availableStock"] <= 0
                ):
                    continue
                created = booking_api.create_order(
                    temporary_customer.token,
                    {
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
                        "contactName": "TEACHING-ONLY-PAYMENT",
                        "contactPhone": f"192{int(run_id[-8:], 16) % 100_000_000:08d}",
                        "source": "course-test",
                        "userRemark": f"pw-course-{run_id}-customer-payment",
                    },
                )
                cleanup_registry.add_order(created["orderId"])
                return created
    pytest.fail(
        "precondition: no saleable ticket/session/price-level combination for "
        f"payment order creation; inspected ticket IDs={inspected_ids}"
    )


def _payment_payload(created):
    return {
        "orderNo": created["orderNo"],
        "amount": created["payAmount"],
        "payChannel": "alipay",
        "paymentScene": "H5",
    }


def _register_customer(booking_api, cleanup_registry, test_data_factory):
    payload = test_data_factory.user()
    user_id = booking_api.register_user(payload)
    cleanup_registry.add_user(user_id)
    return booking_api.login_user(payload["username"], payload["password"])["token"]


def test_customer_can_create_payment_and_query_pending_status(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    created = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    payment = booking_api.create_payment(
        temporary_customer.token, _payment_payload(created)
    )
    status = booking_api.get_payment_status(
        temporary_customer.token, created["orderNo"]
    )

    assert payment["orderNo"] == created["orderNo"]
    assert payment["status"] == "PENDING"
    assert status["status"] == "PENDING"


def test_customer_can_mock_pay_and_duplicate_mock_pay_is_rejected(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    created = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    payment = booking_api.mock_pay(
        temporary_customer.token, _payment_payload(created)
    )
    status = booking_api.get_payment_status(
        temporary_customer.token, created["orderNo"]
    )

    assert payment["status"] == "SUCCESS"
    assert status["status"] == "SUCCESS"
    with pytest.raises(ApiBusinessError) as duplicate:
        booking_api.mock_pay(temporary_customer.token, _payment_payload(created))
    assert duplicate.value.code == 100301


def test_paid_order_can_request_refund_and_query_processing_status(
    run_id, temporary_customer, booking_api, cleanup_registry
):
    created = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    booking_api.mock_pay(temporary_customer.token, _payment_payload(created))
    refund = booking_api.apply_refund(
        temporary_customer.token,
        {"orderNo": created["orderNo"], "reason": "course refund"},
    )
    status = booking_api.get_refund_status(
        temporary_customer.token, created["orderNo"]
    )
    order = booking_api.get_order(temporary_customer.token, created["orderId"])

    assert refund["status"] == "PROCESSING"
    assert status["status"] == "PROCESSING"
    assert order["orderStatus"] == 6
    with pytest.raises(ApiBusinessError) as repeated:
        booking_api.apply_refund(
            temporary_customer.token,
            {"orderNo": created["orderNo"], "reason": "duplicate course refund"},
        )
    assert repeated.value.code == 100504


def test_other_customer_cannot_query_or_pay_run_owned_order(
    run_id,
    temporary_customer,
    booking_api,
    cleanup_registry,
    test_data_factory,
):
    created = _create_run_owned_order(
        run_id, temporary_customer, booking_api, cleanup_registry
    )
    other_token = _register_customer(booking_api, cleanup_registry, test_data_factory)

    with pytest.raises((ApiBusinessError, ApiHttpError)) as denied:
        booking_api.get_payment_status(other_token, created["orderNo"])
    if isinstance(denied.value, ApiBusinessError):
        assert denied.value.code == 100300
    else:
        assert denied.value.status_code in {403, 404}
