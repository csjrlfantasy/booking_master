"""BookingApi routes built on the shared :class:`ApiClient`."""

from __future__ import annotations

import re
from collections.abc import Iterable, Mapping
from typing import Any
from urllib.parse import quote

from .client import ApiClient


_RUN_ID_PATTERN = re.compile(r"[a-z0-9]{8,32}\Z")


class BookingApi:
    """Typed facade for the course's supported BookingApi endpoints."""

    def __init__(self, client: ApiClient) -> None:
        self._client = client

    def register_user(self, payload: Mapping[str, object]) -> int:
        user_id = self._client.post("/user/register", json=dict(payload))
        _validate_resource_id(user_id, "user ID")
        return user_id

    def login_user(self, username: str, password: str) -> dict[str, Any]:
        data = self._client.post(
            "/user/login", json={"username": username, "password": password}
        )
        if not isinstance(data, dict):
            raise ValueError("login response data must be an object")
        return data

    def get_user(self, user_id: int, token: str) -> dict[str, Any]:
        _validate_resource_id(user_id, "user ID")
        data = self._client.get(f"/user/{user_id}", token=token)
        if not isinstance(data, dict):
            raise ValueError("user response data must be an object")
        return data

    def get_user_by_username(self, username: str) -> dict[str, Any]:
        username = _validate_non_empty_string(username, "username")
        data = self._client.get(f"/user/username/{quote(username, safe='')}")
        if not isinstance(data, dict):
            raise ValueError("user response data must be an object")
        return data

    def list_identities(self, token: str) -> list[dict[str, Any]]:
        token = _validate_non_empty_string(token, "token")
        data = self._client.get("/user/identities", token=token)
        if not isinstance(data, list) or not all(isinstance(item, dict) for item in data):
            raise ValueError("identity response data must be a list of objects")
        return data

    def create_identity(
        self, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_mapping(payload, "identity payload")
        data = self._client.post("/user/identities", token=token, json=payload)
        if not isinstance(data, dict):
            raise ValueError("identity response data must be an object")
        return data

    def update_identity(
        self, token: str, identity_id: int, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(identity_id, "identity ID")
        payload = _validate_mapping(payload, "identity payload")
        data = self._client.put(
            f"/user/identities/{identity_id}", token=token, json=payload
        )
        if not isinstance(data, dict):
            raise ValueError("identity response data must be an object")
        return data

    def delete_identity(self, token: str, identity_id: int) -> None:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(identity_id, "identity ID")
        data = self._client.delete(f"/user/identities/{identity_id}", token=token)
        if data is not None:
            raise ValueError("identity delete response data must be null")

    def list_tickets(
        self,
        *,
        page_num: int | None = None,
        page_size: int | None = None,
        category_id: int | None = None,
    ) -> dict[str, Any]:
        params = _catalog_page_params(page_num, page_size)
        if category_id is not None:
            _validate_resource_id(category_id, "category ID")
            params["categoryId"] = category_id
        return _validate_ticket_page(self._client.get("/tickets", params=params or None))

    def search_tickets(
        self,
        keyword: str,
        *,
        page_num: int | None = None,
        page_size: int | None = None,
    ) -> dict[str, Any]:
        params = _catalog_page_params(page_num, page_size)
        params["keyword"] = _validate_non_empty_string(keyword, "keyword")
        return _validate_ticket_page(
            self._client.get("/tickets/search", params=params)
        )

    def get_ticket(self, ticket_id: int) -> dict[str, Any]:
        _validate_resource_id(ticket_id, "ticket ID")
        return _validate_ticket_detail(self._client.get(f"/tickets/{ticket_id}"))

    def list_categories(self) -> list[dict[str, Any]]:
        return _validate_categories(self._client.get("/categories"))

    def get_hot_tickets(self, *, limit: int | None = None) -> list[dict[str, Any]]:
        return _validate_ticket_collection(
            self._client.get("/tickets/hot", params=_limit_param(limit))
        )

    def get_upcoming_tickets(
        self, *, limit: int | None = None
    ) -> list[dict[str, Any]]:
        return _validate_ticket_collection(
            self._client.get("/tickets/upcoming", params=_limit_param(limit))
        )

    def add_cart_item(self, token: str, payload: Mapping[str, object]) -> int:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_cart_add_payload(payload)
        data = self._client.post("/cart/add", token=token, json=payload)
        _validate_resource_id(data, "cart item ID")
        return data

    def list_cart_items(self, token: str) -> list[dict[str, Any]]:
        token = _validate_non_empty_string(token, "token")
        return _validate_cart_items(self._client.get("/cart", token=token))

    def get_cart_count(self, token: str) -> int:
        token = _validate_non_empty_string(token, "token")
        data = self._client.get("/cart/count", token=token)
        if type(data) is not int or data < 0:
            raise ValueError("cart count response data must be a non-negative integer")
        return data

    def update_cart_quantity(self, token: str, cart_id: int, quantity: int) -> None:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(cart_id, "cart ID")
        _validate_resource_id(quantity, "quantity")
        _validate_null_response(
            self._client.put(
                "/cart/quantity",
                token=token,
                json={"cartId": cart_id, "quantity": quantity},
            ),
            "cart quantity update",
        )

    def delete_cart_item(self, token: str, cart_id: int) -> None:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(cart_id, "cart ID")
        _validate_null_response(
            self._client.delete(f"/cart/{cart_id}", token=token), "cart delete"
        )

    def batch_delete_cart_items(
        self, token: str, cart_ids: Iterable[int]
    ) -> None:
        token = _validate_non_empty_string(token, "token")
        cart_ids = _validate_ids(cart_ids, "cart ID")
        if not cart_ids:
            raise ValueError("cart IDs must not be empty")
        _validate_null_response(
            self._client.delete("/cart/batch", token=token, json=cart_ids),
            "cart batch delete",
        )

    def clear_cart(self, token: str) -> None:
        token = _validate_non_empty_string(token, "token")
        _validate_null_response(
            self._client.delete("/cart", token=token), "cart clear"
        )

    def list_available_coupons(
        self,
        token: str,
        *,
        order_amount: int | float | None = None,
        ticket_id: int | None = None,
        category_id: int | None = None,
    ) -> list[dict[str, Any]]:
        token = _validate_non_empty_string(token, "token")
        params: dict[str, object] = {}
        if order_amount is not None:
            _validate_non_negative_number(order_amount, "order amount")
            params["orderAmount"] = order_amount
        if ticket_id is not None:
            _validate_resource_id(ticket_id, "ticket ID")
            params["ticketId"] = ticket_id
        if category_id is not None:
            _validate_resource_id(category_id, "category ID")
            params["categoryId"] = category_id
        return _validate_user_coupon_collection(
            self._client.get("/coupons", token=token, params=params or None)
        )

    def get_coupon_center(
        self, token: str, *, page: int = 1, page_size: int = 10
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        return _validate_coupon_center_page(
            self._client.get(
                "/coupons/center",
                token=token,
                params=_page_params(page, page_size),
            )
        )

    def list_my_coupons(
        self,
        token: str,
        *,
        status: int | None = None,
        page: int = 1,
        page_size: int = 10,
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        params: dict[str, int] = {}
        if status is not None:
            _validate_non_negative_integer(status, "coupon status")
            params["status"] = status
        params.update(_page_params(page, page_size))
        return _validate_user_coupon_page(
            self._client.get("/coupons/list", token=token, params=params)
        )

    def receive_coupon(self, token: str, coupon_id: int) -> int:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(coupon_id, "coupon ID")
        data = self._client.post(
            "/coupons/receive", token=token, params={"couponId": coupon_id}
        )
        _validate_resource_id(data, "user coupon ID")
        return data

    def validate_coupon(
        self, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_coupon_validation_payload(payload)
        return _validate_coupon_validation(
            self._client.post("/coupons/validate", token=token, json=payload)
        )

    def create_order(
        self, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_order_payload(payload)
        return _validate_created_order(
            self._client.post("/orders", token=token, json=payload)
        )

    def list_orders(
        self,
        token: str,
        *,
        status: int | None = None,
        page: int = 1,
        page_size: int = 10,
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        params: dict[str, int] = {}
        if status is not None:
            _validate_non_negative_integer(status, "order status")
            params["status"] = status
        params.update(_page_params(page, page_size))
        return _validate_order_page(
            self._client.get("/orders", token=token, params=params)
        )

    def get_order(self, token: str, order_id_or_no: int | str) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        order_path = _validate_order_identifier(order_id_or_no)
        return _validate_order_detail(
            self._client.get(f"/orders/{order_path}", token=token)
        )

    def cancel_order(
        self, token: str, order_id: int, *, cancel_reason: str | None = None
    ) -> None:
        token = _validate_non_empty_string(token, "token")
        _validate_resource_id(order_id, "order ID")
        payload: dict[str, object] | None = None
        if cancel_reason is not None:
            payload = {"cancelReason": _validate_non_empty_string(cancel_reason, "cancel reason")}
        _validate_null_response(
            self._client.put(
                f"/orders/{order_id}/cancel", token=token, json=payload
            ),
            "order cancel",
        )

    def create_payment(
        self, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        return self._submit_payment("/payment/create", token, payload)

    def mock_pay(self, token: str, payload: Mapping[str, object]) -> dict[str, Any]:
        return self._submit_payment("/payment/mock-pay", token, payload)

    def get_payment_status(self, token: str, order_no: str) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        order_no = _validate_non_empty_string(order_no, "order number")
        return _validate_payment_result(
            self._client.get(
                f"/payment/status/{quote(order_no, safe='')}", token=token
            )
        )

    def apply_refund(
        self, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_refund_payload(payload)
        return _validate_refund_result(
            self._client.post("/payment/refund", token=token, json=payload)
        )

    def get_refund_status(self, token: str, order_no: str) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        order_no = _validate_non_empty_string(order_no, "order number")
        return _validate_refund_result(
            self._client.get(
                f"/payment/refund/{quote(order_no, safe='')}", token=token
            )
        )

    def cancel_payment(self, token: str, order_no: str) -> None:
        token = _validate_non_empty_string(token, "token")
        order_no = _validate_non_empty_string(order_no, "order number")
        _validate_null_response(
            self._client.delete(
                f"/payment/{quote(order_no, safe='')}", token=token
            ),
            "payment cancel",
        )

    def _submit_payment(
        self, path: str, token: str, payload: Mapping[str, object]
    ) -> dict[str, Any]:
        token = _validate_non_empty_string(token, "token")
        payload = _validate_payment_payload(payload)
        return _validate_payment_result(
            self._client.post(path, token=token, json=payload)
        )

    def delete_test_user(self, run_id: str, user_id: int, course_key: str) -> dict[str, Any]:
        _validate_run_id(run_id)
        _validate_resource_id(user_id, "user ID")
        data = self._client.delete(
            f"/test-support/users/{user_id}",
            params={"runId": run_id},
            headers={"X-Course-Test-Key": course_key},
        )
        if not isinstance(data, dict):
            raise ValueError("cleanup response data must be an object")
        return data

    def cleanup(
        self,
        run_id: str,
        *,
        user_ids: Iterable[int] = (),
        order_ids: Iterable[int] = (),
        ticket_ids: Iterable[int] = (),
        coupon_ids: Iterable[int] = (),
        course_key: str,
    ) -> dict[str, Any]:
        _validate_run_id(run_id)
        payload = {
            "runId": run_id,
            "userIds": _validate_ids(user_ids, "user ID"),
            "orderIds": _validate_ids(order_ids, "order ID"),
            "ticketIds": _validate_ids(ticket_ids, "ticket ID"),
            "couponIds": _validate_ids(coupon_ids, "coupon ID"),
        }
        data = self._client.post(
            "/test-support/cleanup",
            headers={"X-Course-Test-Key": course_key},
            json=payload,
        )
        if not isinstance(data, dict):
            raise ValueError("cleanup response data must be an object")
        return data


def _validate_run_id(run_id: str) -> None:
    if not isinstance(run_id, str) or not _RUN_ID_PATTERN.fullmatch(run_id):
        raise ValueError("runId must match [a-z0-9]{8,32}")


def _validate_resource_id(resource_id: object, name: str) -> None:
    if type(resource_id) is not int or resource_id <= 0:
        raise ValueError(f"{name} must be a positive integer")


def _validate_non_empty_string(value: object, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{name} must be a non-empty string")
    return value


def _validate_mapping(value: object, name: str) -> dict[str, object]:
    if not isinstance(value, Mapping):
        raise ValueError(f"{name} must be a mapping")
    return dict(value)


def _validate_non_negative_integer(value: object, name: str) -> int:
    if type(value) is not int or value < 0:
        raise ValueError(f"{name} must be a non-negative integer")
    return value


def _validate_non_negative_number(value: object, name: str) -> int | float:
    if not _is_number(value) or value < 0:
        raise ValueError(f"{name} must be a non-negative number")
    return value


def _validate_ids(resource_ids: Iterable[int], name: str) -> list[int]:
    values = list(resource_ids)
    for resource_id in values:
        _validate_resource_id(resource_id, name)
    return values


def _catalog_page_params(
    page_num: int | None, page_size: int | None
) -> dict[str, int]:
    params: dict[str, int] = {}
    if page_num is not None:
        _validate_resource_id(page_num, "page number")
        params["pageNum"] = page_num
    if page_size is not None:
        _validate_resource_id(page_size, "page size")
        params["pageSize"] = page_size
    return params


def _page_params(page: int, page_size: int) -> dict[str, int]:
    _validate_resource_id(page, "page")
    _validate_resource_id(page_size, "page size")
    return {"page": page, "pageSize": page_size}


def _limit_param(limit: int | None) -> dict[str, int] | None:
    if limit is None:
        return None
    _validate_resource_id(limit, "limit")
    return {"limit": limit}


def _validate_ticket_page(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("ticket page response data must be an object")
    required = ("list", "pageNum", "pageSize", "total", "pages", "hasPrevious", "hasNext")
    if not all(field in data for field in required):
        raise ValueError("ticket page response data is missing pagination fields")
    _validate_ticket_collection(data["list"])
    if type(data["pageNum"]) is not int or data["pageNum"] <= 0:
        raise ValueError("ticket page number must be a positive integer")
    if type(data["pageSize"]) is not int or data["pageSize"] <= 0:
        raise ValueError("ticket page size must be a positive integer")
    if type(data["total"]) is not int or data["total"] < 0:
        raise ValueError("ticket page total must be a non-negative integer")
    if type(data["pages"]) is not int or data["pages"] < 0:
        raise ValueError("ticket page pages must be a non-negative integer")
    if type(data["hasPrevious"]) is not bool or type(data["hasNext"]) is not bool:
        raise ValueError("ticket page navigation fields must be booleans")
    return data


def _validate_ticket_collection(data: object) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        raise ValueError("ticket response data must be a list of objects")
    for item in data:
        _validate_ticket_list_item(item)
    return data


def _validate_ticket_list_item(data: object) -> dict[str, Any]:
    data = _validate_resource_object(data, "ticket response")
    _validate_non_empty_string(data.get("title"), "ticket title")
    return data


def _validate_ticket_detail(data: object) -> dict[str, Any]:
    data = _validate_resource_object(data, "ticket detail")
    _validate_non_empty_string(data.get("name"), "ticket name")
    return data


def _validate_categories(data: object) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        raise ValueError("category response data must be a list of objects")
    for category in data:
        category = _validate_resource_object(category, "category response")
        _validate_non_empty_string(category.get("name"), "category name")
        count = category.get("count")
        if type(count) is not int or count < 0:
            raise ValueError("category count must be a non-negative integer")
    return data


def _validate_cart_items(data: object) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        raise ValueError("cart item response data must be a list of objects")
    for item in data:
        item = _validate_resource_object(item, "cart item response")
        for field, label in (
            ("userId", "cart user ID"),
            ("ticketId", "cart ticket ID"),
            ("sessionId", "cart session ID"),
            ("priceLevelId", "cart price level ID"),
            ("quantity", "cart quantity"),
        ):
            _validate_resource_id(item.get(field), label)
        for field in ("unitPrice", "subtotal"):
            if not _is_number(item.get(field)):
                raise ValueError(f"cart {field} must be a number")
        if type(item.get("status")) is not int:
            raise ValueError("cart status must be an integer")
    return data


def _validate_resource_list(data: object, name: str) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        raise ValueError(f"{name} data must be a list of objects")
    for item in data:
        _validate_resource_object(item, name)
    return data


def _validate_resource_object(data: object, name: str) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError(f"{name} data must be an object")
    _validate_resource_id(data.get("id"), f"{name} ID")
    return data


def _is_number(value: object) -> bool:
    return isinstance(value, (int, float)) and not isinstance(value, bool)


def _validate_cart_add_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "cart add payload")
    for field, label in (
        ("ticketId", "ticket ID"),
        ("sessionId", "session ID"),
        ("priceLevelId", "price level ID"),
        ("quantity", "quantity"),
    ):
        _validate_resource_id(payload.get(field), label)
    if payload.get("unitPrice") is None:
        raise ValueError("unitPrice must be provided")
    return payload


def _validate_null_response(data: object, name: str) -> None:
    if data is not None:
        raise ValueError(f"{name} response data must be null")


def _validate_page(
    data: object, name: str, item_validator: Any
) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError(f"{name} response data must be an object")
    required = ("list", "pageNum", "pageSize", "total", "pages", "hasPrevious", "hasNext")
    if not all(field in data for field in required):
        raise ValueError(f"{name} response data is missing pagination fields")
    if not isinstance(data["list"], list):
        raise ValueError(f"{name} response list must be a list")
    for item in data["list"]:
        item_validator(item)
    if type(data["pageNum"]) is not int or data["pageNum"] <= 0:
        raise ValueError(f"{name} page number must be a positive integer")
    if type(data["pageSize"]) is not int or data["pageSize"] <= 0:
        raise ValueError(f"{name} page size must be a positive integer")
    if type(data["total"]) is not int or data["total"] < 0:
        raise ValueError(f"{name} total must be a non-negative integer")
    if type(data["pages"]) is not int or data["pages"] < 0:
        raise ValueError(f"{name} pages must be a non-negative integer")
    if type(data["hasPrevious"]) is not bool or type(data["hasNext"]) is not bool:
        raise ValueError(f"{name} navigation fields must be booleans")
    return data


def _validate_coupon_center_page(data: object) -> dict[str, Any]:
    return _validate_page(data, "coupon center", _validate_coupon_center_item)


def _validate_coupon_center_item(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("coupon center item must be an object")
    _validate_resource_id(data.get("couponId"), "coupon center coupon ID")
    _validate_non_empty_string(data.get("couponName"), "coupon center coupon name")
    if type(data.get("couponType")) is not int:
        raise ValueError("coupon center coupon type must be an integer")
    if not _is_number(data.get("couponValue")):
        raise ValueError("coupon center coupon value must be a number")
    if "received" in data and type(data["received"]) is not bool:
        raise ValueError("coupon center received must be a boolean")
    return data


def _validate_user_coupon_page(data: object) -> dict[str, Any]:
    return _validate_page(data, "user coupon", _validate_user_coupon_item)


def _validate_user_coupon_collection(data: object) -> list[dict[str, Any]]:
    if not isinstance(data, list):
        raise ValueError("user coupon response data must be a list of objects")
    for item in data:
        _validate_user_coupon_item(item)
    return data


def _validate_user_coupon_item(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("user coupon item must be an object")
    _validate_resource_id(data.get("id"), "user coupon ID")
    _validate_resource_id(data.get("couponId"), "coupon template ID")
    _validate_non_empty_string(data.get("couponName"), "coupon name")
    if type(data.get("couponType")) is not int:
        raise ValueError("coupon type must be an integer")
    if not _is_number(data.get("couponValue")):
        raise ValueError("coupon value must be a number")
    if type(data.get("status")) is not int:
        raise ValueError("coupon status must be an integer")
    return data


def _validate_coupon_validation_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "coupon validation payload")
    _validate_resource_id(payload.get("userCouponId"), "user coupon ID")
    _validate_non_negative_number(payload.get("orderAmount"), "order amount")
    if "ticketId" in payload and payload["ticketId"] is not None:
        _validate_resource_id(payload["ticketId"], "ticket ID")
    if "categoryId" in payload and payload["categoryId"] is not None:
        _validate_resource_id(payload["categoryId"], "category ID")
    return payload


def _validate_coupon_validation(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("coupon validation response data must be an object")
    if type(data.get("usable")) is not bool:
        raise ValueError("coupon validation usable must be a boolean")
    _validate_resource_id(data.get("userCouponId"), "coupon validation user coupon ID")
    if "couponId" in data and data["couponId"] is not None:
        _validate_resource_id(data["couponId"], "coupon validation coupon ID")
    if "discountAmount" in data and data["discountAmount"] is not None and not _is_number(data["discountAmount"]):
        raise ValueError("coupon validation discount amount must be a number")
    return data


def _validate_order_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "order payload")
    _validate_resource_id(payload.get("ticketId"), "order ticket ID")
    _validate_resource_id(payload.get("sessionId"), "order session ID")
    _validate_non_empty_string(payload.get("contactName"), "order contact name")
    _validate_non_empty_string(payload.get("contactPhone"), "order contact phone")
    items = payload.get("items")
    if not isinstance(items, list) or not items:
        raise ValueError("order items must be a non-empty list")
    for item in items:
        if not isinstance(item, Mapping):
            raise ValueError("order item must be a mapping")
        _validate_resource_id(item.get("priceLevelId"), "order price level ID")
        _validate_resource_id(item.get("quantity"), "order item quantity")
        _validate_non_negative_number(item.get("unitPrice"), "order item unit price")
    if "userCouponId" in payload and payload["userCouponId"] is not None:
        _validate_resource_id(payload["userCouponId"], "order user coupon ID")
    return payload


def _validate_created_order(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("created order response data must be an object")
    _validate_resource_id(data.get("orderId"), "created order ID")
    _validate_non_empty_string(data.get("orderNo"), "created order number")
    _validate_non_negative_number(data.get("totalAmount"), "created order total amount")
    _validate_non_negative_number(data.get("payAmount"), "created order pay amount")
    return data


def _validate_order_page(data: object) -> dict[str, Any]:
    return _validate_page(data, "order", _validate_order_list_item)


def _validate_order_list_item(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("order list item must be an object")
    _validate_resource_id(data.get("id"), "order ID")
    _validate_non_empty_string(data.get("orderNo"), "order number")
    _validate_resource_id(data.get("ticketId"), "order ticket ID")
    _validate_non_empty_string(data.get("ticketName"), "order ticket name")
    _validate_resource_id(data.get("totalQuantity"), "order total quantity")
    _validate_non_negative_number(data.get("payAmount"), "order pay amount")
    if type(data.get("orderStatus")) is not int or type(data.get("payStatus")) is not int:
        raise ValueError("order status fields must be integers")
    return data


def _validate_order_detail(data: object) -> dict[str, Any]:
    _validate_order_list_item(data)
    if not isinstance(data, dict):
        raise ValueError("order detail response data must be an object")
    _validate_resource_id(data.get("userId"), "order user ID")
    if "items" in data and data["items"] is not None and not isinstance(data["items"], list):
        raise ValueError("order detail items must be a list")
    return data


def _validate_order_identifier(value: int | str) -> str:
    if type(value) is int:
        _validate_resource_id(value, "order ID")
        return str(value)
    return quote(_validate_non_empty_string(value, "order identifier"), safe="")


def _validate_payment_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "payment payload")
    _validate_non_empty_string(payload.get("orderNo"), "payment order number")
    _validate_non_negative_number(payload.get("amount"), "payment amount")
    _validate_non_empty_string(payload.get("payChannel"), "payment channel")
    return payload


def _validate_payment_result(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("payment response data must be an object")
    _validate_non_empty_string(data.get("orderNo"), "payment order number")
    _validate_non_negative_number(data.get("amount"), "payment amount")
    _validate_non_empty_string(data.get("status"), "payment status")
    if "paymentId" in data and data["paymentId"] is not None:
        _validate_resource_id(data["paymentId"], "payment ID")
    return data


def _validate_refund_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "refund payload")
    _validate_non_empty_string(payload.get("orderNo"), "refund order number")
    if "refundAmount" in payload and payload["refundAmount"] is not None:
        _validate_non_empty_string(payload["refundAmount"], "refund amount")
    if "reason" in payload and payload["reason"] is not None:
        _validate_non_empty_string(payload["reason"], "refund reason")
    return payload


def _validate_refund_result(data: object) -> dict[str, Any]:
    if not isinstance(data, dict):
        raise ValueError("refund response data must be an object")
    _validate_non_empty_string(data.get("orderNo"), "refund order number")
    _validate_non_negative_number(data.get("refundAmount"), "refund amount")
    _validate_non_empty_string(data.get("status"), "refund status")
    if "refundId" in data and data["refundId"] is not None:
        _validate_resource_id(data["refundId"], "refund ID")
    return data
