"""Execution-only admin contexts and run-owned business data."""

from datetime import date, timedelta
import json
import os
import time
from urllib.parse import urlsplit

import pytest


def _credentials():
    result = {}
    for field, env in (("username", "BOOKING_ADMIN_USERNAME"), ("password", "BOOKING_ADMIN_PASSWORD")):
        value = os.environ.get(env)
        if not value or not value.strip():
            raise RuntimeError(f"{env} must be a non-empty environment variable")
        result[field] = value
    return result


@pytest.fixture
def admin_ui_credentials():
    credentials = _credentials()
    try:
        yield credentials
    finally:
        credentials.clear()


@pytest.fixture(scope="session")
def admin_ui_login(booking_api):
    credentials = _credentials()
    try:
        return booking_api.login_admin(**credentials)
    finally:
        credentials.clear()


def admin_storage_state(web_url, login, *, now_ms=None):
    """Exact Zustand persist v0 envelope and LoginPage user mapping; memory only."""
    origin = urlsplit(web_url)
    state = {"version": 0, "state": {
        "isAuthenticated": True, "token": login["token"],
        "expiresAt": (int(time.time() * 1000) if now_ms is None else now_ms) + login["expiresIn"] * 1000,
        "user": {"id": login["userId"], "username": login["username"], "nickname": login.get("nickname"),
                 "phone": "", "userType": 0, "userTypeText": "管理员", "status": 1,
                 "statusText": "正常", "createdAt": ""},
    }}
    return {"cookies": [], "origins": [{"origin": f"{origin.scheme}://{origin.netloc}", "localStorage": [
        {"name": "auth-storage", "value": json.dumps(state, ensure_ascii=False)},
    ]}]}


@pytest.fixture
def admin_ui_page(browser, settings):
    context = browser.new_context(base_url=settings.web_url, viewport={"width": 1440, "height": 1000})
    try:
        context.set_default_timeout(settings.timeout_seconds * 1000)
        yield context.new_page()
    finally:
        context.close()


@pytest.fixture
def logged_in_admin_page(browser, settings, admin_ui_login):
    context = browser.new_context(base_url=settings.web_url, viewport={"width": 1440, "height": 1000},
                                  storage_state=admin_storage_state(settings.web_url, admin_ui_login))
    try:
        context.set_default_timeout(settings.timeout_seconds * 1000)
        yield context.new_page()
    finally:
        context.close()


@pytest.fixture
def admin_ui_customer(booking_api, admin_token, cleanup_registry, test_data_factory):
    data = test_data_factory.user()
    user_id = booking_api.create_admin_user(admin_token, data)
    cleanup_registry.add_user(user_id)
    try:
        yield {"id": user_id, "data": data}
    finally:
        data.clear()


@pytest.fixture
def admin_ui_ticket(booking_api, admin_token, cleanup_registry, run_id):
    name = f"pw-course-{run_id}-admin-ui-ticket"
    payload = {"name": name, "description": name,
               "showDate": (date.today() + timedelta(days=60)).isoformat(), "showTime": "19:30",
               "priceMin": 99, "priceMax": 99, "totalStock": 10, "status": 2}
    ticket_id = booking_api.create_admin_ticket(admin_token, payload)
    cleanup_registry.add_ticket(ticket_id)
    return {"id": ticket_id, **payload}


@pytest.fixture
def admin_ui_coupon(booking_api, admin_token, cleanup_registry, run_id):
    payload = {"name": f"pw-course-{run_id}-admin-ui-coupon", "couponType": 3, "couponValue": 5,
               "minOrderAmount": 0, "totalCount": 10, "remainCount": 10, "perLimit": 1,
               "receiveType": 2, "validityType": 2, "validDays": 7, "applicableScope": 1,
               "description": f"pw-course-{run_id}-coupon", "status": 1}
    coupon_id = booking_api.create_admin_coupon(admin_token, payload)
    cleanup_registry.add_coupon(coupon_id)
    return {"id": coupon_id, **payload}


@pytest.fixture
def admin_ui_order(booking_api, admin_ui_ticket, temporary_customer, cleanup_registry, run_id):
    ticket = booking_api.get_ticket(admin_ui_ticket["id"])
    sessions = [s for s in ticket.get("sessions", []) if s.get("status") == 1 and s.get("availableCapacity", 0) > 0]
    levels = [p for p in ticket.get("priceLevels", []) if p.get("availableStock", 0) > 0]
    if not sessions or not levels:
        pytest.fail(f"precondition: created ticket {ticket['id']} lacks saleable session/price level")
    base = f"00000020000101{int(run_id[-8:], 16) % 1000:03d}"
    weights = (7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2)
    id_card = base + "10X98765432"[sum(int(n) * w for n, w in zip(base, weights)) % 11]
    phone = f"193{int(run_id[-8:], 16) % 100_000_000:08d}"
    identity = booking_api.create_identity(temporary_customer.token, {
        "realName": "TEACHING-ONLY-ADMIN-UI", "idCardNo": id_card, "phone": phone,
        "self": True, "defaultIdentity": True,
    })
    order = booking_api.create_order(temporary_customer.token, {
        "ticketId": ticket["id"], "sessionId": sessions[0]["id"],
        "items": [{"priceLevelId": levels[0]["id"], "quantity": 1, "unitPrice": levels[0]["price"],
                   "attendees": [{"identityId": identity["id"]}]}],
        "contactName": "TEACHING-ONLY-ADMIN-UI", "contactPhone": phone,
        "source": "course-test", "userRemark": f"pw-course-{run_id}-admin-ui-order",
    })
    cleanup_registry.add_order(order["orderId"])
    return order


@pytest.fixture
def paid_admin_ui_order(booking_api, temporary_customer, admin_ui_order):
    order = admin_ui_order
    booking_api.mock_pay(temporary_customer.token, {"orderNo": order["orderNo"], "amount": order["payAmount"],
                                                  "payChannel": "alipay", "paymentScene": "H5"})
    detail = booking_api.get_order(temporary_customer.token, order["orderId"])
    if detail["orderStatus"] != 2 or detail["payStatus"] != 1:
        pytest.fail("precondition: owned mock-paid order failed to reach paid state")
    return order
