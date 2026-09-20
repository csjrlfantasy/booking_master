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
