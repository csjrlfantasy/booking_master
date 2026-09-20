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
