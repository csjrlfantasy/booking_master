"""Statistics assert internal relationships, avoiding volatile cross-request totals."""

import pytest


pytestmark = [pytest.mark.api, pytest.mark.regression]


def test_admin_overview_has_nonnegative_counts_and_amounts(booking_api, admin_token):
    data = booking_api.get_admin_overview_stats(admin_token)
    assert data["todaySales"] >= 0
    assert data["pendingOrderCount"] >= 0


def test_admin_order_distribution_matches_total(booking_api, admin_token):
    data = booking_api.get_admin_order_stats(admin_token)
    distribution = {item["status"]: item["count"] for item in data["orderStatusDistribution"]}
    assert len(distribution) == len(data["orderStatusDistribution"])
    assert sum(distribution.values()) == data["totalOrders"]
    for status, field in ((1, "pendingPayOrders"), (2, "paidOrders"), (3, "completedOrders"), (4, "cancelledOrders"), (6, "refundingOrders")):
        assert data[field] == distribution.get(status, 0)


def test_admin_user_statistics_are_consistent(booking_api, admin_token):
    data = booking_api.get_admin_user_stats(admin_token)
    assert 0 <= data["activeUsers"] <= data["totalUsers"]
    assert 0 <= data["vipUsers"] <= data["totalUsers"]
    assert data["newUsersToday"] <= data["newUsersThisWeek"]
    assert data["newUsersToday"] <= data["newUsersThisMonth"]
    trend = data["userGrowthTrend"]
    assert len(trend) == 7
    assert [item["date"] for item in trend] == sorted({item["date"] for item in trend})
    assert trend[-1]["count"] == data["newUsersToday"]
