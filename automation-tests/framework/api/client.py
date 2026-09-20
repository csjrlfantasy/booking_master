"""A small, safe client for the BookingApi Result envelope."""

from __future__ import annotations

import logging
from collections.abc import Mapping
from typing import Any

import requests


_SENSITIVE_KEYS = {
    "password",
    "token",
    "access_token",
    "authorization",
    "x-course-test-key",
}
_REDACTED = "***REDACTED***"


class ApiTransportError(RuntimeError):
    """The request could not reach the API."""


class ApiHttpError(RuntimeError):
    """The API returned a non-success HTTP status."""

    def __init__(self, status_code: int, redacted_summary: str) -> None:
        self.status_code = status_code
        self.redacted_summary = redacted_summary
        super().__init__(f"HTTP {status_code}: {redacted_summary}")


class ApiContractError(RuntimeError):
    """The API response was not a BookingApi Result envelope."""


class ApiBusinessError(RuntimeError):
    """The API returned a non-zero application code."""

    def __init__(self, code: int, message: object) -> None:
        self.code = code
        del message
        super().__init__(f"API business error {code}: message redacted")


def redact_sensitive(value: Any) -> Any:
    """Return a recursively redacted copy suitable for diagnostics."""
    if isinstance(value, Mapping):
        return {
            key: _REDACTED if str(key).casefold() in _SENSITIVE_KEYS else redact_sensitive(item)
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [redact_sensitive(item) for item in value]
    if isinstance(value, tuple):
        return tuple(redact_sensitive(item) for item in value)
    return value


class ApiClient:
    """Own a requests session and unwrap BookingApi Result envelopes."""

    def __init__(
        self,
        base_url: str,
        timeout_seconds: int = 30,
        *,
        session: requests.Session | None = None,
        logger: logging.Logger | None = None,
    ) -> None:
        self.base_url = _normalize_base_url(base_url)
        self.timeout_seconds = timeout_seconds
        self._session = session or requests.Session()
        self._logger = logger or logging.getLogger(__name__)

    def request(
        self,
        method: str,
        path: str,
        *,
        token: str | None = None,
        headers: Mapping[str, str] | None = None,
        params: Mapping[str, object] | None = None,
        json: object = None,
    ) -> Any:
        if not path.startswith("/"):
            raise ValueError("API paths must begin with '/'")

        request_headers = dict(headers or {})
        if token is not None:
            request_headers["Authorization"] = f"Bearer {token}"

        try:
            response = self._session.request(
                method=method,
                url=f"{self.base_url}{path}",
                headers=request_headers,
                params=params,
                json=json,
                timeout=self.timeout_seconds,
            )
        except requests.RequestException:
            raise ApiTransportError("request failed") from None

        if not response.ok:
            raise ApiHttpError(response.status_code, "response body redacted")

        try:
            envelope = response.json()
        except ValueError:
            envelope = None
        if envelope is None:
            raise ApiContractError("Result envelope must be valid JSON")

        if not isinstance(envelope, Mapping) or not {"code", "message", "data"}.issubset(envelope):
            raise ApiContractError("Result envelope must contain code, message, and data")

        code = envelope["code"]
        if type(code) is not int:
            raise ApiContractError("Result envelope code must be an integer")
        if code != 0:
            raise ApiBusinessError(code, envelope["message"])
        return envelope["data"]

    def get(self, path: str, **kwargs: Any) -> Any:
        return self.request("GET", path, **kwargs)

    def post(self, path: str, **kwargs: Any) -> Any:
        return self.request("POST", path, **kwargs)

    def put(self, path: str, **kwargs: Any) -> Any:
        return self.request("PUT", path, **kwargs)

    def delete(self, path: str, **kwargs: Any) -> Any:
        return self.request("DELETE", path, **kwargs)

    def close(self) -> None:
        self._session.close()

    def __enter__(self) -> "ApiClient":
        return self

    def __exit__(self, exc_type: object, exc_value: object, traceback: object) -> None:
        self.close()


def _normalize_base_url(base_url: str) -> str:
    if not isinstance(base_url, str):
        raise ValueError("base_url must be a string")
    normalized = base_url.rstrip("/")
    if not normalized:
        raise ValueError("base_url must not be empty")
    return normalized
