"""BookingApi routes built on the shared :class:`ApiClient`."""

from __future__ import annotations

import re
from collections.abc import Iterable, Mapping
from typing import Any

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


def _validate_ids(resource_ids: Iterable[int], name: str) -> list[int]:
    values = list(resource_ids)
    for resource_id in values:
        _validate_resource_id(resource_id, name)
    return values
