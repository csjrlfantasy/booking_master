"""Execution-only customer UI setup; no browser/API calls during collection."""

from datetime import date
import re

import pytest


@pytest.fixture
def customer_ui_page(browser, settings, request):
    viewport = getattr(request, "param", {"width": 1440, "height": 1000})
    context = browser.new_context(base_url=settings.web_url, viewport=viewport)
    context.set_default_timeout(settings.timeout_seconds * 1000)
    try:
        yield context.new_page()
    finally:
        context.close()


@pytest.fixture
def logged_in_customer_page(authenticated_customer_context, settings):
    authenticated_customer_context.set_default_timeout(settings.timeout_seconds * 1000)
    page = authenticated_customer_context.new_page()
    page.set_viewport_size({"width": 1440, "height": 1000})
    return page


@pytest.fixture
def ui_login_credentials(booking_api, cleanup_registry, test_data_factory):
    data = test_data_factory.user()
    user_id = booking_api.register_user(data)
    cleanup_registry.add_user(user_id)
    try:
        yield data
    finally:
        data.clear()


@pytest.fixture
def catalog_ticket(api_client):
    tickets = api_client.get("/tickets", params={"pageNum": 1, "pageSize": 24,
                                                "sortBy": "date", "sortOrder": "asc"})["list"]
    if not tickets:
        pytest.fail("precondition: public catalog has no tickets; seed teaching catalog")
    for ticket in tickets:
        if ticket.get("title") and sum(t.get("title") == ticket["title"] for t in tickets) == 1:
            return ticket
    pytest.fail("precondition: catalog requires a uniquely named ticket for discovery")


@pytest.fixture
def catalog_category(booking_api, api_client):
    display_by_id = {1: "演唱", 2: "戏剧", 3: "音乐", 4: "舞蹈", 5: "体育",
                     6: "Live", 7: "喜剧", 8: "展览", 9: "二次元", 10: "乐园"}
    categories = booking_api.list_categories()[:9]
    for category in categories:
        # Select a real visible category with results, never a hard-coded ticket.
        if category["id"] not in display_by_id:
            continue
        tickets = api_client.get("/tickets", params={"pageNum": 1, "pageSize": 24,
                                                    "sortBy": "date", "sortOrder": "asc",
                                                    "categoryId": category["id"]})["list"]
        for ticket in tickets:
            if ticket.get("title") and sum(t.get("title") == ticket["title"] for t in tickets) == 1:
                return category, display_by_id[category["id"]], ticket
    pytest.fail(f"precondition: no populated visible category; inspected IDs={[c['id'] for c in categories]}")


@pytest.fixture
def customer_saleable_inventory(booking_api, temporary_customer):
    inspected = []
    profile = booking_api.get_user(temporary_customer.user_id, temporary_customer.token)
    balance = profile.get("alipayBalance")
    if not isinstance(balance, (int, float)) or balance <= 0:
        pytest.fail("precondition: temporary teaching customer needs a positive mock Alipay balance")
    for ticket in booking_api.list_tickets(page_num=1, page_size=100)["list"]:
        inspected.append(ticket["id"])
        detail = booking_api.get_ticket(ticket["id"])
        if detail.get("status") != 2 or not detail.get("availableStock", 0) > 0:
            continue
        min_age = re.sub(r"[^0-9]", "", detail.get("ageLimit") or "")
        if min_age and int(min_age) > date.today().year - 2000:
            continue
        sessions = detail.get("sessions", [])
        levels = detail.get("priceLevels", [])
        for session in sessions:
            if session.get("status") != 1 or not session.get("availableCapacity", 0) > 0:
                continue
            if not session.get("showDate") or date.fromisoformat(session["showDate"][:10]) <= date.today():
                continue
            if sum((s.get("showDate"), s.get("showTime")) == (session.get("showDate"), session.get("showTime")) for s in sessions) != 1:
                continue
            for level in levels:
                if not 0 < level.get("price", 0) <= balance or level.get("availableStock", 0) <= 0 or not level.get("name"):
                    continue
                if sum(p.get("name") == level["name"] for p in levels) != 1:
                    continue
                return {"ticket": detail, "session": session, "level": level}
    pytest.fail(f"precondition: no future saleable ticket with uniquely named choices; inspected IDs={inspected}")


@pytest.fixture
def customer_order_data(run_id, temporary_customer, booking_api, customer_saleable_inventory):
    base = f"00000020000101{int(run_id[-8:], 16) % 1000:03d}"
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    checksum = "10X98765432"[sum(int(d) * w for d, w in zip(base, weights)) % 11]
    phone = f"191{int(run_id[-8:], 16) % 100_000_000:08d}"
    identity = booking_api.create_identity(temporary_customer.token, {
        "realName": "TEACHING-ONLY-UI", "idCardNo": base + checksum,
        "phone": phone, "defaultIdentity": True, "self": True,
    })
    inventory = customer_saleable_inventory
    return {**inventory, "identity": identity, "payload": {
        "ticketId": inventory["ticket"]["id"], "sessionId": inventory["session"]["id"],
        "items": [{"priceLevelId": inventory["level"]["id"], "quantity": 1,
                   "unitPrice": inventory["level"]["price"], "attendees": [{"identityId": identity["id"]}]}],
        "contactName": "TEACHING-ONLY-UI", "contactPhone": phone,
        "userRemark": f"pw-course-{run_id}-customer-ui", "source": "h5",
    }}


@pytest.fixture
def customer_ui_order(temporary_customer, booking_api, cleanup_registry, customer_order_data):
    created = booking_api.create_order(temporary_customer.token, customer_order_data["payload"])
    cleanup_registry.add_order(created["orderId"])
    return created


@pytest.fixture
def customer_display_name(temporary_customer, booking_api):
    profile = booking_api.get_user(temporary_customer.user_id, temporary_customer.token)
    return profile.get("nickname") or profile["username"]
