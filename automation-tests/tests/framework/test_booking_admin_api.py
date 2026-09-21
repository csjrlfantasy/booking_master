"""Offline contracts derived from the current Java admin controllers and DTOs."""

import copy

import pytest

from framework.api.booking import BookingApi
from framework.api.client import ApiBusinessError, ApiClient, ApiHttpError


BASE = "http://booking.invalid/api"
TOKEN = "contract-admin-token"
USER = {"id": 17, "username": "course-user", "status": 1, "userType": 1}
TICKET = {"id": 23, "name": "course-ticket", "status": 2, "availableStock": 10}
ORDER = {"id": 31, "orderNo": "ORD31", "userId": 17, "orderStatus": 1, "payStatus": 0, "payAmount": 99}
COUPON = {"name": "course-coupon", "couponType": 3, "couponValue": 5, "totalCount": 10,
          "remainCount": 10, "perLimit": 1, "validityType": 2, "validDays": 7, "status": 1}
ROLE = {"id": 2, "roleName": "Operator", "roleCode": "OPERATOR", "status": 1}
LOGIN = {"userId": 1, "username": "course-admin", "token": TOKEN, "expiresIn": 3600, "userLevel": 10}
REGISTER = {"username": "course-user", "password": "contract-only", "phone": "19900000001"}
OVERVIEW = {"todaySales": 0, "yesterdaySales": 100, "salesGrowth": -100, "todayOrders": 0,
            "todayUsers": 0, "todayRefunds": 0, "pendingRefundCount": 0, "pendingOrderCount": 0}
ORDER_STATS = {"totalOrders": 1, "pendingPayOrders": 1, "paidOrders": 0, "completedOrders": 0,
               "cancelledOrders": 0, "refundingOrders": 0, "orderStatusDistribution": [{"status": 1, "count": 1}]}
USER_STATS = {"totalUsers": 1, "activeUsers": 1, "newUsersToday": 1, "newUsersThisWeek": 1,
              "newUsersThisMonth": 1, "vipUsers": 0, "userGrowthTrend": [{"date": "2026-09-21", "count": 1}]}


def page(item):
    return {"list": [item], "pageNum": 1, "pageSize": 10, "total": 1,
            "pages": 1, "hasPrevious": False, "hasNext": False}


