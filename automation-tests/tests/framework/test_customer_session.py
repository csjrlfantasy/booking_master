import json

import pytest

from framework.auth.customer_session import customer_init_script


def test_customer_session_matches_frontend_storage_contract():
    script, values = customer_init_script(
        token="secret-token", user_id=7, name="course-user", expires_in=3600
    )

    assert "booking_customer_token" in script
    assert values["token"] == "secret-token"
    assert values["userId"] == "7"
    assert values["expiresAt"] > 0


@pytest.mark.parametrize(
    ("token", "user_id", "name", "expires_in"),
    [
        ("", 7, "course-user", 3600),
        ("secret-token", 0, "course-user", 3600),
        ("secret-token", 7, "", 3600),
        ("secret-token", 7, "course-user", 0),
    ],
)
def test_customer_session_rejects_invalid_values(token, user_id, name, expires_in):
    with pytest.raises(ValueError):
        customer_init_script(
            token=token, user_id=user_id, name=name, expires_in=expires_in
        )


def test_customer_session_serializes_hostile_values_as_data():
    token = 'quote " and </script><script>bad()</script>'
    name = 'customer " name </script><script>bad()</script>'

    script, values = customer_init_script(
        token=token, user_id=7, name=name, expires_in=3600
    )

    payload = json.loads(script.removeprefix("const values = ").split(";", 1)[0])
    assert payload == values
    assert "bad()" in payload["token"]
    assert "</script>" not in script
