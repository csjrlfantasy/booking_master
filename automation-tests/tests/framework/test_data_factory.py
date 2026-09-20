import pytest

from framework.data.factory import TestDataFactory


def test_factory_prefixes_every_created_user():
    user = TestDataFactory("a1b2c3d4").user()

    assert user["username"].startswith("pw-course-a1b2c3d4-")
    assert user["nickname"].startswith("pw-course-a1b2c3d4-")
    assert len(user["phone"]) == 11
    assert user["phone"].isdigit()


def test_factory_user_values_are_unique_and_have_required_defaults():
    factory = TestDataFactory("a1b2c3d4")

    first = factory.user()
    second = factory.user()

    assert first["username"] != second["username"]
    assert first["nickname"] != second["nickname"]
    assert first["phone"] != second["phone"]
    assert first["phone"].startswith("19")
    assert first["password"]
    assert first["source"] == "h5"


@pytest.mark.parametrize("run_id", ["short", "UPPERCASE", "has-hyphen", "a" * 33])
def test_factory_rejects_invalid_run_ids(run_id):
    with pytest.raises(ValueError, match="runId"):
        TestDataFactory(run_id)
