"""Mocked contracts for current catalog and cart controller routes."""

from __future__ import annotations

import pytest

from framework.api.booking import BookingApi
from framework.api.client import ApiClient


def _result(data):
    return {"code": 0, "message": "ok", "data": data}


def _page(items):
    return {
        "list": items,
        "pageNum": 2,
        "pageSize": 5,
        "total": 6,
        "pages": 2,
        "hasPrevious": True,
        "hasNext": False,
    }


def test_catalog_wrappers_use_current_controller_routes_queries_and_shapes(requests_mock):
    base_url = "http://example.test/api"
    ticket = {"id": 7, "title": "Course concert", "status": 2}
    detail = {
        "id": 7,
        "name": "Course concert",
        "sessions": [{"id": 11, "status": 2}],
        "priceLevels": [{"id": 13, "name": "VIP", "price": 99.0}],
    }
    category = {"id": 3, "name": "Music", "count": 1}
    requests_mock.get(f"{base_url}/tickets", json=_result(_page([ticket])))
    requests_mock.get(f"{base_url}/tickets/search", json=_result(_page([ticket])))
    requests_mock.get(f"{base_url}/tickets/7", json=_result(detail))
    requests_mock.get(f"{base_url}/categories", json=_result([category]))
    requests_mock.get(f"{base_url}/tickets/hot", json=_result([ticket]))
    requests_mock.get(f"{base_url}/tickets/upcoming", json=_result([ticket]))
    booking = BookingApi(ApiClient(base_url))

    assert booking.list_tickets(page_num=2, page_size=5, category_id=3) == _page([ticket])
    assert booking.search_tickets("concert", page_num=2, page_size=5) == _page([ticket])
    assert booking.get_ticket(7) == detail
    assert booking.list_categories() == [category]
    assert booking.get_hot_tickets(limit=4) == [ticket]
    assert booking.get_upcoming_tickets(limit=4) == [ticket]

    list_request, search_request, detail_request, categories_request, hot_request, upcoming_request = requests_mock.request_history
    assert list_request.method == "GET"
    assert list_request.url == f"{base_url}/tickets?pageNum=2&pageSize=5&categoryId=3"
    assert search_request.url == f"{base_url}/tickets/search?pageNum=2&pageSize=5&keyword=concert"
    assert detail_request.url == f"{base_url}/tickets/7"
    assert categories_request.url == f"{base_url}/categories"
    assert hot_request.url == f"{base_url}/tickets/hot?limit=4"
    assert upcoming_request.url == f"{base_url}/tickets/upcoming?limit=4"


def test_cart_wrappers_use_current_controller_routes_bearer_headers_and_json(requests_mock):
    base_url = "http://example.test/api"
    payload = {
        "ticketId": 7,
        "sessionId": 11,
        "priceLevelId": 13,
        "priceLevelName": "VIP",
        "quantity": 2,
        "unitPrice": 99.0,
    }
    cart_item = {"id": 17, **payload}
    requests_mock.post(f"{base_url}/cart/add", json=_result(17))
    requests_mock.get(f"{base_url}/cart", json=_result([cart_item]))
    requests_mock.get(f"{base_url}/cart/count", json=_result(2))
    requests_mock.put(f"{base_url}/cart/quantity", json=_result(None))
    requests_mock.delete(f"{base_url}/cart/17", json=_result(None))
    requests_mock.delete(f"{base_url}/cart/batch", json=_result(None))
    requests_mock.delete(f"{base_url}/cart", json=_result(None))
    booking = BookingApi(ApiClient(base_url))

    assert booking.add_cart_item("synthetic-token", payload) == 17
    assert booking.list_cart_items("synthetic-token") == [cart_item]
    assert booking.get_cart_count("synthetic-token") == 2
    assert booking.update_cart_quantity("synthetic-token", 17, 3) is None
    assert booking.delete_cart_item("synthetic-token", 17) is None
    assert booking.batch_delete_cart_items("synthetic-token", [17, 18]) is None
    assert booking.clear_cart("synthetic-token") is None

    requests = requests_mock.request_history
    assert [request.method for request in requests] == ["POST", "GET", "GET", "PUT", "DELETE", "DELETE", "DELETE"]
    assert [request.url for request in requests] == [
        f"{base_url}/cart/add",
        f"{base_url}/cart",
        f"{base_url}/cart/count",
        f"{base_url}/cart/quantity",
        f"{base_url}/cart/17",
        f"{base_url}/cart/batch",
        f"{base_url}/cart",
    ]
    assert all(request.headers["Authorization"] == "Bearer synthetic-token" for request in requests)
    assert requests[0].json() == payload
    assert requests[3].json() == {"cartId": 17, "quantity": 3}
    assert requests[5].json() == [17, 18]


