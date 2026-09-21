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

    def login_admin(self, username: str, password: str) -> dict[str, Any]:
        payload = {"username": _validate_non_empty_string(username, "username"),
                   "password": _validate_non_empty_string(password, "password")}
        data = self._client.post("/admin/login", json=payload)
        data = _validate_mapping(data, "admin login response")
        for field in ("userId", "expiresIn"):
            _validate_resource_id(data.get(field), field)
        for field in ("token", "username"):
            _validate_non_empty_string(data.get(field), field)
        if type(data.get("userLevel")) is not int or data["userLevel"] < 10:
            raise ValueError("admin login requires administrator userLevel")
        return data

    def list_admin_users(self, token: str, *, page: int = 1, page_size: int = 10,
                         keyword: str | None = None, status: int | None = None,
                         user_type: int | None = None) -> dict[str, Any]:
        params = _admin_page_params(page, page_size, keyword=keyword, status=status, userType=user_type)
        return self._admin_page("users", token, params, _admin_user)

    def get_admin_user(self, token: str, user_id: int) -> dict[str, Any]:
        return _admin_user(self._admin_get("users", token, user_id))

    def create_admin_user(self, token: str, payload: Mapping[str, object]) -> int:
        payload = _validate_mapping(payload, "admin user payload")
        for field in ("username", "password", "phone"):
            _validate_non_empty_string(payload.get(field), field)
        if re.fullmatch(r"1[3-9]\d{9}", payload["phone"]) is None:
            raise ValueError("phone must be an eleven-digit mobile number")
        return self._admin_create("users", token, payload)

    def update_admin_user_status(self, token: str, user_id: int, status: int) -> None:
        self._admin_status("users", token, user_id, status, {0, 1, 2})

    def list_admin_roles(self, token: str) -> list[dict[str, Any]]:
        data = self._client.get("/admin/roles", token=_validate_non_empty_string(token, "token"))
        _validate_resource_list(data, "admin roles")
        for role in data:
            _validate_non_empty_string(role.get("roleName"), "role name")
            _validate_non_empty_string(role.get("roleCode"), "role code")
            _admin_integer_fields(role, ("status",))
        return data

    def assign_admin_user_roles(self, token: str, user_id: int, role_ids: Iterable[int]) -> None:
        ids = _validate_ids(role_ids, "role ID")
        if not ids:
            raise ValueError("role IDs must not be empty")
        self._admin_mutation("PUT", "users", token, user_id, "role", {"roleIds": ids})

    def list_admin_tickets(self, token: str, *, page: int = 1, page_size: int = 10,
                           keyword: str | None = None, status: int | None = None,
                           category_id: int | None = None, is_hot: int | None = None) -> dict[str, Any]:
        params = _admin_page_params(page, page_size, keyword=keyword, status=status,
                                    categoryId=category_id, isHot=is_hot)
        return self._admin_page("tickets", token, params, _admin_ticket)

    def get_admin_ticket(self, token: str, ticket_id: int) -> dict[str, Any]:
        return _admin_ticket(self._admin_get("tickets", token, ticket_id))

    def create_admin_ticket(self, token: str, payload: Mapping[str, object]) -> int:
        payload = _validate_mapping(payload, "admin ticket payload")
        _validate_non_empty_string(payload.get("name"), "ticket name")
        if "status" in payload:
            _admin_enum(payload["status"], {0, 1, 2, 3, 4}, "ticket status")
        return self._admin_create("tickets", token, payload)

    def update_admin_ticket_status(self, token: str, ticket_id: int, status: int) -> None:
        self._admin_status("tickets", token, ticket_id, status, {0, 1, 2, 3, 4})

    def list_admin_orders(self, token: str, *, page: int = 1, page_size: int = 10,
                          order_no: str | None = None, user_id: int | None = None,
                          status: int | None = None, pay_status: int | None = None) -> dict[str, Any]:
        params = _admin_page_params(page, page_size, orderNo=order_no, userId=user_id,
                                    status=status, payStatus=pay_status)
        return self._admin_page("orders", token, params, _admin_order)

    def get_admin_order(self, token: str, order_id: int) -> dict[str, Any]:
        return _admin_order(self._admin_get("orders", token, order_id))

    def update_admin_order_status(self, token: str, order_id: int, status: int) -> None:
        """Send the real route; callers must verify persistence (backend TODO)."""
        _admin_enum(status, {1, 2, 3, 4, 5, 6}, "order status")
        self._admin_mutation("PUT", "orders", token, order_id, "status", None, {"status": status})

    def cancel_admin_order(self, token: str, order_id: int, reason: str) -> None:
        self._admin_mutation("PUT", "orders", token, order_id, "cancel",
                             {"cancelReason": _validate_non_empty_string(reason, "cancel reason")})

    def refund_admin_order(self, token: str, order_id: int, payload: Mapping[str, object]) -> None:
        payload = _validate_mapping(payload, "admin refund payload")
        if payload.get("refundAmount") is not None:
            _validate_non_negative_number(payload["refundAmount"], "refund amount")
        for field in ("reason", "proofImages"):
            if payload.get(field) is not None:
                _validate_non_empty_string(payload[field], field)
        self._admin_mutation("PUT", "orders", token, order_id, "refund", payload)

    def list_admin_coupons(self, token: str, *, page: int = 1, page_size: int = 10,
                           keyword: str | None = None, status: int | None = None) -> dict[str, Any]:
        return self._admin_page("coupons", token, _admin_page_params(page, page_size, keyword=keyword, status=status), _admin_coupon_item)

    def get_admin_coupon(self, token: str, coupon_id: int) -> dict[str, Any]:
        # Detail returns AdminCouponCreateRequest, deliberately without an id.
        return _admin_coupon(self._admin_get("coupons", token, coupon_id))

    def create_admin_coupon(self, token: str, payload: Mapping[str, object]) -> int:
        return self._admin_create("coupons", token, _admin_coupon_payload(payload))

    def update_admin_coupon(self, token: str, coupon_id: int, payload: Mapping[str, object]) -> None:
        self._admin_mutation("PUT", "coupons", token, coupon_id, "", _admin_coupon_payload(payload))

    def update_admin_coupon_status(self, token: str, coupon_id: int, status: int) -> None:
        self._admin_status("coupons", token, coupon_id, status, {0, 1, 2})

    def issue_admin_coupon(self, token: str, coupon_id: int, user_ids: Iterable[int],
                           *, issue_mode: str = "admin") -> None:
        ids = _validate_ids(user_ids, "user ID")
        if not ids:
            raise ValueError("user IDs must not be empty")
        payload = {"userIds": ids, "issueMode": _validate_non_empty_string(issue_mode, "issue mode")}
        self._admin_mutation("POST", "coupons", token, coupon_id, "issue", payload)

    def get_admin_overview_stats(self, token: str) -> dict[str, Any]:
        data = self._admin_stats("overview", token)
        _admin_integer_fields(data, ("todayOrders", "todayUsers", "todayRefunds", "pendingRefundCount", "pendingOrderCount"))
        for field in ("todaySales", "yesterdaySales"):
            _validate_non_negative_number(data.get(field), field)
        if not _is_number(data.get("salesGrowth")):
            raise ValueError("sales growth must be numeric")
        return data

    def get_admin_order_stats(self, token: str) -> dict[str, Any]:
        data = self._admin_stats("orders", token)
        _admin_integer_fields(data, ("totalOrders", "pendingPayOrders", "paidOrders", "completedOrders", "cancelledOrders", "refundingOrders"))
        _admin_trend(data.get("orderStatusDistribution"), "status")
        return data

    def get_admin_user_stats(self, token: str) -> dict[str, Any]:
        data = self._admin_stats("users", token)
        _admin_integer_fields(data, ("totalUsers", "activeUsers", "newUsersToday", "newUsersThisWeek", "newUsersThisMonth", "vipUsers"))
        _admin_trend(data.get("userGrowthTrend"), "date")
        return data

    def _admin_stats(self, kind: str, token: str) -> dict[str, Any]:
        return _validate_mapping(self._client.get(f"/admin/stats/{kind}", token=_validate_non_empty_string(token, "token")), "admin stats")

    def _admin_page(self, resource: str, token: str, params: dict, validator: Any) -> dict[str, Any]:
        return _validate_page(self._client.get(f"/admin/{resource}", token=_validate_non_empty_string(token, "token"), params=params), f"admin {resource}", validator)

    def _admin_get(self, resource: str, token: str, resource_id: int) -> Any:
        _validate_resource_id(resource_id, f"{resource} ID")
        return self._client.get(f"/admin/{resource}/{resource_id}", token=_validate_non_empty_string(token, "token"))

    def _admin_create(self, resource: str, token: str, payload: dict) -> int:
        data = self._client.post(f"/admin/{resource}", token=_validate_non_empty_string(token, "token"), json=payload)
        _validate_resource_id(data, f"created {resource} ID")
        return data

    def _admin_status(self, resource: str, token: str, resource_id: int, status: int, allowed: set[int]) -> None:
        _admin_enum(status, allowed, f"{resource} status")
        self._admin_mutation("PUT", resource, token, resource_id, "status", {"status": status})

    def _admin_mutation(self, method: str, resource: str, token: str, resource_id: int,
                        action: str, payload: dict | None, params: dict | None = None) -> None:
        _validate_resource_id(resource_id, f"{resource} ID")
        path = f"/admin/{resource}/{resource_id}" + (f"/{action}" if action else "")
        _validate_null_response(self._client.request(method, path, token=_validate_non_empty_string(token, "token"), json=payload, params=params), f"admin {resource} {action}")

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


