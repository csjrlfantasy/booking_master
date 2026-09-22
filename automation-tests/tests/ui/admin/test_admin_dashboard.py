"""Meaningful widget checks use the same snapshot rendered by the frontend."""

import pytest
from pages.admin.dashboard_page import DashboardPage

pytestmark = [pytest.mark.ui, pytest.mark.regression]


@pytest.mark.smoke
def test_admin_dashboard_renders_api_snapshot(logged_in_admin_page):
    dashboard = DashboardPage(logged_in_admin_page)
    snapshot = dashboard.open()
    dashboard.expect_snapshot(snapshot)
