"""Offline composition checks; these do not claim browser or backend acceptance."""

import ast
import importlib
import importlib.util
import json
import subprocess
import sys
from decimal import Decimal
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import MagicMock

import pytest

from framework.api.client import ApiBusinessError
from framework.config import pytest_plugin


ROOT = Path(__file__).resolve().parents[2]
RUN_ID = "a1b2c3d4e5f6"
MODULES = ("test_purchase_lifecycle", "test_coupon_purchase", "test_admin_customer_visibility")


def workflow(name):
    return importlib.import_module(f"tests.e2e.{name}")


def fixtures():
    return workflow("conftest")


def order_snapshot(**changes):
    # Match OrderDetailResponse, including userId (not only test-consumed fields).
    return {"id": 72, "orderNo": "ORD72", "userId": 71, "ticketId": 81, "sessionId": 82,
            "orderStatus": 1, "payStatus": 0, "totalAmount": 99, "discountAmount": 0,
            "payAmount": 99, "items": [{"priceLevelId": 83, "quantity": 1}], **changes}


def journey_inputs(module, monkeypatch):
    # Isolate browser I/O, while executing the actual E2E test and its assertions.
    checkout = MagicMock()
    checkout.submit.return_value = {"orderId": 72, "orderNo": "ORD72"}
    monkeypatch.setattr(module, "CheckoutPanel", lambda page: checkout)
    monkeypatch.setattr(module, "ShopPage", lambda page: MagicMock())
    data = {"ticket": {"id": 81, "name": "Owned selection"}, "session": {"id": 82},
            "level": {"id": 83, "price": 99}, "identity": {"id": 84},
            "payload": {"contactName": "TEACHING-ONLY", "contactPhone": "19000000000", "userRemark": "owned"}}
    return checkout, data, SimpleNamespace(user_id=71, token="synthetic-token")


@pytest.mark.parametrize("wrong_state", [False, True])
def test_purchase_composition_checks_paid_and_refund_persistence(monkeypatch, wrong_state):
    module = workflow("test_purchase_lifecycle")
    checkout, data, customer = journey_inputs(module, monkeypatch)
    panel = MagicMock()
    monkeypatch.setattr(module, "OrderPanel", lambda page: panel)
    api, evidence = MagicMock(), MagicMock()
    api.get_order.side_effect = [order_snapshot(), order_snapshot(orderStatus=2, payStatus=1),
                                 order_snapshot(orderStatus=2 if wrong_state else 6, payStatus=1)]
    api.get_payment_status.return_value = {"status": "SUCCESS"}
    api.get_refund_status.return_value = {"status": "PROCESSING"}
    def execute():
        module.test_purchase_payment_and_refund_request(
            object(), customer, data, "display name", api, MagicMock(), evidence)
    if wrong_state:
        with pytest.raises(AssertionError):
            execute()
    else:
        execute()
        assert [call.kwargs["orderStatus"] for call in evidence.call_args_list] == [1, 2, 6]
        assert all(call.kwargs["orderId"] == 72 for call in evidence.call_args_list)
        panel.pay.assert_called_once_with()
        panel.refund.assert_called_once_with()
        assert api.get_order.call_count == 3
    checkout.select_inventory.assert_called_once_with(data["session"], data["level"])


@pytest.mark.parametrize("wrong_discount", [False, True])
def test_coupon_composition_checks_template_ownership_and_exact_money(monkeypatch, wrong_discount):
    module = workflow("test_coupon_purchase")
    checkout, data, customer = journey_inputs(module, monkeypatch)
    coupon = {"id": 91, "couponId": 92, "couponName": "owned coupon", "couponValue": 5}
    api, evidence = MagicMock(), MagicMock()
    api.get_order.return_value = order_snapshot(couponId=92, userCouponId=91,
                                                discountAmount=4 if wrong_discount else 5, payAmount=94)
    def execute():
        module.test_owned_coupon_is_applied_to_browser_purchase(
            object(), customer, data, coupon, api, MagicMock(), evidence)
    if wrong_discount:
        with pytest.raises(AssertionError):
            execute()
    else:
        execute()
        assert evidence.call_args.kwargs["orderId"] == 72
        assert evidence.call_args.kwargs["userId"] == 71
    checkout.apply_coupon.assert_called_once_with(91, "owned coupon")
    checkout.expect_coupon_discount.assert_called_once_with(Decimal("5"))


@pytest.mark.parametrize("denial", [100102, 100100])
def test_admin_visibility_requires_ui_mutation_and_specific_login_denial(monkeypatch, denial):
    module = workflow("test_admin_customer_visibility")
    users, api = MagicMock(), MagicMock()
    monkeypatch.setattr(module, "UserPage", lambda page: users)
    api.get_admin_user.side_effect = [{"username": "owned", "status": 1}, {"status": 0}]
    api.login_user.side_effect = ApiBusinessError(denial, "not attached")
    credentials = {"user_id": 71, "credentials": {"username": "owned", "password": "synthetic-password"}}
    def execute():
        module.test_admin_disabling_customer_prevents_customer_login(object(), credentials, api, "admin-token", MagicMock())
    if denial != 100102:
        with pytest.raises(AssertionError):
            execute()
    else:
        execute()
    users.show_detail.assert_called_once_with("owned")
    users.disable.assert_called_once_with(71, "owned")
    api.update_admin_user_status.assert_not_called()


