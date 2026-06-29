# Release Quality And Ticketing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the project into a demo-ready teaching system with customer ticket purchase on mobile and H5, a stable admin backend, and repeatable automated quality gates.

**Architecture:** Keep one Spring Boot backend as the source of truth for tickets, inventory, orders, payments, users, coupons, and admin operations. Keep `booking-app` as the mobile customer client and add H5 customer routes under the existing Vite app at `/shop/*`, while preserving `/login` and protected admin routes. Quality gates run in layers: backend unit tests, API smoke tests, admin web type/build checks, mobile type checks, then UI smoke checks.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus, MySQL-compatible schema, React/Vite/Tailwind admin and H5, React Native mobile app, Maven, TypeScript, pytest API tests, PowerShell smoke scripts.

---

## Current Quality Baseline

- Backend full unit test command fails: `mvn -pl booking-api test`.
- Backend failures observed on 2026-05-25:
  - `UserControllerTest.testGetUserByIdSuccess`: missing mocked Authorization header.
  - `UserControllerTest.testGetUserByIdNotFound`: expected user-not-found code but request fails earlier as token invalid.
  - `OrderServicePaymentTest.testPaySuccessIgnoredForCompletedOrder`: completed orders are still updated by payment callback.
  - `UserServiceTest.testLoginSuccess`: test password does not match current BCrypt verification behavior.
  - `UserServiceTest.testLoginSuccessShouldResetFailCount`: locked user setup blocks success path.
- Admin frontend quality is currently green:
  - `npm.cmd run type-check` passes in `frontend`.
  - `npm.cmd run build` passes in `frontend`, with only chunk-size warning.
- Mobile app quality is currently red:
  - `npx.cmd tsc --noEmit` fails in `booking-app`.
  - Main error groups: broken imports, inconsistent design token names, navigation parameter mismatches, API DTO mismatches, invalid component props, ambiguous slice exports.
- Runtime smoke script currently cannot connect if backend is not running:
  - `scripts/full-smoke-test.ps1` needs startup precheck and clearer failure output.

## Functional Gap Register

### P0: Release Blocking

- Customer purchase flow is not complete.
  - `booking-app/src/screens/cart/CheckoutScreen.tsx` still uses mock address, mock coupon, mock cart items, and mock order ID.
  - `booking-app/src/screens/payment/PaymentScreen.tsx` uses mock fallback for payment.
  - H5 customer purchase pages do not exist.
- Inventory is not protected during order creation.
  - `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java` creates orders without stock deduction.
  - Cancel order has a TODO for stock restoration.
  - Price-level stock, ticket total stock, and session available capacity are not updated atomically.
- Payment callback has unsafe state transition.
  - Completed orders can be updated by a late payment callback.
- Mobile app cannot pass TypeScript, so it is not packageable.
- Backend full unit suite is red, so it cannot be used as a release gate.

### P1: Important Before Class Demo

- Admin edit/create functions are incomplete.
  - `frontend/src/pages/user/UserEditPage.tsx` shows success without calling an update API.
  - `frontend/src/pages/ticket/TicketCreatePage.tsx` shows success without calling a create API.
  - `booking-api/src/main/java/com/ticket/app/controller/admin/AdminUserController.java` update user still has TODO.
  - `booking-api/src/main/java/com/ticket/app/controller/admin/AdminOrderController.java` update order status still has TODO.
  - `AdminUserServiceImpl.assignUserRole` and `getRoleList` are simplified placeholders.
- Mobile profile, address, favorite, refund, venue map, and seat confirm screens still contain placeholder pages.
- Mobile APIs contain comments for backend-missing endpoints, including favorites, SMS/reset password, and some coupon/cart actions.
- Smoke testing exists but is not a packaged, reliable one-command quality gate.

### P2: Nice To Have

- Admin frontend bundle is large and should use route-level lazy loading.
- Mobile app uses many `any` types and mock fallback paths; these should be reduced after P0/P1 are stable.
- Test reports and generated Allure/result files are noisy in the repository and should be ignored or cleaned.
- API docs and implementation paths should be reconciled because older docs mention obsolete endpoint shapes.

---

## Acceptance Criteria

### Customer Ticket Purchase

