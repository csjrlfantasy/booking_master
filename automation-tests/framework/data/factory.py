"""Factories for safely-owned course test data."""

from __future__ import annotations

import re
import uuid


_RUN_ID_PATTERN = re.compile(r"[a-z0-9]{8,32}\Z")


class TestDataFactory:
    """Create unique user records prefixed with one validated test run ID."""

    __test__ = False

    def __init__(self, run_id: str) -> None:
        if not isinstance(run_id, str) or not _RUN_ID_PATTERN.fullmatch(run_id):
            raise ValueError("runId must match [a-z0-9]{8,32}")
        self._prefix = f"pw-course-{run_id}-"

    def user(self) -> dict[str, str]:
        suffix = uuid.uuid4().hex
        phone = f"19{int(suffix, 16) % 1_000_000_000:09d}"
        return {
            "username": f"{self._prefix}{suffix[:12]}",
            "nickname": f"{self._prefix}{suffix[12:24]}",
            "phone": phone,
            "password": f"Course!{suffix[:16]}",
            "source": "h5",
        }
