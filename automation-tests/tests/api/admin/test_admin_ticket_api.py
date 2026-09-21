"""Admin catalogue reads and status transitions on freshly created tickets."""

from datetime import date, timedelta

import pytest


pytestmark = [pytest.mark.api, pytest.mark.regression]


def test_admin_ticket_pagination_and_detail(booking_api, admin_token):
    result = booking_api.list_admin_tickets(admin_token, page=1, page_size=10)
    assert len(result["list"]) <= 10
    if not result["list"]:
        pytest.fail("precondition: no admin tickets available for read-only detail check")
    first = result["list"][0]
    detail = booking_api.get_admin_ticket(admin_token, first["id"])
    assert detail["id"] == first["id"]
    assert detail["name"] == first["name"]


@pytest.mark.write
def test_admin_created_ticket_can_be_filtered_and_taken_off_sale(booking_api, admin_token, cleanup_registry, run_id):
    name = f"pw-course-{run_id}-admin-ticket"
    ticket_id = booking_api.create_admin_ticket(admin_token, {
        "name": name, "description": name, "showDate": (date.today() + timedelta(days=60)).isoformat(),
        "showTime": "19:30", "priceMin": 99, "priceMax": 99, "totalStock": 10, "status": 2,
    })
    cleanup_registry.add_ticket(ticket_id)
    detail = booking_api.get_admin_ticket(admin_token, ticket_id)
    assert detail["name"] == name
    assert detail["status"] == 2
    assert detail["availableStock"] == 10
    result = booking_api.list_admin_tickets(admin_token, keyword=name, status=2)
    assert [item["id"] for item in result["list"]] == [ticket_id]
    booking_api.update_admin_ticket_status(admin_token, ticket_id, 0)
    assert booking_api.get_admin_ticket(admin_token, ticket_id)["status"] == 0
    assert booking_api.list_admin_tickets(admin_token, keyword=name, status=2)["list"] == []