- A registered/logged-in customer can view ticket list and detail on H5 `/shop` and mobile `booking-app`.
- A customer can select one price level and quantity.
- The client sends `ticketId`, `sessionId`, `priceLevelId`, `quantity`, contact name, contact phone, and optional remark.
- Backend validates:
  - token exists and belongs to the user;
  - ticket exists and is on sale;
  - session belongs to ticket and is available;
  - price level belongs to ticket and is available;
  - quantity is positive and not greater than available stock;
  - client price cannot silently override backend price.
- Backend creates an unpaid order and order items, deducting stock in the same transaction.
- Canceling an unpaid order restores stock.
- Payment creation returns a demo payment URL/result payload.
- Demo payment success moves order from unpaid to paid.
- Late payment callbacks do not update canceled, completed, refunded, or refunding orders.
- H5 and mobile both navigate to an order detail/result page after payment.

### Quality Gates

- Backend unit tests pass with:
  - `mvn -pl booking-api test`
- Admin web passes:
  - `npm.cmd run type-check`
  - `npm.cmd run build`
- Mobile app passes:
  - `npx.cmd tsc --noEmit`
- API smoke passes after services are started:
  - `powershell -ExecutionPolicy Bypass -File scripts/full-smoke-test.ps1`
- New purchase API smoke passes:
  - login/register;
  - ticket detail;
  - create order;
  - verify stock decreased;
  - create payment;
  - simulate payment callback or dev pay;
  - verify order paid;
  - cancel unpaid order restores stock.

---

## Implementation Phases

### Phase 0: Stabilize Quality Baseline

**Purpose:** Make current tests trustworthy before adding more purchase functionality.

**Files:**
- Modify: `booking-api/src/test/java/com/ticket/app/controller/user/UserControllerTest.java`
- Modify: `booking-api/src/test/java/com/ticket/app/service/user/UserServiceTest.java`
- Modify: `booking-api/src/test/java/com/ticket/app/service/order/OrderServicePaymentTest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java`
- Modify: `scripts/full-smoke-test.ps1`

- [ ] Fix user controller tests to set `Authorization: Bearer token` and mock `jwtUtils.getUserIdFromToken`.
- [ ] Fix user service tests to use real `PasswordUtils.encode("password123")` for login success cases.
- [ ] Fix payment callback behavior so only `STATUS_PENDING_PAY` orders can transition to paid.
- [ ] Update the completed-order payment test to assert no repository update.
- [ ] Add backend startup precheck to `scripts/full-smoke-test.ps1` with a clear message when `http://127.0.0.1:8088/api` is unreachable.
- [ ] Run `mvn -pl booking-api test`.
- [ ] Run `powershell -ExecutionPolicy Bypass -File scripts/full-smoke-test.ps1` after backend is started.

**Exit:** Backend tests are green and smoke failure messages are useful.

### Phase 1: Backend Purchase Core

**Purpose:** Make buying tickets correct and safe at the API layer.

**Files:**
- Create: `booking-api/src/test/java/com/ticket/app/service/order/OrderInventoryServiceTest.java`
- Create: `booking-api/src/test/java/com/ticket/app/controller/order/CustomerPurchaseFlowTest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/repository/ticket/TicketRepository.java`
- Modify: `booking-api/src/main/java/com/ticket/app/repository/ticket/TicketPriceLevelRepository.java`
- Modify: `booking-api/src/main/java/com/ticket/app/repository/ticket/TicketSessionRepository.java`
- Modify: `booking-api/src/main/resources/mapper/TicketMapper.xml`
- Modify: `booking-api/src/main/resources/mapper/TicketPriceLevelMapper.xml`

- [ ] Write a failing test: creating an order with sufficient stock deducts `ticket.availableStock`, `ticket_price_level.availableStock`, and `ticket_session.availableCapacity`.
- [ ] Write a failing test: creating an order with insufficient price-level stock throws a business error and inserts no order.
- [ ] Write a failing test: client-submitted unit price different from backend price is rejected.
- [ ] Add repository methods for conditional stock decrement using `available_stock >= quantity`.
- [ ] Implement stock deduction inside `createOrder` transaction before inserting order items.
- [ ] Implement stock restoration inside unpaid `cancelOrder`.
- [ ] Add a dev/demo payment completion endpoint or use signed callback helper in smoke tests.
- [ ] Run targeted order tests, then full backend tests.

