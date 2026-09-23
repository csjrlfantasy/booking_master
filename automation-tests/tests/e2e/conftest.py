"""Compose existing execution-only fixtures and redact all E2E evidence."""

import json

import allure
import pytest

# Explicit fixture re-exports keep auth serialization and payloads in one place.
from tests.ui.customer.conftest import (
    logged_in_customer_page, customer_saleable_inventory, customer_order_data,
    customer_display_name, ui_login_credentials,
)
from tests.ui.admin.conftest import (
    admin_ui_login, logged_in_admin_page, admin_ui_coupon,
)


GROUP = "booking_e2e_commerce"
ID_FIELDS = {"userId", "orderId", "ticketId", "sessionId", "priceLevelId", "couponId", "userCouponId"}
STATE_FIELDS = {"orderStatus", "payStatus", "status", "discountAmount", "payAmount", "totalAmount"}


def attach_evidence(name, run_id, **values):
    """Allowlist only business IDs and numeric states; omit all raw payloads."""
    safe = {"runId": run_id}
    for key in ID_FIELDS | STATE_FIELDS:
        if key in values and type(values[key]) in (int, float):
            safe[key] = values[key]
    order_no = values.get("orderNo")
    if isinstance(order_no, str) and order_no.isalnum() and len(order_no) <= 64:
        safe["orderNo"] = order_no
    if values.get("refundStatus") in {"NONE", "PROCESSING", "SUCCESS"}:
        safe["refundStatus"] = values["refundStatus"]
    allure.attach(json.dumps(safe, ensure_ascii=False), name=name, attachment_type=allure.attachment_type.JSON)


@pytest.fixture
def e2e_evidence(run_id):
    return lambda name, **values: attach_evidence(name, run_id, **values)


@pytest.fixture
def cleanup_reporter(run_id):
    def report(result):
        # Keys from a remote response are not trusted. Aggregate only numeric counts.
        assert isinstance(result, dict), "invalid cleanup response"
        safe = {"runId": run_id}
        for category in ("deleted", "notFound", "rejected"):
            counts = result.get(category)
            assert isinstance(counts, dict), "invalid cleanup response counts"
            assert all(type(v) is int and v >= 0 for v in counts.values()), "invalid cleanup response counts"
            safe[category] = sum(counts.values())
        allure.attach(json.dumps(safe), name="cleanup response (redacted counts)",
                      attachment_type=allure.attachment_type.JSON)
        assert safe["rejected"] == 0, "cleanup rejected run-owned resources"
    return report


def pytest_collection_modifyitems(config, items):
    selected = [item for item in items if any(
        marker.args == (GROUP,) for marker in item.iter_markers("xdist_group"))]
    if not selected or config.option.collectonly:
        return
    if config.getoption("numprocesses", default=0) and config.getoption("dist", default="no") != "loadgroup":
        raise pytest.UsageError("E2E commerce must run serially: use -n 0 or --dist=loadgroup")
    if any(name != "chromium" for name in config.getoption("browser", default=[])):
        raise pytest.UsageError("E2E commerce requires --browser chromium")


@pytest.fixture
def e2e_customer_credentials(ui_login_credentials, booking_api, e2e_evidence):
    # Registration and immediate cleanup registration belong to the reused fixture.
    login = booking_api.login_user(ui_login_credentials["username"], ui_login_credentials["password"])
    e2e_evidence("customer created and authenticated", userId=login["userId"])
    return {"user_id": login["userId"], "credentials": ui_login_credentials}


@pytest.fixture
def e2e_issued_coupon(admin_ui_coupon, temporary_customer, booking_api, admin_token, e2e_evidence):
    booking_api.issue_admin_coupon(admin_token, admin_ui_coupon["id"],
                                  [temporary_customer.user_id], issue_mode="course-test")
    coupons = booking_api.list_my_coupons(temporary_customer.token, page_size=100)
    owned = [item for item in coupons["list"] if item["couponId"] == admin_ui_coupon["id"]]
    assert len(owned) == 1, "run-owned coupon must be visible to its recipient"
    assert owned[0]["status"] == 0
    e2e_evidence("run-owned coupon issued", userId=temporary_customer.user_id,
                 couponId=admin_ui_coupon["id"], userCouponId=owned[0]["id"])
    return owned[0]
