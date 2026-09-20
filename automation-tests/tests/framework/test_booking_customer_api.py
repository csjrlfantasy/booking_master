import pytest

from framework.api.booking import BookingApi
from framework.api.client import ApiClient


def _result(data):
    return {"code": 0, "message": "ok", "data": data}


def test_get_user_by_username_url_encodes_the_path_segment(requests_mock):
    requests_mock.get(
        "http://example.test/api/user/username/course%20learner%2Fone",
        json=_result({"userId": 9, "username": "course learner/one"}),
    )
    booking = BookingApi(ApiClient("http://example.test/api"))

    user = booking.get_user_by_username("course learner/one")

    assert user == {"userId": 9, "username": "course learner/one"}
    request = requests_mock.request_history[0]
    assert request.method == "GET"
    assert request.url == "http://example.test/api/user/username/course%20learner%2Fone"


def test_identity_wrappers_use_exact_routes_bearer_headers_and_json(requests_mock):
    base_url = "http://example.test/api/user/identities"
    create_payload = {
        "realName": "TEACHING-ONLY-A",
        "idCardNo": "110101200001010011",
        "phone": "19000000001",
        "self": True,
        "defaultIdentity": True,
    }
    update_payload = {
        "realName": "TEACHING-ONLY-B",
        "idCardNo": "110101200001010012",
        "phone": "19000000002",
        "self": False,
        "defaultIdentity": False,
    }
    requests_mock.get(base_url, json=_result([]))
    requests_mock.post(base_url, json=_result({"id": 12}))
    requests_mock.put(f"{base_url}/12", json=_result({"id": 12}))
    requests_mock.delete(f"{base_url}/12", json=_result(None))
    booking = BookingApi(ApiClient("http://example.test/api"))

    assert booking.list_identities("synthetic-token") == []
    assert booking.create_identity("synthetic-token", create_payload) == {"id": 12}
    assert booking.update_identity("synthetic-token", 12, update_payload) == {"id": 12}
    assert booking.delete_identity("synthetic-token", 12) is None

    list_request, create_request, update_request, delete_request = requests_mock.request_history
    assert [request.method for request in requests_mock.request_history] == [
        "GET",
        "POST",
        "PUT",
        "DELETE",
    ]
    assert [request.url for request in requests_mock.request_history] == [
        base_url,
        base_url,
        f"{base_url}/12",
        f"{base_url}/12",
    ]
    assert all(
        request.headers["Authorization"] == "Bearer synthetic-token"
        for request in (list_request, create_request, update_request, delete_request)
    )
    assert create_request.json() == create_payload
    assert update_request.json() == update_payload


@pytest.mark.parametrize(
    ("method", "args"),
    [
        ("get_user_by_username", ("",)),
        ("list_identities", ("",)),
        ("create_identity", ("", {"realName": "TEACHING-ONLY"})),
        ("create_identity", ("synthetic-token", [])),
        ("update_identity", ("", 1, {"realName": "TEACHING-ONLY"})),
        ("update_identity", ("synthetic-token", 0, {"realName": "TEACHING-ONLY"})),
        ("update_identity", ("synthetic-token", 1, [])),
        ("delete_identity", ("", 1)),
        ("delete_identity", ("synthetic-token", 0)),
    ],
)
def test_customer_identity_wrappers_reject_invalid_arguments(method, args):
    booking = BookingApi(ApiClient("http://example.test/api"))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args)


@pytest.mark.parametrize(
    ("method", "path", "data", "args"),
    [
        ("get_user_by_username", "/user/username/course", [], ("course",)),
        ("list_identities", "/user/identities", {}, ("synthetic-token",)),
        (
            "create_identity",
            "/user/identities",
            [],
            ("synthetic-token", {"realName": "TEACHING-ONLY"}),
        ),
        (
            "update_identity",
            "/user/identities/7",
            [],
            ("synthetic-token", 7, {"realName": "TEACHING-ONLY"}),
        ),
        ("delete_identity", "/user/identities/7", {}, ("synthetic-token", 7)),
    ],
)
def test_customer_identity_wrappers_reject_invalid_response_shapes(
    requests_mock, method, path, data, args
):
    requests_mock.register_uri(
        {"get_user_by_username": "GET", "list_identities": "GET", "create_identity": "POST", "update_identity": "PUT", "delete_identity": "DELETE"}[method],
        f"http://example.test/api{path}",
        json=_result(data),
    )
    booking = BookingApi(ApiClient("http://example.test/api"))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args)
