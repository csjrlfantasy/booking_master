"""Offline safeguards for admin auth, ownership, locators and real UI contracts."""

import ast
import importlib.util
import json
from contextlib import contextmanager
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest


ROOT = Path(__file__).resolve().parents[2]
FRONTEND = ROOT.parent / "frontend/src"


def fixtures():
    path = ROOT / "tests/ui/admin/conftest.py"
    assert path.exists(), "admin UI fixtures must be implemented"
    spec = importlib.util.spec_from_file_location("admin_ui_fixtures", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_admin_persistence_matches_zustand_and_login_user_shape():
    state = fixtures().admin_storage_state("http://example.test/admin", {
        "token": "synthetic-token", "userId": 71, "username": "synthetic-admin",
        "nickname": "Teacher", "expiresIn": 60,
    }, now_ms=1000)
    assert state["cookies"] == []
    origin = state["origins"][0]
    assert origin["origin"] == "http://example.test"
    entry = origin["localStorage"][0]
    assert entry["name"] == "auth-storage"
    persisted = json.loads(entry["value"])
    assert persisted == {"version": 0, "state": {
        "isAuthenticated": True, "token": "synthetic-token", "expiresAt": 61000,
        "user": {"id": 71, "username": "synthetic-admin", "nickname": "Teacher", "phone": "",
                 "userType": 0, "userTypeText": "管理员", "status": 1, "statusText": "正常", "createdAt": ""},
    }}
    store = (FRONTEND / "store/authStore.ts").read_text(encoding="utf-8")
    assert "name: 'auth-storage'" in store
    for key in persisted["state"]:
        assert f"{key}: state.{key}" in store


@pytest.mark.parametrize("field", ["BOOKING_ADMIN_USERNAME", "BOOKING_ADMIN_PASSWORD"])
def test_missing_admin_execution_credentials_fail_without_network(monkeypatch, field):
    monkeypatch.setenv("BOOKING_ADMIN_USERNAME", "synthetic-admin")
    monkeypatch.setenv("BOOKING_ADMIN_PASSWORD", "synthetic-password")
    monkeypatch.delenv(field)
    api = MagicMock()
    with pytest.raises(RuntimeError, match=field):
        fixtures().admin_ui_login.__wrapped__(api)
    api.login_admin.assert_not_called()


def test_authenticated_context_closes_on_test_failure():
    module = fixtures()
    browser = MagicMock()
    settings = SimpleNamespace(web_url="http://example.test", timeout_seconds=10)
    login = {"token": "synthetic", "userId": 1, "username": "a", "nickname": "n", "expiresIn": 60}
    lifecycle = module.logged_in_admin_page.__wrapped__(browser, settings, login)
    next(lifecycle)
    with pytest.raises(RuntimeError):
        lifecycle.throw(RuntimeError("test failed"))
    browser.new_context.return_value.close.assert_called_once()


@pytest.mark.parametrize("kind", ["user", "coupon"])
@pytest.mark.parametrize("business_code", [0, 100600])
def test_ui_creations_register_id_even_if_postresponse_assertion_fails(monkeypatch, kind, business_code):
    from pages.admin import coupon_page, user_page
    module = user_page if kind == "user" else coupon_page
    monkeypatch.setattr(module, "expect", lambda _: MagicMock())
    page = MagicMock()
    response = SimpleNamespace(url=f"http://example.test/api/admin/{kind}s", request=SimpleNamespace(method="POST"),
                               ok=True, json=lambda: {"code": business_code, "data": 71})
    @contextmanager
    def capture(predicate):
        assert predicate(response)
        assert not predicate(SimpleNamespace(url=response.url, request=SimpleNamespace(method="GET")))
        yield SimpleNamespace(value=response)
    page.expect_response.side_effect = capture
    registry = MagicMock()
    def create():
        if kind == "user":
            return module.UserPage(page).create({"username": "owned", "password": "temporary", "phone": "19000000000"}, registry)
        return module.CouponPage(page).create("owned", registry)
    if business_code:
        with pytest.raises(RuntimeError, match="creation failed"):
            create()
    else:
        assert create() == 71
    getattr(registry, f"add_{kind}").assert_called_once_with(71)


def test_admin_pages_have_real_routes_and_existing_literal_test_ids():
    paths = list((ROOT / "pages/admin").glob("*.py"))
    assert len(paths) == 7
    source = "\n".join(p.read_text(encoding="utf-8") for p in (FRONTEND / "pages").rglob("*.tsx"))
    app = (FRONTEND / "App.tsx").read_text(encoding="utf-8")
    for route in ("/login", "users", "users/new", "users/:id", "users/:id/edit", "tickets", "tickets/:id", "orders", "orders/:id", "coupons"):
        assert f'path="{route}"' in app
    for path in paths:
        for node in ast.walk(ast.parse(path.read_text(encoding="utf-8"))):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute) and node.func.attr == "get_by_test_id" and isinstance(node.args[0], ast.Constant):
                assert node.args[0].value in source, f"missing frontend contract: {node.args[0].value}"


def test_admin_ui_no_fixed_sleeps_css_paths_or_runtime_skips():
    paths = list((ROOT / "pages/admin").glob("*.py")) + list((ROOT / "tests/ui/admin").glob("*.py"))
    assert paths
    for path in paths:
        for node in ast.walk(ast.parse(path.read_text(encoding="utf-8"))):
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                assert node.func.attr not in {"sleep", "wait_for_timeout", "locator", "query_selector", "skip"}, path.name


