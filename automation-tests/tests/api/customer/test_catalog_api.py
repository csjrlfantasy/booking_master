"""Read-only catalog coverage for the current public controller contract."""

from __future__ import annotations

import pytest

from framework.api.client import ApiBusinessError, ApiHttpError


pytestmark = [pytest.mark.api, pytest.mark.public]


def _catalog_page(booking_api):
    page = booking_api.list_tickets(page_num=1, page_size=50)
    if not page["list"]:
        pytest.fail("precondition: GET /tickets returned no catalog tickets")
    return page


def _catalog_ticket(booking_api):
    page = _catalog_page(booking_api)
    ticket = page["list"][0]
    ticket_id = ticket["id"]
    if type(ticket_id) is not int or ticket_id <= 0:
        pytest.fail("precondition: GET /tickets returned a ticket without a positive id")
    return ticket, ticket_id


@pytest.mark.smoke
def test_ticket_list_returns_current_pagination_envelope(booking_api):
    page = _catalog_page(booking_api)

    assert set(("list", "pageNum", "pageSize", "total", "pages", "hasPrevious", "hasNext")) <= page.keys()
    assert page["pageNum"] == 1
    assert page["pageSize"] == 50
    assert page["total"] >= len(page["list"])
    assert all(type(ticket["id"]) is int and ticket["id"] > 0 for ticket in page["list"])


@pytest.mark.smoke
def test_ticket_search_and_category_filter_use_current_catalog_values(booking_api):
    ticket, _ = _catalog_ticket(booking_api)
    keyword = ticket.get("title") or ticket.get("name")
    if not isinstance(keyword, str) or not keyword.strip():
        pytest.fail("precondition: GET /tickets returned a ticket without title/name")

    search = booking_api.search_tickets(keyword, page_num=1, page_size=50)
    assert any(item["id"] == ticket["id"] for item in search["list"])

    category_name = ticket.get("category") or ticket.get("categoryName")
    categories = booking_api.list_categories()
    category = next(
        (item for item in categories if item.get("name") == category_name), None
    )
    if category is None:
        pytest.fail(
            "precondition: GET /categories did not contain the category observed from GET /tickets"
        )
    filtered = booking_api.list_tickets(
        page_num=1, page_size=50, category_id=category["id"]
    )
    assert any(item["id"] == ticket["id"] for item in filtered["list"])


@pytest.mark.smoke
def test_ticket_detail_matches_list_id_and_exposes_supported_session_price_level_schema(
    booking_api,
):
    _, ticket_id = _catalog_ticket(booking_api)
    detail = booking_api.get_ticket(ticket_id)

    assert detail["id"] == ticket_id
    assert isinstance(detail.get("sessions"), list), "GET /tickets/{id} must expose sessions"
    assert isinstance(detail.get("priceLevels"), list), "GET /tickets/{id} must expose priceLevels"
    assert all(
        type(session.get("id")) is int and session["id"] > 0
        and type(session.get("status")) is int
        for session in detail["sessions"]
    )
    assert all(
        type(level.get("id")) is int and level["id"] > 0
        and isinstance(level.get("name"), str)
        and isinstance(level.get("price"), (int, float))
        for level in detail["priceLevels"]
    )


def test_nonexistent_positive_ticket_id_returns_not_found_contract(booking_api):
    page = _catalog_page(booking_api)
    missing_ticket_id = max(ticket["id"] for ticket in page["list"]) + 1_000_000_000

    with pytest.raises((ApiBusinessError, ApiHttpError)) as error:
        booking_api.get_ticket(missing_ticket_id)

    if isinstance(error.value, ApiBusinessError):
        assert error.value.code == 404
    else:
        assert error.value.status_code == 404


def test_categories_hot_and_upcoming_return_current_ticket_collections(booking_api):
    categories = booking_api.list_categories()
    hot_tickets = booking_api.get_hot_tickets(limit=10)
    upcoming_tickets = booking_api.get_upcoming_tickets(limit=10)

    assert categories, "precondition: GET /categories returned no categories"
    assert all(type(category["id"]) is int and category["id"] > 0 for category in categories)
    assert all(type(ticket["id"]) is int and ticket["id"] > 0 for ticket in hot_tickets)
    assert all(type(ticket["id"]) is int and ticket["id"] > 0 for ticket in upcoming_tickets)
