# Playwright Suite Migration and Coverage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the original API suite while replacing its active execution path with accurate API, customer UI, admin UI, and business E2E coverage.

**Architecture:** Tag the original suite, quarantine incompatible files outside pytest collection, and migrate by business capability into the new framework. Page Objects expose stable user actions, API tests assert contracts and side effects, and E2E tests combine API setup with UI verification and the cleanup registry.

**Tech Stack:** Python 3.11+, pytest, pytest-playwright, requests, Playwright Chromium/Firefox/WebKit, Allure.

---

## File structure

- Move old active files from `automation-tests/api-tests` to `automation-tests/legacy/api-tests-v1` after tagging.
- Create `automation-tests/docs/api-v1-migration-matrix.md`.
- Create API tests under `automation-tests/tests/api/{customer,admin}`.
- Create Page Objects under `automation-tests/pages/{customer,admin}`.
- Create UI tests under `automation-tests/tests/ui/{customer,admin}`.
- Create cross-layer scenarios under `automation-tests/tests/e2e`.

### Task 1: Freeze and quarantine the old API suite

**Files:**
- Move: `automation-tests/api-tests/**` to `automation-tests/legacy/api-tests-v1/**`
- Create: `automation-tests/docs/api-v1-migration-matrix.md`

- [ ] Verify no staged user changes exist with `git diff --cached --name-only`.
- [ ] Create annotated tag: `git tag -a api-tests-v1-baseline -m "Preserve original pytest requests API suite"`.
- [ ] Move the directory with `git mv automation-tests/api-tests automation-tests/legacy/api-tests-v1`.
- [ ] Add `automation-tests/legacy/api-tests-v1/pytest.ini` containing `testpaths = __disabled__` so root collection cannot execute legacy files.
- [ ] Write the migration matrix with one row for every legacy `test_*.py` file and exactly one status: `migrated`, `invalid-endpoint`, `business-changed`, or `pending`.
- [ ] Run `python -m pytest --collect-only -q` and verify no node id contains `legacy/api-tests-v1`.
- [ ] Commit with message `refactor: quarantine legacy API suite`.

### Task 2: Migrate customer authentication and user APIs

**Files:**
- Create: `automation-tests/tests/api/customer/test_auth_api.py`
- Create: `automation-tests/tests/api/customer/test_identity_api.py`
- Modify: `automation-tests/framework/api/booking.py`

- [ ] Add tests for successful registration, duplicate username, duplicate phone, valid login, invalid password, missing fields, authenticated profile, identity create/update/delete, and cross-user identity denial.
- [ ] Every success test uses `temporary_customer`; every independently registered user calls `cleanup_registry.add_user(user_id)` immediately after registration.
- [ ] Assert HTTP semantics plus Result envelope `code`, `message`, and typed `data`; do not accept a placeholder token.
- [ ] Run: `python -m pytest tests/api/customer/test_auth_api.py tests/api/customer/test_identity_api.py --env=public --allow-write -v`.
- [ ] Expected: all tests PASS and the cleanup audit reports zero residual users.
- [ ] Update the corresponding migration-matrix rows to `migrated` and commit as `test: migrate customer identity API coverage`.

### Task 3: Migrate catalog, cart, coupon, order, payment, and refund APIs

**Files:**
- Create: `automation-tests/tests/api/customer/test_catalog_api.py`
- Create: `automation-tests/tests/api/customer/test_cart_api.py`
- Create: `automation-tests/tests/api/customer/test_coupon_api.py`
- Create: `automation-tests/tests/api/customer/test_order_api.py`
- Create: `automation-tests/tests/api/customer/test_payment_api.py`
- Modify: `automation-tests/framework/api/booking.py`

