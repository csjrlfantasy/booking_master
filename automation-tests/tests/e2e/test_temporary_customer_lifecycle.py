import pytest


@pytest.mark.e2e
@pytest.mark.api
@pytest.mark.write
@pytest.mark.public
@pytest.mark.smoke
def test_temporary_customer_is_authenticated(temporary_customer, booking_api):
    profile = booking_api.get_user(
        temporary_customer.user_id, token=temporary_customer.token
    )
    assert profile["id"] == temporary_customer.user_id
    assert profile["username"] == temporary_customer.username