**Exit:** Backend has safe create/cancel/pay order behavior and unit tests around inventory.

### Phase 2: Customer H5 Pages

**Purpose:** Add browser customer pages without breaking admin routes.

**Files:**
- Create: `frontend/src/api/shop.ts`
- Create: `frontend/src/pages/shop/ShopLayout.tsx`
- Create: `frontend/src/pages/shop/ShopHomePage.tsx`
- Create: `frontend/src/pages/shop/ShopTicketDetailPage.tsx`
- Create: `frontend/src/pages/shop/ShopCheckoutPage.tsx`
- Create: `frontend/src/pages/shop/ShopPaymentPage.tsx`
- Create: `frontend/src/pages/shop/ShopOrderResultPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/types/index.ts`

- [ ] Add public `/shop` route group outside `ProtectedRoute`.
- [ ] Implement ticket list and search using `/api/tickets`.
- [ ] Implement ticket detail using `/api/tickets/{id}`.
- [ ] Store pending purchase selection in route state or local storage.
- [ ] Implement checkout form with contact name and phone validation.
- [ ] Require customer token before order creation; redirect to a customer login state if token is missing.
- [ ] Create order via `/api/orders`.
- [ ] Create demo payment via `/api/payment/create`.
- [ ] Show payment/result page and link to order detail.
- [ ] Run `npm.cmd run type-check` and `npm.cmd run build`.

**Exit:** H5 purchase flow is usable at `http://127.0.0.1:3000/shop`.

### Phase 3: Mobile Purchase Flow

**Purpose:** Replace mock purchase paths in `booking-app` with real APIs and make the app packageable.

**Files:**
- Modify: `booking-app/src/constants/index.ts`
- Modify: `booking-app/src/constants/spacing.ts`
- Modify: `booking-app/src/constants/typography.ts`
- Modify: `booking-app/src/navigation/types.ts`
- Modify: `booking-app/src/services/api/orderApi.ts`
- Modify: `booking-app/src/services/api/paymentApi.ts`
- Modify: `booking-app/src/screens/ticket/TicketDetailScreen.tsx`
- Modify: `booking-app/src/screens/cart/CheckoutScreen.tsx`
- Modify: `booking-app/src/screens/payment/PaymentScreen.tsx`
- Modify: `booking-app/src/screens/payment/PaymentResultScreen.tsx`
- Modify: `booking-app/src/screens/order/OrderListScreen.tsx`
- Modify: `booking-app/src/screens/order/OrderDetailScreen.tsx`

- [ ] Fix TypeScript baseline errors caused by design token aliases, navigation params, component props, and API DTOs.
- [ ] Replace checkout mock cart item with the immediate-buy selection passed from ticket detail.
- [ ] Replace mock order ID with real `orderApi.createOrder` response.
- [ ] Replace payment mock fallback with `paymentApi.createPayment` and status query.
- [ ] Map backend order statuses to mobile order status labels.
- [ ] Remove mock fallback from purchase-critical pages.
- [ ] Run `npx.cmd tsc --noEmit`.

**Exit:** Mobile app TypeScript passes and purchase-critical pages use real APIs.

### Phase 4: Admin Completion