- [ ] Catalog tests cover list, search, category filter, detail, nonexistent ID, sessions, price levels, hot, and upcoming responses.
- [ ] Cart tests cover add, list, count, quantity update, delete, batch delete, clear, unauthorized access, and invalid quantity.
- [ ] Coupon tests cover center/list, receive, duplicate receive, my coupons, validate, unavailable coupon, and unauthorized access.
- [ ] Order tests cover create, list, detail, invalid inventory, cancel, repeat cancel, other-user access denial, and status transition.
- [ ] Payment tests cover create, mock pay, status, duplicate pay, refund request, refund query, and invalid order ownership.
- [ ] Use a helper fixture that selects an actually saleable ticket/session/price level from the target environment; if none exists, fail precondition with the searched IDs rather than silently skip.
- [ ] Register every created order ID in `cleanup_registry` before the first assertion after creation.
- [ ] Run the six customer API files against local, then public with `--allow-write`; expect zero cleanup residue.
- [ ] Update the migration matrix and commit as `test: migrate customer commerce API coverage`.

### Task 4: Add administrator API coverage

**Files:**
- Create: `automation-tests/tests/api/admin/test_admin_auth_api.py`
- Create: `automation-tests/tests/api/admin/test_admin_user_api.py`
- Create: `automation-tests/tests/api/admin/test_admin_ticket_api.py`
- Create: `automation-tests/tests/api/admin/test_admin_order_api.py`
- Create: `automation-tests/tests/api/admin/test_admin_coupon_api.py`
- Create: `automation-tests/tests/api/admin/test_admin_stats_api.py`

- [ ] Build `admin_token` only from `BOOKING_ADMIN_USERNAME` and `BOOKING_ADMIN_PASSWORD`; mark all logs containing the request body as sensitive.
- [ ] Cover login success/failure, missing token, customer token on admin endpoint, user search/detail/create/status/roles, ticket list/detail/status, order list/detail/status/cancel/refund, coupon create/update/status/issue, and stats overview/orders/users.
- [ ] Prefix all created users, tickets, and coupons and register IDs before assertions.
- [ ] Run: `python -m pytest tests/api/admin --env=public --allow-write -v`.
- [ ] Expected: PASS and no resources in the cleanup response `rejected` map.
- [ ] Commit as `test: add admin API regression suite`.

### Task 5: Implement customer Page Objects and UI tests

**Files:**
- Create: `automation-tests/pages/customer/login_page.py`
- Create: `automation-tests/pages/customer/shop_page.py`
- Create: `automation-tests/pages/customer/checkout_panel.py`
- Create: `automation-tests/pages/customer/order_panel.py`
- Create: `automation-tests/tests/ui/customer/test_customer_login.py`
- Create: `automation-tests/tests/ui/customer/test_shop_catalog.py`
- Create: `automation-tests/tests/ui/customer/test_customer_checkout.py`
- Create: `automation-tests/tests/ui/customer/test_customer_orders.py`

- [ ] Page Objects use role/label/text locators first and existing `data-testid` for contract-critical elements; no CSS class selector may be exposed from a test.
- [ ] Login tests cover successful UI login, invalid password, required fields, registration tab, successful registration, and returnTo navigation. Registration tests record the returned user through API lookup before leaving setup.
- [ ] Catalog tests cover search, category, empty result, detail navigation, and authenticated/anonymous header state.
- [ ] Checkout tests use `authenticated_customer_context`, add a saleable item, create/select an identity, apply an eligible coupon when available, submit an order, and assert the API-side order record.
- [ ] Order tests cover list/detail, mock payment, cancel, refund state, and refresh persistence.
- [ ] Run Chromium customer UI tests against local and public. Run `@cross_browser` login/catalog smoke against Firefox and WebKit.
- [ ] Commit as `test: add customer Playwright page objects and UI flows`.

### Task 6: Implement admin Page Objects and UI tests