def _admin_enum(value: object, allowed: set[int], name: str) -> None:
    if type(value) is not int or value not in allowed:
        raise ValueError(f"invalid {name}")


def _admin_page_params(page: int, page_size: int, **filters: object) -> dict[str, object]:
    params = _page_params(page, page_size)
    for field, value in filters.items():
        if value is None:
            continue
        if field in {"keyword", "orderNo"}:
            _validate_non_empty_string(value, field)
        elif field in {"categoryId", "userId"}:
            _validate_resource_id(value, field)
        else:
            _validate_non_negative_integer(value, field)
        params[field] = value
    return params


def _admin_integer_fields(data: Mapping, fields: Iterable[str]) -> None:
    for field in fields:
        _validate_non_negative_integer(data.get(field), field)


def _admin_user(data: object) -> dict[str, Any]:
    data = _validate_resource_object(data, "admin user")
    _validate_non_empty_string(data.get("username"), "username")
    _admin_integer_fields(data, ("status", "userType"))
    return data


def _admin_ticket(data: object) -> dict[str, Any]:
    data = _validate_resource_object(data, "admin ticket")
    _validate_non_empty_string(data.get("name"), "ticket name")
    _admin_integer_fields(data, ("status", "availableStock"))
    return data


def _admin_order(data: object) -> dict[str, Any]:
    data = _validate_resource_object(data, "admin order")
    _validate_non_empty_string(data.get("orderNo"), "order number")
    _validate_resource_id(data.get("userId"), "order user ID")
    _admin_integer_fields(data, ("orderStatus", "payStatus"))
    _validate_non_negative_number(data.get("payAmount"), "pay amount")
    return data


