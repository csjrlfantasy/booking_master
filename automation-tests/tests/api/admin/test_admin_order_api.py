"""Administrator commerce checks create their own ticket, customer and order."""

from datetime import date, timedelta

import pytest

from framework.api.client import ApiBusinessError


pytestmark = [pytest.mark.api, pytest.mark.regression, pytest.mark.write, pytest.mark.destructive_commerce]


@pytest.fixture
def admin_owned_order(booking_api, admin_token, cleanup_registry, run_id, temporary_customer):
    prefix = f"pw-course-{run_id}-"
    ticket_id = booking_api.create_admin_ticket(admin_token, {
        "name": prefix + "order-ticket", "description": prefix + "order-ticket",
        "showDate": (date.today() + timedelta(days=60)).isoformat(), "showTime": "19:30",
        "priceMin": 99, "priceMax": 99, "totalStock": 10, "status": 2,
    })
    cleanup_registry.add_ticket(ticket_id)
    ticket = booking_api.get_ticket(ticket_id)
    sessions = [item for item in ticket.get("sessions", []) if item.get("status") == 1 and item.get("availableCapacity", 0) > 0]
    levels = [item for item in ticket.get("priceLevels", []) if item.get("availableStock", 0) > 0]
    if not sessions or not levels:
        pytest.fail(f"precondition: created ticket {ticket_id} lacks saleable session/price level")
    # Deliberately fictitious region code; checksum satisfies teaching API validation.
    base = f"00000020000101{int(run_id[-8:], 16) % 1000:03d}"
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    id_card = base + "10X98765432"[sum(int(n) * w for n, w in zip(base, weights)) % 11]
    phone = f"193{int(run_id[-8:], 16) % 100_000_000:08d}"
    identity = booking_api.create_identity(temporary_customer.token, {
        "realName": "TEACHING-ONLY-ADMIN", "idCardNo": id_card, "phone": phone,
        "self": True, "defaultIdentity": True,
    })
    order = booking_api.create_order(temporary_customer.token, {
        "ticketId": ticket_id, "sessionId": sessions[0]["id"],
        "items": [{"priceLevelId": levels[0]["id"], "quantity": 1,
                   "unitPrice": levels[0]["price"], "attendees": [{"identityId": identity["id"]}]}],
        "contactName": "TEACHING-ONLY-ADMIN", "contactPhone": phone,
        "source": "course-test", "userRemark": prefix + "admin-order",
    })
    cleanup_registry.add_order(order["orderId"])
    return order


def test_admin_can_search_and_read_run_owned_order(booking_api, admin_token, admin_owned_order, temporary_customer):
    order = admin_owned_order
    result = booking_api.list_admin_orders(admin_token, order_no=order["orderNo"], user_id=temporary_customer.user_id, status=1, pay_status=0)
    assert [item["id"] for item in result["list"]] == [order["orderId"]]
    detail = booking_api.get_admin_order(admin_token, order["orderId"])
    assert detail["userId"] == temporary_customer.user_id
    assert detail["orderNo"] == order["orderNo"]
    assert detail["orderStatus"] == 1
    assert detail["payAmount"] == order["payAmount"]


def test_admin_cancel_persists_and_repeated_cancel_is_rejected(booking_api, admin_token, admin_owned_order, temporary_customer, run_id):
    order_id = admin_owned_order["orderId"]
    booking_api.cancel_admin_order(admin_token, order_id, f"pw-course-{run_id}-cancel")
    assert booking_api.get_admin_order(admin_token, order_id)["orderStatus"] == 4
    assert booking_api.get_order(temporary_customer.token, order_id)["orderStatus"] == 4
    with pytest.raises(ApiBusinessError) as repeated:
        booking_api.cancel_admin_order(admin_token, order_id, f"pw-course-{run_id}-repeat")
    assert repeated.value.code == 100600


def test_admin_refund_paid_order_persists_both_states(booking_api, admin_token, admin_owned_order, temporary_customer, run_id):
    order = admin_owned_order
    booking_api.mock_pay(temporary_customer.token, {"orderNo": order["orderNo"], "amount": order["payAmount"], "payChannel": "alipay", "paymentScene": "H5"})
    assert booking_api.get_admin_order(admin_token, order["orderId"])["payStatus"] == 1
    booking_api.refund_admin_order(admin_token, order["orderId"], {"refundAmount": order["payAmount"], "reason": f"pw-course-{run_id}-refund"})
    detail = booking_api.get_admin_order(admin_token, order["orderId"])
    assert detail["orderStatus"] == 5
    assert detail["payStatus"] == 2
    customer_detail = booking_api.get_order(temporary_customer.token, order["orderId"])
    assert customer_detail["orderStatus"] == 5
    assert customer_detail["payStatus"] == 2


def test_admin_cannot_refund_unpaid_order(booking_api, admin_token, admin_owned_order, run_id):
    order_id = admin_owned_order["orderId"]
    with pytest.raises(ApiBusinessError) as rejected:
        booking_api.refund_admin_order(admin_token, order_id, {"reason": f"pw-course-{run_id}-unpaid"})
    assert rejected.value.code == 100600
    detail = booking_api.get_admin_order(admin_token, order_id)
    assert detail["orderStatus"] == 1
    assert detail["payStatus"] == 0


@pytest.mark.xfail(strict=True, raises=AssertionError, reason="ADMIN-TODO-002: updateOrderStatus returns success without persisting status")
def test_admin_status_update_persists_on_owned_paid_order(booking_api, admin_token, admin_owned_order, temporary_customer):
    order = admin_owned_order
    booking_api.mock_pay(temporary_customer.token, {"orderNo": order["orderNo"], "amount": order["payAmount"], "payChannel": "alipay", "paymentScene": "H5"})
    before = booking_api.get_admin_order(admin_token, order["orderId"])
    if before["orderStatus"] != 2:
        pytest.fail("precondition: mock-paid order did not become paid")
    booking_api.update_admin_order_status(admin_token, order["orderId"], 3)
    assert booking_api.get_admin_order(admin_token, order["orderId"])["orderStatus"] == 3, "ADMIN-TODO-002: order status was not persisted"