# name, args, kwargs, HTTP verb, path, query, body, successful data.
CASES = [
    ("login_admin", ("course-admin", "contract-only"), {}, "POST", "/admin/login", {}, {"username": "course-admin", "password": "contract-only"}, LOGIN),
    ("list_admin_users", (TOKEN,), {"keyword": "course", "status": 1, "user_type": 1}, "GET", "/admin/users", {"page": ["1"], "pagesize": ["10"], "keyword": ["course"], "status": ["1"], "usertype": ["1"]}, None, page(USER)),
    ("get_admin_user", (TOKEN, 17), {}, "GET", "/admin/users/17", {}, None, USER),
    ("create_admin_user", (TOKEN, REGISTER), {}, "POST", "/admin/users", {}, REGISTER, 17),
    ("update_admin_user_status", (TOKEN, 17, 0), {}, "PUT", "/admin/users/17/status", {}, {"status": 0}, None),
    ("list_admin_roles", (TOKEN,), {}, "GET", "/admin/roles", {}, None, [ROLE]),
    ("assign_admin_user_roles", (TOKEN, 17, [2]), {}, "PUT", "/admin/users/17/role", {}, {"roleIds": [2]}, None),
    ("list_admin_tickets", (TOKEN,), {"keyword": "course", "status": 2, "category_id": 3, "is_hot": 0}, "GET", "/admin/tickets", {"page": ["1"], "pagesize": ["10"], "keyword": ["course"], "status": ["2"], "categoryid": ["3"], "ishot": ["0"]}, None, page(TICKET)),
    ("get_admin_ticket", (TOKEN, 23), {}, "GET", "/admin/tickets/23", {}, None, TICKET),
    ("create_admin_ticket", (TOKEN, {"name": "course-ticket", "status": 2}), {}, "POST", "/admin/tickets", {}, {"name": "course-ticket", "status": 2}, 23),
    ("update_admin_ticket_status", (TOKEN, 23, 0), {}, "PUT", "/admin/tickets/23/status", {}, {"status": 0}, None),
    ("list_admin_orders", (TOKEN,), {"order_no": "ORD31", "user_id": 17, "status": 1, "pay_status": 0}, "GET", "/admin/orders", {"page": ["1"], "pagesize": ["10"], "orderno": ["ord31"], "userid": ["17"], "status": ["1"], "paystatus": ["0"]}, None, page(ORDER)),
    ("get_admin_order", (TOKEN, 31), {}, "GET", "/admin/orders/31", {}, None, ORDER),
    ("update_admin_order_status", (TOKEN, 31, 3), {}, "PUT", "/admin/orders/31/status", {"status": ["3"]}, None, None),
    ("cancel_admin_order", (TOKEN, 31, "course-cancel"), {}, "PUT", "/admin/orders/31/cancel", {}, {"cancelReason": "course-cancel"}, None),
    ("refund_admin_order", (TOKEN, 31, {"refundAmount": 99, "reason": "course-refund"}), {}, "PUT", "/admin/orders/31/refund", {}, {"refundAmount": 99, "reason": "course-refund"}, None),
    ("list_admin_coupons", (TOKEN,), {"keyword": "course", "status": 1}, "GET", "/admin/coupons", {"page": ["1"], "pagesize": ["10"], "keyword": ["course"], "status": ["1"]}, None, page({"id": 41, **COUPON})),
    ("get_admin_coupon", (TOKEN, 41), {}, "GET", "/admin/coupons/41", {}, None, COUPON),
    ("create_admin_coupon", (TOKEN, COUPON), {}, "POST", "/admin/coupons", {}, COUPON, 41),
    ("update_admin_coupon", (TOKEN, 41, COUPON), {}, "PUT", "/admin/coupons/41", {}, COUPON, None),
    ("update_admin_coupon_status", (TOKEN, 41, 0), {}, "PUT", "/admin/coupons/41/status", {}, {"status": 0}, None),
    ("issue_admin_coupon", (TOKEN, 41, [17]), {"issue_mode": "course-test"}, "POST", "/admin/coupons/41/issue", {}, {"userIds": [17], "issueMode": "course-test"}, None),
    ("get_admin_overview_stats", (TOKEN,), {}, "GET", "/admin/stats/overview", {}, None, OVERVIEW),
    ("get_admin_order_stats", (TOKEN,), {}, "GET", "/admin/stats/orders", {}, None, ORDER_STATS),
    ("get_admin_user_stats", (TOKEN,), {}, "GET", "/admin/stats/users", {}, None, USER_STATS),
]


@pytest.mark.parametrize("case", CASES, ids=lambda c: c[0])
def test_admin_wire_contract(case, requests_mock):
    name, args, kwargs, verb, path, query, body, data = case
    requests_mock.register_uri(verb, BASE + path, json={"code": 0, "message": "ok", "data": data})
    with ApiClient(BASE) as client:
        assert getattr(BookingApi(client), name)(*args, **kwargs) == data
    request = requests_mock.last_request
    assert request.qs == query
    assert request.json() == body if body is not None else not request.body
    assert request.headers.get("Authorization") == (None if name == "login_admin" else "Bearer " + TOKEN)


@pytest.mark.parametrize("case", CASES, ids=lambda c: c[0])
@pytest.mark.parametrize("failure", ["malformed", "http", "business", "local"])
def test_admin_wrappers_fail_closed(case, failure, requests_mock):
    name, args, kwargs, verb, path, _, _, data = case
    expected = ValueError
    if failure == "http":
        requests_mock.register_uri(verb, BASE + path, status_code=403)
        expected = ApiHttpError
    elif failure == "business":
        requests_mock.register_uri(verb, BASE + path, json={"code": 100600, "message": "rejected", "data": None})
        expected = ApiBusinessError
    elif failure == "malformed":
        requests_mock.register_uri(verb, BASE + path, json={"code": 0, "message": "ok", "data": {} if data is None else None})
    else:
        args = (" ", *args[1:])
    with ApiClient(BASE) as client, pytest.raises(expected):
        getattr(BookingApi(client), name)(*args, **kwargs)
    if failure == "local":
        assert requests_mock.call_count == 0