**Purpose:** Remove obvious admin placeholders that affect teaching demos.

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminUserController.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/admin/AdminUserServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminOrderController.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/admin/AdminOrderServiceImpl.java`
- Modify: `frontend/src/pages/user/UserEditPage.tsx`
- Modify: `frontend/src/pages/ticket/TicketCreatePage.tsx`
- Modify: `frontend/src/api/admin.ts`

- [ ] Implement admin user update endpoint and connect `UserEditPage`.
- [ ] Implement admin order status update using controlled transitions.
- [ ] Implement ticket creation or remove the create button until backend support is real.
- [ ] Add backend tests for admin update endpoints.
- [ ] Run admin route smoke and frontend build checks.

**Exit:** Admin pages do not show fake success for unsupported actions.

### Phase 5: Automated Quality Gate Package

**Purpose:** Make one command tell whether the project is demo/release ready.

**Files:**
- Create: `scripts/quality-gate.ps1`
- Create: `automation-tests/api-tests/tests/test_purchase_flow.py`
- Modify: `automation-tests/api-tests/config.py`
- Modify: `automation-tests/README.md`
- Modify: `.gitignore`

- [ ] Add `scripts/quality-gate.ps1` to run backend tests, admin type/build, mobile type-check, and API smoke in order.
- [ ] Add `test_purchase_flow.py` for customer purchase API happy path and stock-negative path.
- [ ] Add config switches for base URL and test user credentials.
- [ ] Mark generated report folders and `__pycache__` as ignored.
- [ ] Ensure quality gate returns non-zero on any failed required check.

**Exit:** A single local quality gate can be run before class or packaging.

---

## Regression Scope

## Customer Order Query And Mock Payment Plan

### Goal

Give learners a complete user-side loop after checkout: create an order, simulate WeChat or Alipay payment, query personal orders, open order detail, and retry payment for pending orders.

### Acceptance Criteria

- Customer can open `/shop`, log in or register, create an order from a real ticket/session/price level, and see a payment action immediately.
- Mock WeChat and Alipay payment call backend `/api/payment/mock-pay`, not a front-end-only fake state.
- Mock payment validates order ownership, amount, channel, pending status, and expiry through the payment service before marking the order paid.
- Customer can switch to `My Orders`, filter all/pending/paid orders, select an order, see item detail, amount, contact info, channel, trade number, and pay pending orders.
- H5 page uses local visual assets so the teaching demo still looks presentable without network images.

### Test Strategy

- Backend unit: `PaymentServiceMockPayTest` covers pending order to paid transition and generated mock trade number.
- Existing backend regression: order payment callback, payment state transitions, inventory, user auth, and ticket detail must remain green.
- Frontend quality: `npm run type-check` and `npm run build` must pass after every customer H5 change.
- Runtime smoke: register a temporary customer, create an order, mock-pay it, then query `/api/orders` and verify paid status.

### Remaining Follow-Up

- Add browser E2E around `/shop`: register/login, checkout, mock pay, order detail.
- Add mobile app parity for order list/detail and mock payment.
- Split customer H5 components if the page grows further.

### Must Regress

- Auth: customer login/register, admin login, invalid token handling.
- Ticket: list, detail, hot list, category list, price levels.
- Order: create, list, detail, cancel unpaid, complete paid.
- Inventory: sufficient stock, insufficient stock, cancel restoration, repeated payment callback.
- Payment: create payment, mock payment, status query, signed callback, cancel payment.
- Admin: dashboard stats, users list/detail/create/edit/status, orders list/detail/cancel/refund, tickets list/detail.
- H5: `/shop` list/detail/checkout/payment/result.
- Mobile: login, ticket detail, checkout, payment, order list/detail.

### Sample Regress

- Coupon receive/validate/use with one available and one unavailable coupon.
- Cart add/list/update/delete if purchase flow still supports cart purchase.
- Search and category screens.

### Manual Exploratory

- Browser mobile viewport for H5 purchase pages.
- Slow network and duplicate submit on checkout.
- Back button behavior after payment.
- Token expiration during checkout.
- Admin operating on the same order while customer is paying.

---

## Quality Gate Rules

### PASS

- Backend unit tests pass.
- Admin web type-check and build pass.
- Mobile TypeScript passes.
- API smoke passes against a running local backend.
- Purchase flow API tests pass.
- No P0 placeholder or mock remains in purchase-critical code paths.

### WARN

- Admin frontend chunk-size warning remains.
- Non-purchase mobile screens still have placeholders, but no purchase/auth/order P0 path depends on them.
- Generated reports exist locally but are ignored by git.

### FAIL

- Any P0 automated check fails.
- Backend can create orders without stock validation.
- Payment callback can mutate canceled/completed/refunded orders.
- H5 or mobile purchase creates mock orders instead of backend orders.
- Mobile app TypeScript cannot pass.

---

## Execution Order

1. Phase 0: make test baseline green.
2. Phase 1: backend purchase correctness.
3. Phase 2: H5 customer purchase.
4. Phase 3: mobile purchase and packageability.
5. Phase 4: admin placeholder cleanup.
6. Phase 5: one-command quality gate and report cleanup.

This order protects the project from building attractive screens on top of unsafe order and inventory behavior.
