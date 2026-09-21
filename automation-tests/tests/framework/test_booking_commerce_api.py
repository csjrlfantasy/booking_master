"""Mocked contracts for current customer coupon, order, and payment routes."""

from __future__ import annotations

import pytest

from framework.api.booking import BookingApi
from framework.api.client import ApiBusinessError, ApiClient, ApiHttpError


BASE_URL = "http://example.test/api"
TOKEN = "synthetic-token"


def _result(data, *, code: int = 0):
    return {"code": code, "message": "ok", "data": data}


def _page(items):
    return {
        "list": items,
        "pageNum": 1,
        "pageSize": 10,
        "total": len(items),
        "pages": 1,
        "hasPrevious": False,
        "hasNext": False,
    }


def _coupon_center_item():
    return {
        "couponId": 41,
        "couponName": "Course coupon",
        "couponType": 1,
        "couponValue": 20.0,
        "minOrderAmount": 100.0,
        "received": False,
    }


def _user_coupon_item():
    return {
        "id": 51,
        "couponId": 41,
        "couponName": "Course coupon",
        "couponType": 1,
        "couponValue": 20.0,
        "status": 0,
    }


def _order_payload():
    return {
        "ticketId": 7,
        "sessionId": 11,
        "items": [
            {
                "priceLevelId": 13,
                "quantity": 1,
                "unitPrice": 99.0,
            }
        ],
        "contactName": "Course customer",
        "contactPhone": "13800138000",
        "source": "course-test",
    }


def _created_order():
    return {
        "orderId": 61,
        "orderNo": "ORD-61",
        "totalAmount": 99.0,
        "discountAmount": 0.0,
        "payAmount": 99.0,
    }


def _order_item():
    return {
        "id": 61,
        "orderNo": "ORD-61",
        "ticketId": 7,
        "ticketName": "Course concert",
        "totalQuantity": 1,
        "payAmount": 99.0,
        "orderStatus": 1,
        "payStatus": 0,
    }


def _order_detail():
    return {**_order_item(), "userId": 19, "sessionId": 11, "items": []}


def _payment_result(*, status: str = "PENDING"):
    return {
        "paymentId": 71,
        "orderNo": "ORD-61",
        "amount": 99.0,
        "status": status,
        "payChannel": "alipay",
    }


def _refund_result(*, status: str = "PROCESSING"):
    return {
        "refundId": 81,
        "orderNo": "ORD-61",
        "payAmount": 99.0,
        "refundAmount": 99.0,
        "status": status,
    }


def test_coupon_wrappers_use_current_routes_queries_bearer_headers_and_shapes(requests_mock):
    requests_mock.get(f"{BASE_URL}/coupons", json=_result([_user_coupon_item()]))
    requests_mock.get(
        f"{BASE_URL}/coupons/center", json=_result(_page([_coupon_center_item()]))
    )
    requests_mock.get(
        f"{BASE_URL}/coupons/list", json=_result(_page([_user_coupon_item()]))
    )
    requests_mock.post(f"{BASE_URL}/coupons/receive", json=_result(51))
    requests_mock.post(
        f"{BASE_URL}/coupons/validate",
        json=_result({"usable": True, "userCouponId": 51, "couponId": 41}),
    )
    booking = BookingApi(ApiClient(BASE_URL))

    assert booking.list_available_coupons(
        TOKEN, order_amount=120.0, ticket_id=7, category_id=3
    ) == [_user_coupon_item()]
    assert booking.get_coupon_center(TOKEN, page=1, page_size=10) == _page(
        [_coupon_center_item()]
    )
    assert booking.list_my_coupons(TOKEN, status=0, page=1, page_size=10) == _page(
        [_user_coupon_item()]
    )
    assert booking.receive_coupon(TOKEN, 41) == 51
    assert booking.validate_coupon(
        TOKEN, {"userCouponId": 51, "orderAmount": 120.0, "ticketId": 7, "categoryId": 3}
    ) == {"usable": True, "userCouponId": 51, "couponId": 41}

    requests = requests_mock.request_history
    assert [request.method for request in requests] == ["GET", "GET", "GET", "POST", "POST"]
    assert [request.url for request in requests] == [
        f"{BASE_URL}/coupons?orderAmount=120.0&ticketId=7&categoryId=3",
        f"{BASE_URL}/coupons/center?page=1&pageSize=10",
        f"{BASE_URL}/coupons/list?status=0&page=1&pageSize=10",
        f"{BASE_URL}/coupons/receive?couponId=41",
        f"{BASE_URL}/coupons/validate",
    ]
    assert all(request.headers["Authorization"] == f"Bearer {TOKEN}" for request in requests)
    assert requests[4].json() == {
        "userCouponId": 51,
        "orderAmount": 120.0,
        "ticketId": 7,
        "categoryId": 3,
    }


