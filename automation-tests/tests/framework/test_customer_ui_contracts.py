"""Offline checks for UI resource tracking, real routes and safety contracts."""

import ast
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest

from pages.customer import checkout_panel, login_page, order_panel, shop_page


ROOT = Path(__file__).resolve().parents[2]
FRONTEND = ROOT.parent / "frontend" / "src"


def _page_for(response):
    page = MagicMock()

    @contextmanager
    def expect_response(predicate):
        assert predicate(response), "page object must match the actual endpoint/method"
        wrong = SimpleNamespace(url=response.url, request=SimpleNamespace(method="GET"))
        assert not predicate(wrong), "mutations must not capture a background GET"
        yield SimpleNamespace(value=response)

    page.expect_response.side_effect = expect_response
    return page


@pytest.mark.parametrize("kind", ["registration", "checkout"])
def test_created_resource_is_registered_before_a_browser_assertion_can_fail(monkeypatch, kind):
    data = 71 if kind == "registration" else {"orderId": 72, "orderNo": "OWNED-ORDER"}
    suffix = "/user/register" if kind == "registration" else "/orders"
    response = SimpleNamespace(url=f"http://example.test/api{suffix}", request=SimpleNamespace(method="POST"),
                               ok=True, json=lambda: {"code": 0, "data": data})
    page = _page_for(response)
    registry = MagicMock()
    if kind == "registration":
        # Force the first *post-response* UI assertion in the calling test to fail.
        monkeypatch.setattr(login_page, "expect", lambda _: MagicMock())
        login = login_page.LoginPage(page)
        assert login.register({"username": "run-owned", "password": "temporary", "phone": "19000000000"}, registry) == 71
        registry.add_user.assert_called_once_with(71)
    else:
        checks = MagicMock()
        checks.to_be_visible.side_effect = AssertionError("UI rendering failed after creation")
        monkeypatch.setattr(checkout_panel, "expect", lambda _: checks)
        with pytest.raises(AssertionError, match="rendering failed"):
            checkout_panel.CheckoutPanel(page).submit(registry)
        registry.add_order.assert_called_once_with(72)


@pytest.mark.parametrize("kind", ["registration", "checkout"])
def test_rejected_creation_does_not_register_an_invalid_resource(monkeypatch, kind):
    suffix = "/user/register" if kind == "registration" else "/orders"
    response = SimpleNamespace(url=f"http://example.test/api{suffix}", request=SimpleNamespace(method="POST"),
                               ok=True, json=lambda: {"code": 100600, "data": None})
    page = _page_for(response)
    registry = MagicMock()
    monkeypatch.setattr(login_page, "expect", lambda _: MagicMock())
    monkeypatch.setattr(checkout_panel, "expect", lambda _: MagicMock())
    with pytest.raises(AssertionError, match="failed"):
        if kind == "registration":
            login_page.LoginPage(page).register({"username": "run-owned", "password": "temporary", "phone": "19000000000"}, registry)
        else:
            checkout_panel.CheckoutPanel(page).submit(registry)
    registry.add_user.assert_not_called()
    registry.add_order.assert_not_called()


@pytest.mark.parametrize(("action", "suffix", "method"), [
    ("pay", "/payment/mock-pay", "POST"), ("refund", "/payment/refund", "POST"),
    ("cancel", "/orders/72/cancel", "PUT"),
])
def test_order_mutations_use_real_customer_endpoints(action, suffix, method):
    response = SimpleNamespace(url=f"http://example.test/api{suffix}", request=SimpleNamespace(method=method),
                               ok=True, json=lambda: {"code": 0, "data": None})
    page = _page_for(response)
    panel = order_panel.OrderPanel(page)
    getattr(panel, action)(72) if action == "cancel" else getattr(panel, action)()


def test_customer_pages_use_real_routes_and_existing_test_ids():
    app = (FRONTEND / "App.tsx").read_text(encoding="utf-8")
    assert 'path="/shop/login"' in app
    assert 'path="/shop/tickets/:ticketId"' in app
    frontend = "\n".join((FRONTEND / "pages/shop" / name).read_text(encoding="utf-8")
                         for name in ("ShopPage.tsx", "ShopLoginPage.tsx"))
    for path in (ROOT / "pages/customer").glob("*.py"):
        tree = ast.parse(path.read_text(encoding="utf-8"))
        for node in ast.walk(tree):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute) and node.func.attr == "get_by_test_id":
                test_id = ast.literal_eval(node.args[0])
                assert test_id in frontend, f"missing source contract: {test_id}"