@pytest.mark.parametrize(
    ("method", "args", "kwargs"),
    [
        ("list_tickets", (), {"page_num": 0}),
        ("list_tickets", (), {"page_size": 0}),
        ("list_tickets", (), {"category_id": 0}),
        ("search_tickets", ("",), {}),
        ("search_tickets", ("concert",), {"page_num": 0}),
        ("get_ticket", (0,), {}),
        ("get_hot_tickets", (), {"limit": 0}),
        ("get_upcoming_tickets", (), {"limit": 0}),
        ("add_cart_item", ("", {"ticketId": 7, "sessionId": 11, "priceLevelId": 13, "quantity": 1, "unitPrice": 1}), {}),
        ("add_cart_item", ("token", {"ticketId": 0, "sessionId": 11, "priceLevelId": 13, "quantity": 1, "unitPrice": 1}), {}),
        ("add_cart_item", ("token", {"ticketId": 7, "sessionId": 0, "priceLevelId": 13, "quantity": 1, "unitPrice": 1}), {}),
        ("add_cart_item", ("token", {"ticketId": 7, "sessionId": 11, "priceLevelId": 0, "quantity": 1, "unitPrice": 1}), {}),
        ("add_cart_item", ("token", {"ticketId": 7, "sessionId": 11, "priceLevelId": 13, "quantity": 0, "unitPrice": 1}), {}),
        ("update_cart_quantity", ("", 17, 1), {}),
        ("update_cart_quantity", ("token", 0, 1), {}),
        ("update_cart_quantity", ("token", 17, 0), {}),
        ("delete_cart_item", ("", 17), {}),
        ("delete_cart_item", ("token", 0), {}),
        ("batch_delete_cart_items", ("", [17]), {}),
        ("batch_delete_cart_items", ("token", []), {}),
        ("batch_delete_cart_items", ("token", [0]), {}),
        ("clear_cart", ("",), {}),
    ],
)
def test_catalog_and_cart_wrappers_reject_invalid_arguments(method, args, kwargs):
    booking = BookingApi(ApiClient("http://example.test/api"))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args, **kwargs)


@pytest.mark.parametrize(
    ("method", "path", "data", "args"),
    [
        ("list_tickets", "/tickets", {}, ()),
        ("search_tickets", "/tickets/search", {"list": []}, ("concert",)),
        ("get_ticket", "/tickets/7", [], (7,)),
        ("list_categories", "/categories", {}, ()),
        ("get_hot_tickets", "/tickets/hot", [{}], ()),
        ("get_upcoming_tickets", "/tickets/upcoming", [{}], ()),
        ("add_cart_item", "/cart/add", 0, ("token", {"ticketId": 7, "sessionId": 11, "priceLevelId": 13, "quantity": 1, "unitPrice": 1})),
        ("list_cart_items", "/cart", {}, ("token",)),
        ("get_cart_count", "/cart/count", -1, ("token",)),
        ("update_cart_quantity", "/cart/quantity", {}, ("token", 17, 1)),
        ("delete_cart_item", "/cart/17", {}, ("token", 17)),
        ("batch_delete_cart_items", "/cart/batch", {}, ("token", [17])),
        ("clear_cart", "/cart", {}, ("token",)),
    ],
)
def test_catalog_and_cart_wrappers_reject_malformed_response_data(requests_mock, method, path, data, args):
    requests_mock.register_uri(
        {"list_tickets": "GET", "search_tickets": "GET", "get_ticket": "GET", "list_categories": "GET", "get_hot_tickets": "GET", "get_upcoming_tickets": "GET", "add_cart_item": "POST", "list_cart_items": "GET", "get_cart_count": "GET", "update_cart_quantity": "PUT", "delete_cart_item": "DELETE", "batch_delete_cart_items": "DELETE", "clear_cart": "DELETE"}[method],
        f"http://example.test/api{path}",
        json=_result(data),
    )
    booking = BookingApi(ApiClient("http://example.test/api"))

    with pytest.raises(ValueError):
        getattr(booking, method)(*args)