def _admin_coupon(data: object) -> dict[str, Any]:
    data = _validate_mapping(data, "admin coupon")
    _validate_non_empty_string(data.get("name"), "coupon name")
    _validate_non_negative_number(data.get("couponValue"), "coupon value")
    _admin_integer_fields(data, ("couponType", "status", "totalCount", "remainCount", "perLimit", "validityType"))
    return data


def _admin_coupon_item(data: object) -> dict[str, Any]:
    return _admin_coupon(_validate_resource_object(data, "admin coupon"))


def _admin_coupon_payload(payload: Mapping[str, object]) -> dict[str, object]:
    payload = _validate_mapping(payload, "admin coupon payload")
    _validate_non_empty_string(payload.get("name"), "coupon name")
    _validate_non_negative_number(payload.get("couponValue"), "coupon value")
    for field in ("totalCount", "perLimit"):
        _validate_resource_id(payload.get(field), field)
    _admin_enum(payload.get("couponType"), {1, 2, 3}, "coupon type")
    _admin_enum(payload.get("validityType"), {1, 2}, "validity type")
    _admin_enum(payload.get("status", 1), {0, 1, 2}, "coupon status")
    if payload["validityType"] == 1:
        for field in ("validStartDate", "validEndDate"):
            _validate_non_empty_string(payload.get(field), field)
    else:
        _validate_resource_id(payload.get("validDays"), "valid days")
    return payload


def _admin_trend(data: object, dimension: str) -> None:
    if not isinstance(data, list):
        raise ValueError("statistics trend must be a list")
    for item in data:
        item = _validate_mapping(item, "statistics trend item")
        _validate_non_negative_integer(item.get("count"), "trend count")
        if dimension == "date":
            _validate_non_empty_string(item.get("date"), "trend date")
        else:
            _validate_non_negative_integer(item.get("status"), "trend status")


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
