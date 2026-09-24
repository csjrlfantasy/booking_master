"""Build a browser-local customer session without exposing credentials."""

from __future__ import annotations

import json
import time
from urllib.parse import urlsplit


_CUSTOMER_TOKEN_KEY = "booking_customer_token"
_CUSTOMER_NAME_KEY = "booking_customer_name"
_CUSTOMER_USER_ID_KEY = "booking_customer_user_id"
_CUSTOMER_EXPIRES_AT_KEY = "booking_customer_expires_at"


def customer_init_script(
    *, token: str, user_id: int, name: str, expires_in: int, web_url: str
) -> tuple[str, dict[str, str | int]]:
    """Return a self-contained init script and its storage values for assertions."""
    _require_non_empty_string(token, "token")
    _require_positive_integer(user_id, "user_id")
    _require_non_empty_string(name, "name")
    _require_positive_integer(expires_in, "expires_in")
    _require_non_empty_string(web_url, "web_url")
    parsed_url = urlsplit(web_url)
    if parsed_url.scheme not in ("http", "https") or not parsed_url.hostname:
        raise ValueError("web_url must be an absolute HTTP(S) URL")

    values: dict[str, str | int] = {
        "token": token,
        "name": name,
        "userId": str(user_id),
        "expiresAt": int(time.time() * 1000) + expires_in * 1000,
    }
    serialized_values = json.dumps(values, ensure_ascii=True).replace("<", "\\u003c")
    script = f"""(() => {{
if (window.location.origin !== new URL({_json_string(web_url)}).origin) return;
const values = {serialized_values};
window.localStorage.setItem({_json_string(_CUSTOMER_TOKEN_KEY)}, values.token);
window.localStorage.setItem({_json_string(_CUSTOMER_NAME_KEY)}, values.name);
window.localStorage.setItem({_json_string(_CUSTOMER_USER_ID_KEY)}, values.userId);
window.localStorage.setItem({_json_string(_CUSTOMER_EXPIRES_AT_KEY)}, String(values.expiresAt));
}})();
"""
    return script, values


def _json_string(value: str) -> str:
    return json.dumps(value, ensure_ascii=True)


def _require_non_empty_string(value: object, name: str) -> None:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"{name} must be a non-empty string")


def _require_positive_integer(value: object, name: str) -> None:
    if type(value) is not int or value <= 0:
        raise ValueError(f"{name} must be a positive integer")
