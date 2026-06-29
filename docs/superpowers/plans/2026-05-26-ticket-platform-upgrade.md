# Ticket Platform Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the customer-facing ticket site into a responsive ticketing portal with category browsing, upcoming-sale discovery, admin ticket status controls, region-based ticket prices, and ticket comments.

**Architecture:** Reuse the existing Spring Boot + MyBatis-Plus backend and React/Vite frontend. Keep `/shop` as the customer entry, make it responsive for desktop and mobile, and add focused API endpoints for status, upcoming tickets, regions, and comments instead of introducing a second customer app.

**Tech Stack:** Java 17, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, React, TypeScript, TanStack Query, Tailwind CSS, Vite.

---

## Phase 1: Customer Home, Categories, Upcoming, And Admin Status

### Task 1: Admin Ticket Status API

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminTicketController.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminUpdateTicketStatusRequest.java`
- Test: `booking-api/src/test/java/com/ticket/app/controller/admin/AdminTicketStatusControllerTest.java`

- [ ] **Step 1: Write failing test**

Test should assert that `PUT /api/admin/tickets/{id}/status` persists a new status and recalculates sold-out state only when explicitly requested by stock logic later.

- [ ] **Step 2: Run test**

Run: `mvn -pl booking-api -Dtest=AdminTicketStatusControllerTest test`
Expected: compile or route failure because the status API does not exist.

- [ ] **Step 3: Implement request DTO and controller method**

Add `status` validation. Allowed values are `0` offline, `1` upcoming, `2` on sale, `3` sold out, `4` ended.

- [ ] **Step 4: Run test**

Expected: PASS.

### Task 2: Public Ticket List Should Hide Offline Tickets

**Files:**
- Modify: `booking-api/src/main/resources/mapper/ticket/TicketMapper.xml`
- Test: `booking-api/src/test/java/com/ticket/app/service/ticket/TicketVisibilityServiceTest.java`

- [ ] **Step 1: Write failing test**

Test should verify customer list returns only `STATUS_ON_SALE` and `STATUS_PENDING`, never `STATUS_OFFLINE`.

- [ ] **Step 2: Implement query filter**

Public ticket list and hot list should exclude offline and ended tickets.

- [ ] **Step 3: Run ticket service tests**

Run: `mvn -pl booking-api -Dtest=TicketVisibilityServiceTest test`

### Task 3: Upcoming Tickets API

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/ticket/TicketController.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/ticket/TicketService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/ticket/TicketServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/repository/ticket/TicketRepository.java`
- Modify: `booking-api/src/main/resources/mapper/ticket/TicketMapper.xml`
- Test: `booking-api/src/test/java/com/ticket/app/service/ticket/UpcomingTicketServiceTest.java`

- [ ] **Step 1: Write failing service test**

Test should assert upcoming returns tickets with `status=1` or `bookStartTime > now`, ordered by `bookStartTime`.

- [ ] **Step 2: Implement repository query and controller endpoint**

Endpoint: `GET /api/tickets/upcoming?limit=10`.

- [ ] **Step 3: Run test**

Run: `mvn -pl booking-api -Dtest=UpcomingTicketServiceTest test`

### Task 4: Customer Homepage API Client

**Files:**
- Modify: `frontend/src/api/customer.ts`

- [ ] **Step 1: Add typed API helpers**

Add `getTicketCategories`, `getUpcomingTickets`, and allow `getShopTickets` to accept `categoryId`.

- [ ] **Step 2: Build**

Run: `npm.cmd run build` in `frontend`.

### Task 5: Responsive Customer Homepage

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Optionally create: `frontend/src/pages/shop/homeSections.ts`

- [ ] **Step 1: Convert customer page into responsive portal**

Desktop layout:
- Header with logo, city, nav, search, login.
- Hero/banner.
- Category icon row.
- Sectioned ticket grids.

Mobile layout:
- App-style search.
- Category grid.
- Promo cards.
- Horizontal ticket rails.
- Bottom nav.

- [ ] **Step 2: Keep existing checkout workflow**

Ticket selection, order creation, payment, and order query must continue to work.

- [ ] **Step 3: Build**

Run: `npm.cmd run build`.

## Phase 2: Region-Based Ticket Prices

### Task 6: Admin Multi-Region Price Editing

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCreateTicketRequest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminTicketController.java`
- Modify: `frontend/src/pages/ticket/TicketCreatePage.tsx`
- Modify: `frontend/src/pages/ticket/TicketEditPage.tsx`
- Test: `booking-api/src/test/java/com/ticket/app/controller/admin/AdminTicketRegionControllerTest.java`

Use existing `TicketPriceLevel` rows as regions. Each row stores `name`, `price`, `totalStock`, `availableStock`, `description`, and `sortOrder`.

### Task 7: Customer Region Selection

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Test existing backend: `booking-api/src/test/java/com/ticket/app/service/order/OrderInventoryServiceTest.java`

Customer detail should show region chips. Order payload continues to send `priceLevelId`.

## Phase 3: Ticket Comments

### Task 8: Comment Domain And API

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/entity/TicketComment.java`
- Create: `booking-api/src/main/java/com/ticket/app/repository/ticket/TicketCommentRepository.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/ticket/TicketCommentController.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/ticket/TicketCommentService.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/ticket/TicketCommentServiceImpl.java`
- Test: `booking-api/src/test/java/com/ticket/app/service/ticket/TicketCommentServiceTest.java`

Rules:
- User must have a paid or completed order for the ticket.
- One order can create one comment.
- Rating range is 1 to 5.
- Content length is 5 to 500 characters.

### Task 9: Comment UI And Admin Moderation

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Create: `frontend/src/pages/comment/AdminCommentListPage.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/Sidebar.tsx`

Customer sees public comments on ticket details and can post after purchase. Admin can hide or show comments.

## Verification

- Backend: `mvn -pl booking-api test`
- Frontend: `npm.cmd run build`
- Smoke:
  - Admin can create an upcoming ticket.
  - Admin can set a ticket offline.
  - Customer homepage hides offline tickets.
  - Customer homepage shows categories and upcoming tickets.
  - Customer can still create and pay an order.
  - Uploaded ticket images still render through `/api/uploads/**`.