def test_allure_evidence_drops_secrets_pii_and_untrusted_strings(monkeypatch):
    module = fixtures()
    attach = MagicMock()
    monkeypatch.setattr(module.allure, "attach", attach)
    module.attach_evidence("safe step", RUN_ID, token="secret", password="secret", headers={"Authorization": "secret"},
                           contactPhone="19000000000", userId=71, orderId=72, orderNo="ORD72",
                           orderStatus=2, refundStatus="PROCESSING", ticketId="secret", payStatus=True)
    assert json.loads(attach.call_args.args[0]) == {
        "runId": RUN_ID, "userId": 71, "orderId": 72, "orderNo": "ORD72",
        "orderStatus": 2, "refundStatus": "PROCESSING"}


def test_cleanup_response_is_aggregated_and_rejections_fail(monkeypatch):
    module = fixtures()
    attach = MagicMock()
    monkeypatch.setattr(module.allure, "attach", attach)
    report = module.cleanup_reporter.__wrapped__(RUN_ID)
    result = {"runId": "untrusted", "deleted": {"users": 1, "untrusted-key": 2},
              "notFound": {"orders": 1}, "rejected": {}}
    report(result)
    assert json.loads(attach.call_args.args[0]) == {"runId": RUN_ID, "deleted": 3, "notFound": 1, "rejected": 0}
    with pytest.raises(AssertionError, match="rejected"):
        report({**result, "rejected": {"orders": 1}})


@pytest.mark.parametrize("bad_counts", [None, [], {"orders": "1"}, {"orders": -1}, {"orders": True}])
def test_cleanup_reporter_rejects_malformed_counts(bad_counts, monkeypatch):
    module = fixtures()
    monkeypatch.setattr(module.allure, "attach", MagicMock())
    with pytest.raises(AssertionError, match="cleanup response"):
        module.cleanup_reporter.__wrapped__(RUN_ID)({"deleted": {}, "notFound": {}, "rejected": bad_counts})


def test_e2e_reuses_auth_data_and_registers_before_readback():
    module = fixtures()
    from tests.ui.customer import conftest as customer
    from tests.ui.admin import conftest as admin
    assert module.logged_in_customer_page is customer.logged_in_customer_page
    assert module.logged_in_admin_page is admin.logged_in_admin_page
    assert module.customer_order_data is customer.customer_order_data
    assert module.admin_ui_coupon is admin.admin_ui_coupon
    for path, name, create, register in (
        ("tests/ui/customer/conftest.py", "ui_login_credentials", "register_user", "add_user"),
        ("tests/ui/admin/conftest.py", "admin_ui_coupon", "create_admin_coupon", "add_coupon"),
    ):
        tree = ast.parse((ROOT / path).read_text(encoding="utf-8"))
        function = next(n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == name)
        statements = [ast.unparse(n) for n in function.body]
        index = next(i for i, line in enumerate(statements) if f".{create}(" in line)
        assert f".{register}(" in statements[index + 1]


def test_cleanup_report_runs_after_user_teardown_even_when_test_fails(monkeypatch):
    spec = importlib.util.spec_from_file_location("e2e_root_fixture_contract", ROOT / "conftest.py")
    shared = importlib.util.module_from_spec(spec)
    monkeypatch.setitem(sys.modules, spec.name, shared)
    spec.loader.exec_module(shared)
    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-key")
    api, factory, evidence = MagicMock(), MagicMock(), MagicMock()
    factory.user.return_value = {"username": "owned", "password": "synthetic-password"}
    api.register_user.return_value = 71
    api.login_user.return_value = {"userId": 71, "token": "synthetic-token", "expiresIn": 60}
    api.get_user.side_effect = ApiBusinessError(100100, "user deleted")
    result = {"deleted": {"coupons": 1}, "notFound": {"users": 1, "orders": 1}, "rejected": {}}
    api.cleanup.return_value = result
    events = []
    api.delete_test_user.side_effect = lambda *args: events.append("delete-user")
    api.cleanup.side_effect = lambda *args, **kwargs: events.append("batch-cleanup") or result
    def report(value):
        events.append("report")
        evidence(value)
    batch = shared.cleanup_registry.__wrapped__(RUN_ID, api, report)
    registry = next(batch)
    customer = shared.temporary_customer.__wrapped__(RUN_ID, api, registry, factory)
    next(customer)
    registry.add_order(72)
    registry.add_coupon(92)
    with pytest.raises(AssertionError, match="business assertion"):
        customer.throw(AssertionError("business assertion failed"))
    batch.close()
    assert events == ["delete-user", "batch-cleanup", "report"]
    api.cleanup.assert_called_once_with(RUN_ID, user_ids=[71], order_ids=[72], ticket_ids=[], coupon_ids=[92],
                                        course_key="synthetic-key")
    evidence.assert_called_once_with(result)