**Files:**
- Create: `automation-tests/pages/admin/login_page.py`
- Create: `automation-tests/pages/admin/dashboard_page.py`
- Create: `automation-tests/pages/admin/user_page.py`
- Create: `automation-tests/pages/admin/ticket_page.py`
- Create: `automation-tests/pages/admin/order_page.py`
- Create: `automation-tests/pages/admin/coupon_page.py`
- Create: `automation-tests/tests/ui/admin/test_admin_login.py`
- Create: `automation-tests/tests/ui/admin/test_admin_dashboard.py`
- Create: `automation-tests/tests/ui/admin/test_admin_users.py`
- Create: `automation-tests/tests/ui/admin/test_admin_tickets.py`
- Create: `automation-tests/tests/ui/admin/test_admin_orders.py`
- Create: `automation-tests/tests/ui/admin/test_admin_coupons.py`

- [ ] Add an `authenticated_admin_context` fixture that obtains the admin token through API and initializes the Zustand persisted auth state using the exact key observed in `frontend/src/store/authStore.ts`.
- [ ] Cover UI login success/failure, dashboard widgets, user search/create/detail/status, ticket list/filter/detail/status, order list/filter/detail/status, and coupon list/create/status/issue.
- [ ] For every UI write, query the API and assert the changed business state, then register the resource for cleanup.
- [ ] Run admin UI tests against public Chromium and the `@cross_browser` admin login smoke against Firefox/WebKit.
- [ ] Commit as `test: add admin Playwright page objects and UI flows`.

### Task 7: Add representative cross-layer E2E workflows

**Files:**
- Create: `automation-tests/tests/e2e/test_purchase_lifecycle.py`
- Create: `automation-tests/tests/e2e/test_coupon_purchase.py`
- Create: `automation-tests/tests/e2e/test_admin_customer_visibility.py`

- [ ] `test_purchase_lifecycle.py` performs API registration/login, browser token injection, UI selection/cart/checkout, UI mock payment, API paid-state assertion, UI refund request, and final cleanup.
- [ ] `test_coupon_purchase.py` creates or selects a run-owned coupon through admin API, issues it, applies it in the customer UI, and asserts the discounted order amount through API.
- [ ] `test_admin_customer_visibility.py` creates a customer through API, verifies it in admin UI search, disables it through UI, verifies login denial through customer API, and cleans it up.
- [ ] Attach `runId`, created resource IDs, order number, final states, and cleanup response to Allure without attaching secrets.
- [ ] Run all three against public Chromium with `--allow-write`; expect PASS and zero residual run-owned data.
- [ ] Commit as `test: add Booking Master cross layer E2E flows`.

### Task 8: Reporting, artifacts, and suite commands

**Files:**
- Modify: `automation-tests/conftest.py`
- Create: `automation-tests/framework/reporting/artifacts.py`
- Modify: `automation-tests/README.md`

- [ ] On UI failure attach screenshot, page HTML, console messages, and trace; keep video only when `--video=retain-on-failure` is selected.
- [ ] On API failure attach method, relative path, status, elapsed time, and redacted request/response summary.
- [ ] Document exact commands for local/public smoke, API, UI, E2E, regression, Chromium, and cross-browser execution.
- [ ] Run `python -m pytest --collect-only -q`, then smoke, API, UI, and E2E suites separately to prove markers select non-overlapping intended groups.
- [ ] Commit as `feat: publish Playwright diagnostics and runbook`.

### Task 9: Phase verification

- [ ] Run `python -m pytest tests/framework tests/api --env=public --allow-write -n 2 --dist=loadscope` and confirm no shared-user collision.
- [ ] Run `python -m pytest tests/ui tests/e2e --env=public --allow-write --browser=chromium`.
- [ ] Run `python -m pytest -m "smoke and cross_browser" --env=public --browser=firefox`, then WebKit.
- [ ] Confirm `legacy/api-tests-v1` contributes zero collected tests.
- [ ] Confirm every legacy test file has a terminal migration-matrix status.
- [ ] Query by every runId recorded in the reports and confirm no user, order, ticket, or coupon remains.
- [ ] Run `git diff --check` and commit any documentation-only corrections.