def test_order_wrappers_use_current_routes_queries_bearer_headers_and_shapes(requests_mock):
    payload = _order_payload()
    requests_mock.post(f"{BASE_URL}/orders", json=_result(_created_order()))
    requests_mock.get(f"{BASE_URL}/orders", json=_result(_page([_order_item()])))
    requests_mock.get(f"{BASE_URL}/orders/61", json=_result(_order_detail()))
    requests_mock.put(f"{BASE_URL}/orders/61/cancel", json=_result(None))
    booking = BookingApi(ApiClient(BASE_URL))

    assert booking.create_order(TOKEN, payload) == _created_order()
    assert booking.list_orders(TOKEN, status=1, page=1, page_size=10) == _page(
        [_order_item()]
    )
    assert booking.get_order(TOKEN, 61) == _order_detail()
    assert booking.cancel_order(TOKEN, 61, cancel_reason="course cleanup") is None

    requests = requests_mock.request_history
    assert [request.method for request in requests] == ["POST", "GET", "GET", "PUT"]
    assert [request.url for request in requests] == [
        f"{BASE_URL}/orders",
        f"{BASE_URL}/orders?status=1&page=1&pageSize=10",
        f"{BASE_URL}/orders/61",
        f"{BASE_URL}/orders/61/cancel",
    ]
    assert all(request.headers["Authorization"] == f"Bearer {TOKEN}" for request in requests)
    assert requests[0].json() == payload
    assert requests[3].json() == {"cancelReason": "course cleanup"}


def test_payment_and_refund_wrappers_use_current_routes_bearer_headers_and_shapes(requests_mock):
    payment_payload = {"orderNo": "ORD-61", "amount": 99.0, "payChannel": "alipay", "paymentScene": "H5"}
    refund_payload = {"orderNo": "ORD-61", "reason": "course refund", "refundAmount": "99.0"}
    requests_mock.post(f"{BASE_URL}/payment/create", json=_result(_payment_result()))
    requests_mock.post(
        f"{BASE_URL}/payment/mock-pay", json=_result(_payment_result(status="SUCCESS"))
    )
    requests_mock.get(
        f"{BASE_URL}/payment/status/ORD-61", json=_result(_payment_result(status="SUCCESS"))
    )
    requests_mock.post(f"{BASE_URL}/payment/refund", json=_result(_refund_result()))
    requests_mock.get(
        f"{BASE_URL}/payment/refund/ORD-61", json=_result(_refund_result())
    )
    requests_mock.delete(f"{BASE_URL}/payment/ORD-61", json=_result(None))
    booking = BookingApi(ApiClient(BASE_URL))

    assert booking.create_payment(TOKEN, payment_payload) == _payment_result()
    assert booking.mock_pay(TOKEN, payment_payload) == _payment_result(status="SUCCESS")
    assert booking.get_payment_status(TOKEN, "ORD-61") == _payment_result(status="SUCCESS")
    assert booking.apply_refund(TOKEN, refund_payload) == _refund_result()
    assert booking.get_refund_status(TOKEN, "ORD-61") == _refund_result()
    assert booking.cancel_payment(TOKEN, "ORD-61") is None

    requests = requests_mock.request_history
    assert [request.method for request in requests] == ["POST", "POST", "GET", "POST", "GET", "DELETE"]
    assert [request.url for request in requests] == [
        f"{BASE_URL}/payment/create",
        f"{BASE_URL}/payment/mock-pay",
        f"{BASE_URL}/payment/status/ORD-61",
        f"{BASE_URL}/payment/refund",
        f"{BASE_URL}/payment/refund/ORD-61",
        f"{BASE_URL}/payment/ORD-61",
    ]
    assert all(request.headers["Authorization"] == f"Bearer {TOKEN}" for request in requests)
    assert requests[0].json() == payment_payload
    assert requests[1].json() == payment_payload
    assert requests[3].json() == refund_payload