@pytest.mark.parametrize("name,args,kwargs", [
    ("get_admin_user", (TOKEN, True), {}),
    ("create_admin_user", (TOKEN, {**REGISTER, "phone": "invalid"}), {}),
    ("assign_admin_user_roles", (TOKEN, 17, []), {}),
    ("create_admin_ticket", (TOKEN, {"name": ""}), {}),
    ("update_admin_ticket_status", (TOKEN, 23, 99), {}),
    ("list_admin_users", (TOKEN,), {"page": 0}),
    ("list_admin_orders", (TOKEN,), {"pay_status": True}),
    ("refund_admin_order", (TOKEN, 31, {"proofImages": []}), {}),
    ("create_admin_coupon", (TOKEN, {**COUPON, "validDays": 0}), {}),
    ("update_admin_coupon", (TOKEN, 41, {**COUPON, "totalCount": 0}), {}),
    ("issue_admin_coupon", (TOKEN, 41, []), {}),
    ("update_admin_user_status", (TOKEN, 17, 99), {}),
    ("update_admin_order_status", (TOKEN, 31, 0), {}),
    ("update_admin_coupon_status", (TOKEN, 41, 99), {}),
])
def test_admin_invalid_arguments_do_not_send_requests(name, args, kwargs, requests_mock):
    with ApiClient(BASE) as client, pytest.raises(ValueError):
        getattr(BookingApi(client), name)(*args, **kwargs)
    assert requests_mock.call_count == 0


@pytest.mark.parametrize("name,data", [
    ("login_admin", {**LOGIN, "userLevel": 1}),
    ("list_admin_users", page({**USER, "status": "1"})),
    ("list_admin_tickets", page({**TICKET, "availableStock": -1})),
    ("list_admin_orders", page({**ORDER, "payAmount": "99"})),
    ("list_admin_coupons", page({"id": 41, **COUPON, "remainCount": -1})),
    ("list_admin_roles", [{**ROLE, "roleCode": ""}]),
    ("get_admin_order_stats", {**ORDER_STATS, "orderStatusDistribution": [{"status": 1, "count": -1}]}),
    ("get_admin_user_stats", {**USER_STATS, "userGrowthTrend": [{"date": "2026-09-21", "count": "1"}]}),
])
def test_admin_nested_shapes_are_validated(name, data, requests_mock):
    case = next(c for c in CASES if c[0] == name)
    requests_mock.register_uri(case[3], BASE + case[4], json={"code": 0, "message": "ok", "data": copy.deepcopy(data)})
    with ApiClient(BASE) as client, pytest.raises(ValueError):
        getattr(BookingApi(client), name)(*case[1], **case[2])


def test_admin_token_uses_only_environment_credentials(monkeypatch, requests_mock):
    import conftest
    monkeypatch.setenv("BOOKING_ADMIN_USERNAME", "environment-admin")
    monkeypatch.setenv("BOOKING_ADMIN_PASSWORD", "environment-secret")
    requests_mock.post(BASE + "/admin/login", json={"code": 0, "message": "ok", "data": LOGIN})
    with ApiClient(BASE) as client:
        assert conftest.admin_token.__wrapped__(BookingApi(client)) == TOKEN
    assert requests_mock.last_request.json() == {"username": "environment-admin", "password": "environment-secret"}


@pytest.mark.parametrize("missing", ["BOOKING_ADMIN_USERNAME", "BOOKING_ADMIN_PASSWORD"])
def test_missing_admin_credential_fails_before_http(monkeypatch, requests_mock, missing):
    import conftest
    monkeypatch.setenv("BOOKING_ADMIN_USERNAME", "environment-admin")
    monkeypatch.setenv("BOOKING_ADMIN_PASSWORD", "environment-secret")
    monkeypatch.delenv(missing)
    with ApiClient(BASE) as client, pytest.raises(RuntimeError, match=missing):
        conftest.admin_token.__wrapped__(BookingApi(client))
    assert requests_mock.call_count == 0