def test_order_and_coupon_issue_keep_commerce_gates_and_noop_is_strict():
    orders = (ROOT / "tests/ui/admin/test_admin_orders.py").read_text(encoding="utf-8")
    coupons = ast.parse((ROOT / "tests/ui/admin/test_admin_coupons.py").read_text(encoding="utf-8"))
    assert "pytest.mark.write" in orders and "pytest.mark.destructive_commerce" in orders
    issue = next(n for n in coupons.body if isinstance(n, ast.FunctionDef) and "issue" in n.name)
    decorators = [ast.unparse(d) for d in issue.decorator_list]
    assert "pytest.mark.write" in decorators and "pytest.mark.destructive_commerce" in decorators
    users = (ROOT / "tests/ui/admin/test_admin_users.py").read_text(encoding="utf-8")
    assert "strict=True" in users and "raises=UserEditNotPersisted" in users
    assert "get_admin_user" in users and "UI-TODO-001" in users
    frontend = (FRONTEND / "pages/user/UserEditPage.tsx").read_text(encoding="utf-8")
    assert "暂时不调用API" in frontend


def test_api_setup_registers_resources_before_readback():
    source = (ROOT / "tests/ui/admin/conftest.py").read_text(encoding="utf-8")
    tree = ast.parse(source)
    expected = {"admin_ui_customer": ("create_admin_user", "add_user"),
                "admin_ui_ticket": ("create_admin_ticket", "add_ticket"),
                "admin_ui_coupon": ("create_admin_coupon", "add_coupon"),
                "admin_ui_order": ("create_order", "add_order")}
    for name, (create, register) in expected.items():
        function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == name)
        statements = [ast.unparse(n) for n in function.body]
        index = next(i for i, stmt in enumerate(statements) if f".{create}(" in stmt)
        assert f".{register}(" in statements[index + 1], f"{name} must immediately register"


@pytest.mark.parametrize(("module_name", "class_name", "method", "args", "suffix", "http_method"), [
    ("user_page", "UserPage", "disable", (71, "owned"), "/admin/users/71/status", "PUT"),
    ("ticket_page", "TicketPage", "take_off_sale", (71, "owned"), "/admin/tickets/71/status", "PUT"),
    ("order_page", "OrderPage", "cancel", (71, "OWNED"), "/admin/orders/71/cancel", "PUT"),
    ("order_page", "OrderPage", "refund", (71, "OWNED"), "/admin/orders/71/refund", "PUT"),
    ("coupon_page", "CouponPage", "change_value", (71, "owned", 7), "/admin/coupons/71", "PUT"),
    ("coupon_page", "CouponPage", "end", (71, "owned"), "/admin/coupons/71/status", "PUT"),
    ("coupon_page", "CouponPage", "issue", (71, "owned", 81), "/admin/coupons/71/issue", "POST"),
])
@pytest.mark.parametrize("accepted", [True, False])
def test_mutations_wait_for_exact_route_method_and_reject_business_failure(monkeypatch, module_name, class_name, method, args, suffix, http_method, accepted):
    import importlib
    module = importlib.import_module(f"pages.admin.{module_name}")
    monkeypatch.setattr(module, "expect", lambda _: MagicMock())
    page = MagicMock()
    response = SimpleNamespace(url=f"http://example.test/api{suffix}", request=SimpleNamespace(method=http_method),
                               ok=True, json=lambda: {"code": 0 if accepted else 100600})
    @contextmanager
    def capture(predicate):
        assert predicate(response)
        assert not predicate(SimpleNamespace(url=response.url, request=SimpleNamespace(method="GET")))
        assert not predicate(SimpleNamespace(url="http://example.test/api/admin/unrelated", request=response.request))
        yield SimpleNamespace(value=response)
    page.expect_response.side_effect = capture
    action = getattr(getattr(module, class_name)(page), method)
    if accepted:
        action(*args)
    else:
        with pytest.raises(RuntimeError, match="failed"):
            action(*args)


def test_dashboard_uses_exact_browser_response_instead_of_second_api_snapshot(monkeypatch):
    from pages.admin import dashboard_page
    page = MagicMock()
    check = MagicMock()
    monkeypatch.setattr(dashboard_page, "expect", lambda _: check)
    snapshot = {"todaySales": 12.5, "todayOrders": 1200, "todayUsers": 2, "todayRefunds": 0,
                "pendingRefundCount": 3, "pendingOrderCount": 7}
    response = SimpleNamespace(url="http://example.test/api/admin/stats/overview", request=SimpleNamespace(method="GET"),
                               ok=True, json=lambda: {"code": 0, "data": snapshot})
    @contextmanager
    def capture(predicate):
        assert predicate(response)
        yield SimpleNamespace(value=response)
    page.expect_response.side_effect = capture
    dashboard = dashboard_page.DashboardPage(page)
    assert dashboard.open() is snapshot
    dashboard.expect_snapshot(snapshot)
    calls = page.get_by_test_id.return_value.get_by_text.call_args_list
    assert [c.args[0] for c in calls] == ["¥12.50", "1,200", "2", "0"]
    frontend = (FRONTEND / "pages/dashboard/DashboardPage.tsx").read_text(encoding="utf-8")
    assert 'data-testid={`admin-stat-${title}`}' in frontend
    for field in snapshot:
        assert field in frontend