@pytest.mark.parametrize(
    ("method", "args", "kwargs"),
    [
        ("list_available_coupons", ("",), {}),
        ("list_available_coupons", (TOKEN,), {"order_amount": -1}),
        ("list_available_coupons", (TOKEN,), {"ticket_id": 0}),
        ("get_coupon_center", (TOKEN,), {"page": 0}),
        ("list_my_coupons", (TOKEN,), {"status": -1}),
        ("receive_coupon", (TOKEN, 0), {}),
        ("validate_coupon", (TOKEN, {"userCouponId": 0, "orderAmount": 1}), {}),
        ("validate_coupon", (TOKEN, {"userCouponId": 51, "orderAmount": -1}), {}),
        ("create_order", (TOKEN, {}), {}),
        ("create_order", (TOKEN, {**_order_payload(), "items": []}), {}),
        ("list_orders", (TOKEN,), {"page": 0}),
        ("get_order", (TOKEN, ""), {}),
        ("cancel_order", (TOKEN, 0), {}),
        ("create_payment", (TOKEN, {"orderNo": "", "amount": 1, "payChannel": "alipay"}), {}),
        ("mock_pay", (TOKEN, {"orderNo": "ORD-61", "amount": -1, "payChannel": "alipay"}), {}),
        ("get_payment_status", (TOKEN, ""), {}),
        ("apply_refund", (TOKEN, {"orderNo": "", "reason": "course refund"}), {}),
        ("get_refund_status", (TOKEN, ""), {}),
        ("cancel_payment", (TOKEN, ""), {}),
    ],
)
def test_commerce_wrappers_reject_invalid_arguments(method, args, kwargs):
    booking = BookingApi(ApiClient(BASE_URL))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args, **kwargs)


@pytest.mark.parametrize(
    ("method", "path", "data", "args"),
    [
        ("get_coupon_center", "/coupons/center", {"list": []}, (TOKEN,)),
        ("list_my_coupons", "/coupons/list", {"list": []}, (TOKEN,)),
        ("list_available_coupons", "/coupons", [{"id": 51}], (TOKEN,)),
        ("receive_coupon", "/coupons/receive", 0, (TOKEN, 41)),
        ("validate_coupon", "/coupons/validate", {"usable": "yes", "userCouponId": 51}, (TOKEN, {"userCouponId": 51, "orderAmount": 1})),
        ("create_order", "/orders", {"orderId": 61}, (TOKEN, _order_payload())),
        ("list_orders", "/orders", {"list": []}, (TOKEN,)),
        ("get_order", "/orders/61", {"id": 61}, (TOKEN, 61)),
        ("cancel_order", "/orders/61/cancel", {}, (TOKEN, 61)),
        ("create_payment", "/payment/create", {"orderNo": "ORD-61"}, (TOKEN, {"orderNo": "ORD-61", "amount": 1, "payChannel": "alipay"})),
        ("mock_pay", "/payment/mock-pay", {"orderNo": "ORD-61"}, (TOKEN, {"orderNo": "ORD-61", "amount": 1, "payChannel": "alipay"})),
        ("get_payment_status", "/payment/status/ORD-61", {"orderNo": "ORD-61"}, (TOKEN, "ORD-61")),
        ("apply_refund", "/payment/refund", {"orderNo": "ORD-61"}, (TOKEN, {"orderNo": "ORD-61", "reason": "course refund"})),
        ("get_refund_status", "/payment/refund/ORD-61", {"orderNo": "ORD-61"}, (TOKEN, "ORD-61")),
        ("cancel_payment", "/payment/ORD-61", {}, (TOKEN, "ORD-61")),
    ],
)
def test_commerce_wrappers_reject_malformed_response_data(requests_mock, method, path, data, args):
    http_method = {
        "get_coupon_center": "GET",
        "list_my_coupons": "GET",
        "list_available_coupons": "GET",
        "receive_coupon": "POST",
        "validate_coupon": "POST",
        "create_order": "POST",
        "list_orders": "GET",
        "get_order": "GET",
        "cancel_order": "PUT",
        "create_payment": "POST",
        "mock_pay": "POST",
        "get_payment_status": "GET",
        "apply_refund": "POST",
        "get_refund_status": "GET",
        "cancel_payment": "DELETE",
    }[method]
    requests_mock.register_uri(http_method, f"{BASE_URL}{path}", json=_result(data))
    booking = BookingApi(ApiClient(BASE_URL))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args)


def test_commerce_wrappers_preserve_http_and_business_errors(requests_mock):
    requests_mock.get(f"{BASE_URL}/coupons/center", status_code=401, text="unauthorized")
    requests_mock.get(f"{BASE_URL}/orders/61", json=_result(None, code=100600))
    booking = BookingApi(ApiClient(BASE_URL))

    with pytest.raises(ApiHttpError) as http_error:
        booking.get_coupon_center(TOKEN)
    assert http_error.value.status_code == 401

    with pytest.raises(ApiBusinessError) as business_error:
        booking.get_order(TOKEN, 61)
    assert business_error.value.code == 100600
