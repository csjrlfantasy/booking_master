"""Only API-created teaching tickets are mutated by the UI."""

import pytest
from pages.admin.ticket_page import TicketPage

pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.write]


def test_admin_ui_searches_reads_and_takes_ticket_off_sale(logged_in_admin_page, booking_api, admin_token, admin_ui_ticket):
    ticket = admin_ui_ticket
    tickets = TicketPage(logged_in_admin_page)
    tickets.open()
    tickets.search(ticket["name"], 2)
    tickets.show_detail(ticket["name"])
    assert booking_api.get_admin_ticket(admin_token, ticket["id"])["status"] == 2
    tickets.open()
    tickets.search(ticket["name"], 2)
    tickets.take_off_sale(ticket["id"], ticket["name"])
    assert booking_api.get_admin_ticket(admin_token, ticket["id"])["status"] == 0
    assert booking_api.list_admin_tickets(admin_token, keyword=ticket["name"], status=2)["list"] == []
    tickets.open()
    tickets.search(ticket["name"], 0)
    tickets.show_detail(ticket["name"])
