"""Ordered, de-duplicated cleanup registration for one test run."""

from __future__ import annotations

import re
from typing import Any


_RUN_ID_PATTERN = re.compile(r"[a-z0-9]{8,32}\Z")


class CleanupRegistry:
    """Collect resources created by a run and clean them in one API call."""

    def __init__(self, run_id: str, booking_api: Any, course_key: str | None = None) -> None:
        if not isinstance(run_id, str) or not _RUN_ID_PATTERN.fullmatch(run_id):
            raise ValueError("runId must match [a-z0-9]{8,32}")
        self._run_id = run_id
        self._booking_api = booking_api
        self._course_key = course_key
        self._user_ids: dict[int, None] = {}
        self._order_ids: dict[int, None] = {}
        self._ticket_ids: dict[int, None] = {}
        self._coupon_ids: dict[int, None] = {}

    def add_user(self, user_id: int) -> None:
        self._add(self._user_ids, user_id)

    def add_order(self, order_id: int) -> None:
        self._add(self._order_ids, order_id)

    def add_ticket(self, ticket_id: int) -> None:
        self._add(self._ticket_ids, ticket_id)

    def add_coupon(self, coupon_id: int) -> None:
        self._add(self._coupon_ids, coupon_id)

    def cleanup(self) -> dict[str, Any]:
        return self._booking_api.cleanup(
            self._run_id,
            user_ids=list(self._user_ids),
            order_ids=list(self._order_ids),
            ticket_ids=list(self._ticket_ids),
            coupon_ids=list(self._coupon_ids),
            course_key=self._course_key,
        )

    @staticmethod
    def _add(registry: dict[int, None], resource_id: object) -> None:
        if type(resource_id) is not int or resource_id <= 0:
            raise ValueError("resource ID must be a positive integer")
        registry[resource_id] = None