def test_customer_ui_has_no_fixed_sleeps_or_css_locator_paths():
    paths = list((ROOT / "pages/customer").glob("*.py")) + list((ROOT / "tests/ui/customer").glob("*.py"))
    for path in paths:
        tree = ast.parse(path.read_text(encoding="utf-8"))
        for node in ast.walk(tree):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                assert node.func.attr not in {"sleep", "wait_for_timeout", "locator", "query_selector"}, path.name


def test_customer_commerce_modules_preserve_both_write_gates():
    for name in ("test_customer_checkout.py", "test_customer_orders.py"):
        source = (ROOT / "tests/ui/customer" / name).read_text(encoding="utf-8")
        assert "pytest.mark.write" in source
        assert "pytest.mark.destructive_commerce" in source
    fixtures = (ROOT / "tests/ui/customer/conftest.py").read_text(encoding="utf-8")
    assert "authenticated_customer_context" in fixtures
    assert "add_init_script" not in fixtures
    assert "localStorage" not in fixtures


def test_customer_header_does_not_assume_a_banner_landmark(monkeypatch):
    # A <header> nested under <main> has no implicit ARIA banner role.
    frontend = (FRONTEND / "pages/shop/ShopPage.tsx").read_text(encoding="utf-8")
    assert frontend.index('<main className=') < frontend.index("<Header")
    page = MagicMock(viewport_size={"width": 1440, "height": 1000})
    def role(role_name, **kwargs):
        assert role_name != "banner", "nested header is not a banner landmark"
        return MagicMock()
    page.get_by_role.side_effect = role
    monkeypatch.setattr(shop_page, "expect", lambda _: MagicMock())
    shop = shop_page.ShopPage(page)
    shop.expect_anonymous()
    shop.expect_authenticated("run-owned-name")
    shop.open_orders("run-owned-name")


def test_catalog_cards_match_exact_title_not_a_substring(monkeypatch):
    page = MagicMock()
    monkeypatch.setattr(shop_page, "expect", lambda _: MagicMock())
    shop = shop_page.ShopPage(page)
    shop.expect_ticket("Music")
    shop.open_ticket(81, "Music")
    page.get_by_text.assert_called_with("Music", exact=True)
    for call in shop.grid.get_by_role.return_value.filter.call_args_list:
        assert "has" in call.kwargs and "has_text" not in call.kwargs


def test_direct_checkout_does_not_prepare_unrelated_cart_state():
    source = (ROOT / "tests/ui/customer/test_customer_checkout.py").read_text(encoding="utf-8")
    assert "add_cart_item" not in source
    assert "list_cart_items" not in source


def _customer_fixtures():
    import importlib.util
    spec = importlib.util.spec_from_file_location("customer_ui_fixtures", ROOT / "tests/ui/customer/conftest.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_catalog_fixture_queries_same_page_and_sort_as_browser():
    fixtures = _customer_fixtures()
    api = MagicMock()
    api.get.return_value = {"list": [{"id": 51, "title": "Unique show"}]}
    assert fixtures.catalog_ticket.__wrapped__(api)["id"] == 51
    api.get.assert_called_once_with("/tickets", params={"pageNum": 1, "pageSize": 24,
                                                      "sortBy": "date", "sortOrder": "asc"})


def test_category_fixture_selects_the_ui_sorted_page():
    fixtures = _customer_fixtures()
    booking = MagicMock()
    booking.list_categories.return_value = [{"id": 2, "name": "话剧"}]
    api = MagicMock()
    api.get.return_value = {"list": [{"id": 71, "title": "Current category show"}]}
    category, display, ticket = fixtures.catalog_category.__wrapped__(booking, api)
    assert (category["id"], display, ticket["id"]) == (2, "戏剧", 71)
    api.get.assert_called_once_with("/tickets", params={"pageNum": 1, "pageSize": 24,
                                                      "sortBy": "date", "sortOrder": "asc", "categoryId": 2})
