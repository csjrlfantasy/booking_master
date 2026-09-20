"""Run-owned cart coverage using saleable data from the current ticket detail."""

from __future__ import annotations

import pytest

from framework.api.client import ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.write, pytest.mark.public]


def _saleable_cart_payloads(booking_api, minimum: int = 1) -> list[dict[str, object]]:
    page = booking_api.list_tickets(page_num=1, page_size=50)
    inspected_ids = [ticket["id"] for ticket in page["list"]]
    payloads: list[dict[str, object]] = []
    seen: set[tuple[int, int, int]] = set()
    for ticket_id in inspected_ids:
        detail = booking_api.get_ticket(ticket_id)
        sessions = detail.get("sessions")
        levels = detail.get("priceLevels")
        if not isinstance(sessions, list) or not isinstance(levels, list):
            continue
        for session in sessions:
            capacity = session.get("availableCapacity")
            if session.get("status") != 1 or type(capacity) is not int or capacity <= 0:
                continue
            for level in levels:
                price = level.get("price")
                available_stock = level.get("availableStock")
                if (
                    not isinstance(price, (int, float))
                    or price <= 0
                    or type(available_stock) is not int
                    or available_stock <= 0
                    or not isinstance(level.get("name"), str)
                    or not level["name"].strip()
                ):
                    continue
                key = (ticket_id, session.get("id"), level.get("id"))
                if not all(type(value) is int and value > 0 for value in key) or key in seen:
                    continue
                seen.add(key)
                payloads.append(
                    {
                        "ticketId": ticket_id,
                        "sessionId": session["id"],
                        "priceLevelId": level["id"],
                        "priceLevelName": level["name"],
                        "quantity": 1,
                        "unitPrice": price,
                    }
                )
                if len(payloads) >= minimum:
                    return payloads
    pytest.fail(
        "precondition: no saleable ticket/session/price-level combination from "
        f"GET /tickets and GET /tickets/{{id}}; inspected ticket IDs={inspected_ids}; "
        "required fields are session id/status/availableCapacity and price-level "
        "id/name/price/availableStock"
    )


def _cart_item(items, cart_id):
    item = next((candidate for candidate in items if candidate["id"] == cart_id), None)
    assert item is not None, f"cart item {cart_id} was absent from GET /cart"
    return item


def test_customer_can_add_list_and_count_a_saleable_cart_item(temporary_customer, booking_api):
    payload = _saleable_cart_payloads(booking_api)[0]
    count_before = booking_api.get_cart_count(temporary_customer.token)

    cart_id = booking_api.add_cart_item(temporary_customer.token, payload)
    item = _cart_item(booking_api.list_cart_items(temporary_customer.token), cart_id)

    assert item["ticketId"] == payload["ticketId"]
    assert item["sessionId"] == payload["sessionId"]
    assert item["priceLevelId"] == payload["priceLevelId"]
    assert item["quantity"] == 1
    assert booking_api.get_cart_count(temporary_customer.token) == count_before + 1


def test_customer_can_update_quantity_and_observe_count_transition(temporary_customer, booking_api):
    payload = _saleable_cart_payloads(booking_api)[0]
    count_before = booking_api.get_cart_count(temporary_customer.token)
    cart_id = booking_api.add_cart_item(temporary_customer.token, payload)

    assert booking_api.get_cart_count(temporary_customer.token) == count_before + 1
    assert booking_api.update_cart_quantity(temporary_customer.token, cart_id, 2) is None
    assert _cart_item(booking_api.list_cart_items(temporary_customer.token), cart_id)["quantity"] == 2
    assert booking_api.get_cart_count(temporary_customer.token) == count_before + 1


def test_customer_can_delete_one_cart_item_and_observe_its_absence(temporary_customer, booking_api):
    payload = _saleable_cart_payloads(booking_api)[0]
    count_before = booking_api.get_cart_count(temporary_customer.token)
    cart_id = booking_api.add_cart_item(temporary_customer.token, payload)

    assert booking_api.delete_cart_item(temporary_customer.token, cart_id) is None
    assert all(item["id"] != cart_id for item in booking_api.list_cart_items(temporary_customer.token))
    assert booking_api.get_cart_count(temporary_customer.token) == count_before


def test_customer_can_batch_delete_run_owned_cart_items(temporary_customer, booking_api):
    first, second = _saleable_cart_payloads(booking_api, minimum=2)
    cart_ids = [
        booking_api.add_cart_item(temporary_customer.token, first),
        booking_api.add_cart_item(temporary_customer.token, second),
    ]
    assert len(set(cart_ids)) == 2, "precondition: saleable selections must create distinct cart items"

    assert booking_api.batch_delete_cart_items(temporary_customer.token, cart_ids) is None
    remaining_ids = {item["id"] for item in booking_api.list_cart_items(temporary_customer.token)}
    assert not remaining_ids.intersection(cart_ids)
    assert booking_api.get_cart_count(temporary_customer.token) == 0


def test_customer_can_clear_own_cart(temporary_customer, booking_api):
    first, second = _saleable_cart_payloads(booking_api, minimum=2)
    cart_ids = {
        booking_api.add_cart_item(temporary_customer.token, first),
        booking_api.add_cart_item(temporary_customer.token, second),
    }
    assert len(cart_ids) == 2

    assert booking_api.clear_cart(temporary_customer.token) is None
    assert booking_api.list_cart_items(temporary_customer.token) == []
    assert booking_api.get_cart_count(temporary_customer.token) == 0


def test_cart_list_rejects_unauthenticated_access(api_client):
    with pytest.raises(ApiHttpError) as error:
        api_client.get("/cart")

    assert error.value.status_code == 401


def test_cart_quantity_rejects_zero_at_current_dto_boundary(temporary_customer, booking_api, api_client):
    payload = _saleable_cart_payloads(booking_api)[0]
    cart_id = booking_api.add_cart_item(temporary_customer.token, payload)

    with pytest.raises(ApiHttpError) as error:
        api_client.put(
            "/cart/quantity",
            token=temporary_customer.token,
            json={"cartId": cart_id, "quantity": 0},
        )

    assert error.value.status_code == 400
