# Identity Ticketing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real-name attendee identity management and age-based ticket purchase validation.

**Architecture:** Backend owns all identity-card validation, age calculation, ownership checks, and order enforcement. Web surfaces identity management and attendee selection, while admin ticket forms configure the minimum viewing age through the existing `ageLimit` field.

**Tech Stack:** Spring Boot, MyBatis Plus, MySQL schema initializers, React, TypeScript, React Query, lightweight Node source tests.

---

### Task 1: Backend Identity Validation

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/common/util/IdCardUtils.java`
- Test: `booking-api/src/test/java/com/ticket/app/common/util/IdCardUtilsTest.java`

- [ ] Write tests for valid card, invalid checksum, invalid birthday, and age calculation.
- [ ] Implement 18-digit mainland ID validation and age calculation.
- [ ] Run `mvn -pl booking-api -Dtest=IdCardUtilsTest test`.
- [ ] Commit as `feat: add id card validation utilities`.

### Task 2: User Identity Model And API

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/entity/UserIdentity.java`
- Create: `booking-api/src/main/java/com/ticket/app/repository/user/UserIdentityRepository.java`
- Create: `booking-api/src/main/java/com/ticket/app/config/UserIdentityTableInitializer.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/user/dto/UserIdentityRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/user/dto/UserIdentityResponse.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/user/UserIdentityService.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/user/UserIdentityServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/user/UserController.java`

- [ ] Write service tests for create/list/update/delete and ownership checks.
- [ ] Add table initializer for `user_identity`.
- [ ] Add repository, DTOs, service, and controller endpoints.
- [ ] Run targeted backend tests.
- [ ] Commit as `feat: add customer identity management api`.

### Task 3: Order Attendee Validation

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderRequest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/entity/OrderItem.java`
- Modify: `booking-api/src/main/java/com/ticket/app/config/OrderItemAttendeeSchemaInitializer.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java`
- Test: `booking-api/src/test/java/com/ticket/app/service/order/OrderIdentityValidationTest.java`

- [ ] Write failing tests for attendee count mismatch, duplicate ID card, invalid ID card, age too low, and successful order.
- [ ] Add request fields and order item attendee persistence.
- [ ] Validate total attendee count equals total quantity.
- [ ] Validate identity ownership when `identityId` is provided.
- [ ] Validate age using `ticket.ageLimit`.
- [ ] Run targeted backend tests.
- [ ] Commit as `feat: enforce attendee identity checks on orders`.

### Task 4: Web API Types

**Files:**
- Modify: `frontend/src/api/customer.ts`
- Modify: `frontend/src/types/index.ts`
- Test: `frontend/tests/identityTicketingSource.test.mjs`

- [ ] Write source test requiring identity APIs and `attendees` in create order payload.
- [ ] Add customer identity request/response types and API functions.
- [ ] Extend create order payload types.
- [ ] Run `node frontend/tests/identityTicketingSource.test.mjs`.
- [ ] Commit as `feat: add web identity ticketing api types`.

### Task 5: Web Customer Identity UI And Purchase Selection

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Test: `frontend/tests/shopIdentitySelectionSource.test.mjs`

- [ ] Write source test requiring “常用观演人”, “身份证”, and attendee payload mapping.
- [ ] Add personal center identity management panel.
- [ ] Add ticket detail attendee selector matched to quantity.
- [ ] Block order submit when attendee count is insufficient.
- [ ] Run frontend source tests and `npm.cmd run build` in `frontend`.
- [ ] Commit as `feat: add web attendee identity selection`.

### Task 6: Admin Ticket Age Limit Field

**Files:**
- Modify: `frontend/src/pages/ticket/TicketCreatePage.tsx`
- Modify: `frontend/src/pages/ticket/TicketEditPage.tsx`
- Modify: `frontend/src/api/admin.ts`
- Test: `frontend/tests/adminTicketAgeLimitSource.test.mjs`

- [ ] Write source test requiring age-limit options in create and edit pages.
- [ ] Add age-limit select with `0+`, `6+`, `12+`, `16+`, `18+`.
- [ ] Ensure API payload sends `ageLimit`.
- [ ] Run source test and frontend build.
- [ ] Commit as `feat: add admin ticket age limit controls`.

### Task 7: Final Verification

**Files:**
- No production files unless fixing test failures.

- [ ] Run backend targeted tests.
- [ ] Run frontend source tests.
- [ ] Run `npx.cmd tsc --noEmit` in `booking-app` if shared types change.
- [ ] Check `http://127.0.0.1:3000/shop` and backend ticket endpoint return 200.
- [ ] Commit fixes if needed.
