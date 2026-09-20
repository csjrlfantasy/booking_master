import pytest

from framework.cleanup.registry import CleanupRegistry


class FakeBookingApi:
    def __init__(self):
        self.cleanup_payload = None

    def cleanup(self, run_id, *, user_ids, order_ids, ticket_ids, coupon_ids, course_key):
        self.cleanup_payload = {
            "runId": run_id,
            "userIds": list(user_ids),
            "orderIds": list(order_ids),
            "ticketIds": list(ticket_ids),
            "couponIds": list(coupon_ids),
            "courseKey": course_key,
        }
        return {"cleaned": True}


@pytest.fixture
def fake_booking_api():
    return FakeBookingApi()


def test_registry_sends_exact_registered_ids(fake_booking_api):
    injected_key = "synthetic-course-key"
    registry = CleanupRegistry("a1b2c3d4", fake_booking_api, injected_key)
    registry.add_user(7)
    registry.add_user(7)
    registry.cleanup()

    assert fake_booking_api.cleanup_payload["userIds"] == [7]
    assert fake_booking_api.cleanup_payload["courseKey"] == injected_key
    assert not hasattr(registry, "course_key")


def test_registry_keeps_insertion_order_and_deduplicates_each_resource_type(fake_booking_api):
    registry = CleanupRegistry("a1b2c3d4", fake_booking_api, "course-key")
    registry.add_user(2)
    registry.add_user(1)
    registry.add_user(2)
    registry.add_order(4)
    registry.add_order(4)
    registry.add_ticket(6)
    registry.add_ticket(5)
    registry.add_ticket(6)
    registry.add_coupon(8)
    registry.add_coupon(8)

    assert registry.cleanup() == {"cleaned": True}
    assert fake_booking_api.cleanup_payload == {
        "runId": "a1b2c3d4",
        "userIds": [2, 1],
        "orderIds": [4],
        "ticketIds": [6, 5],
        "couponIds": [8],
        "courseKey": "course-key",
    }


def test_registry_sends_all_empty_lists_when_nothing_is_registered(fake_booking_api):
    registry = CleanupRegistry("a1b2c3d4", fake_booking_api, "course-key")

    registry.cleanup()

    assert fake_booking_api.cleanup_payload == {
        "runId": "a1b2c3d4",
        "userIds": [],
        "orderIds": [],
        "ticketIds": [],
        "couponIds": [],
        "courseKey": "course-key",
    }


@pytest.mark.parametrize("resource_id", [0, -1, True, "4"])
def test_registry_rejects_invalid_resource_ids(fake_booking_api, resource_id):
    registry = CleanupRegistry("a1b2c3d4", fake_booking_api, "course-key")

    with pytest.raises(ValueError, match="positive"):
        registry.add_coupon(resource_id)


@pytest.mark.parametrize("run_id", ["invalid", "A1B2C3D4", "a" * 33])
def test_registry_rejects_invalid_run_ids(fake_booking_api, run_id):
    with pytest.raises(ValueError, match="runId"):
        CleanupRegistry(run_id, fake_booking_api, "course-key")


@pytest.mark.parametrize("course_key", ["", "   ", None, 7])
def test_registry_requires_a_non_empty_course_key(fake_booking_api, course_key):
    with pytest.raises(ValueError, match="course key"):
        CleanupRegistry("a1b2c3d4", fake_booking_api, course_key)


def test_registry_does_not_swallow_cleanup_errors():
    class FailingBookingApi:
        def cleanup(self, *args, **kwargs):
            raise RuntimeError("cleanup unavailable")

    registry = CleanupRegistry("a1b2c3d4", FailingBookingApi(), "course-key")

    with pytest.raises(RuntimeError, match="cleanup unavailable"):
        registry.cleanup()