def test_issued_coupon_is_selected_by_owned_template_and_attached_without_credentials():
    module = fixtures()
    api, evidence = MagicMock(), MagicMock()
    owned = {"id": 91, "couponId": 92, "status": 0}
    api.list_my_coupons.return_value = {"list": [{"id": 93, "couponId": 94, "status": 0}, owned]}
    result = module.e2e_issued_coupon.__wrapped__(
        {"id": 92}, SimpleNamespace(user_id=71, token="synthetic-token"), api, "synthetic-admin", evidence)
    assert result is owned
    api.issue_admin_coupon.assert_called_once_with("synthetic-admin", 92, [71], issue_mode="course-test")
    evidence.assert_called_once_with("run-owned coupon issued", userId=71, couponId=92, userCouponId=91)


@pytest.mark.parametrize("name", MODULES)
def test_e2e_marks_require_both_write_gates(monkeypatch, name):
    module = workflow(name)
    markers = {mark.name: mark.mark for mark in module.pytestmark}
    assert {"e2e", "chromium", "write", "destructive_commerce", "xdist_group"} <= markers.keys()
    assert markers["xdist_group"].args == ("booking_e2e_commerce",)
    item = SimpleNamespace(get_closest_marker=markers.get)
    config = SimpleNamespace(option=SimpleNamespace(collectonly=False),
                             getoption=lambda name: name == "--allow-write")
    monkeypatch.setenv("COURSE_TEST_KEY", "synthetic-key")
    with pytest.raises(pytest.UsageError, match="--allow-destructive-commerce"):
        pytest_plugin.pytest_collection_modifyitems(config, [item])


@pytest.mark.parametrize(("workers", "dist", "browsers", "collect", "error"), [
    (0, "no", [], False, None), (0, "no", ["chromium"], False, None),
    (2, "loadgroup", ["chromium"], False, None), (2, "load", ["chromium"], False, "serially"),
    (0, "no", ["firefox"], False, "chromium"), (0, "no", ["chromium", "webkit"], False, "chromium"),
    (2, "load", ["webkit"], True, None),
])
def test_e2e_serial_and_browser_gate(workers, dist, browsers, collect, error):
    options = {"numprocesses": workers, "dist": dist, "browser": browsers}
    config = SimpleNamespace(option=SimpleNamespace(collectonly=collect),
                             getoption=lambda name, default=None: options.get(name, default))
    item = SimpleNamespace(iter_markers=lambda name: [SimpleNamespace(args=("booking_e2e_commerce",))])
    if error:
        with pytest.raises(pytest.UsageError, match=error):
            fixtures().pytest_collection_modifyitems(config, [item])
    else:
        fixtures().pytest_collection_modifyitems(config, [item])


def test_e2e_has_no_browser_selectors_sleeps_or_hidden_skips():
    paths = list((ROOT / "tests/e2e").glob("*.py"))
    for path in paths:
        for node in ast.walk(ast.parse(path.read_text(encoding="utf-8"))):
            if isinstance(node, (ast.While, ast.AsyncFor)):
                pytest.fail(f"unexpected retry loop: {path.name}")
            if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute):
                assert node.func.attr not in {"sleep", "wait_for_timeout", "locator", "query_selector", "skip", "xfail"}, path.name
                assert not node.func.attr.startswith("get_by_"), path.name


def test_coupon_discount_wait_matches_real_visible_text(monkeypatch):
    from pages.customer import checkout_panel
    page, assertion = MagicMock(), MagicMock()
    monkeypatch.setattr(checkout_panel, "expect", lambda locator: assertion)
    checkout_panel.CheckoutPanel(page).expect_coupon_discount(Decimal("5"))
    page.get_by_role.return_value.get_by_text.assert_called_once_with("-¥5.00", exact=True)
    assertion.to_be_visible.assert_called_once_with()
    source = (ROOT.parent / "frontend/src/pages/shop/ShopPage.tsx").read_text(encoding="utf-8")
    assert '`-${currency(validation?.discountAmount ?? 0)}`' in source


def test_e2e_fixture_package_does_not_shadow_root_conftest_during_collection():
    # Collection order reproduces the prior import error in test_customer_session:
    # E2E conftest loads first, followed by a framework module importing root fixtures.
    result = subprocess.run(
        [sys.executable, "-m", "pytest", "tests/e2e", "tests/framework/test_customer_session.py",
         "--collect-only", "--env=local", "-q", "-o", "addopts="],
        cwd=ROOT, capture_output=True, text=True, timeout=30,
    )
    assert result.returncode == 0, result.stdout + result.stderr
    assert "13 tests collected" in result.stdout
