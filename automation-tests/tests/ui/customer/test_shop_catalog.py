"""Environment-derived catalog smoke on desktop and narrow mobile viewports."""

import pytest

from pages.customer.shop_page import ShopPage


pytestmark = [pytest.mark.ui, pytest.mark.regression, pytest.mark.public]


@pytest.mark.smoke
@pytest.mark.cross_browser
@pytest.mark.parametrize("customer_ui_page", [{"width": 1440, "height": 1000}, {"width": 390, "height": 844}], indirect=True, ids=["desktop", "mobile"])
def test_catalog_search_opens_real_ticket_detail(customer_ui_page, catalog_ticket):
    shop = ShopPage(customer_ui_page)
    shop.open()
    if customer_ui_page.viewport_size["width"] < 768:
        shop.expect_mobile_navigation()
    else:
        shop.expect_anonymous()
    shop.search(catalog_ticket["title"])
    shop.expect_ticket(catalog_ticket["title"])
    shop.open_ticket(catalog_ticket["id"], catalog_ticket["title"])


@pytest.mark.cross_browser
def test_catalog_search_has_explicit_empty_state(customer_ui_page, run_id):
    shop = ShopPage(customer_ui_page)
    shop.open()
    shop.search(f"no-such-ticket-{run_id}")
    shop.expect_empty()


@pytest.mark.chromium
def test_category_filter_displays_matching_catalog(customer_ui_page, catalog_category):
    category, display, ticket = catalog_category
    shop = ShopPage(customer_ui_page)
    shop.open()
    shop.select_category(category["id"], display)
    shop.expect_ticket(ticket["title"])


@pytest.mark.write
@pytest.mark.chromium
def test_api_token_injection_displays_authenticated_header(logged_in_customer_page, customer_display_name):
    shop = ShopPage(logged_in_customer_page)
    shop.open()
    shop.expect_authenticated(customer_display_name)
