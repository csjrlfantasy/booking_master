# Share And Coupons Design

## Background

The current ticketing platform already has partial coupon capabilities in the backend and Android client, but lacks a complete management workflow, Web user workflow, and a unified sharing experience. At the same time, the repository baseline has just been normalized, so this feature should be designed to build directly on the now-tracked codebase rather than introducing a parallel implementation.

This design covers two related product areas:

1. Share ticket content from the consumer side.
2. Complete coupon issuance, claiming, and order deduction across admin, Web, backend, and Android surfaces.

## Goals

1. Users can share ticket content from key consumer journeys without exposing private order data.
2. Admin users can create, manage, and manually issue coupons using existing coupon domain capabilities.
3. Consumer users can claim coupons, view their coupons, and use eligible coupons during checkout.
4. Coupon lifecycle stays consistent when orders are cancelled, timed out, or refunded.

## Non-Goals

1. No third-party SDK integration for WeChat, QQ, Weibo, Xiaohongshu, Douyin, or Kuaishou in this phase.
2. No social feed publishing, share callback tracking, referral growth system, or invitation rewards in this phase.
3. No new coupon domain model if existing `coupon` and `user_coupon` tables can cover the requirement.
4. No admin-side share management or content review system in this phase.

## Confirmed Product Decisions

### Share Entry Scope

Use option `3`:

1. Home ticket cards
2. Ticket detail page
3. Order detail page

### Share Target Behavior

All share actions point to the public consumer ticket detail page, not to private order pages. If the user shares from an order detail page, the shared payload still resolves to the related public ticket detail URL.

### Coupon Scope Strategy

Reuse existing coupon applicability modes:

1. Full-site coupon
2. Specific ticket coupon
3. Specific category coupon

## Existing System Context

### Backend

The backend already contains:

1. `Coupon` and `UserCoupon` entities
2. `CouponService` and `CouponServiceImpl`
3. Consumer coupon endpoints such as `/coupons`, `/coupons/list`, `/coupons/receive`, `/coupons/use`, and `/coupons/validate`
4. Existing order creation and cancellation flows

The current service already enforces:

1. Coupon status validation
2. Stock validation
3. Claim time validation
4. Per-user claim limit validation
5. Scope validation for ticket and category coupons
6. Coupon return on some order rollback paths

### Web Consumer Side

The Web consumer side already has:

1. A multi-tab shop flow in [ShopPage.tsx](D:/AIProjects/BookingApi/frontend/src/pages/shop/ShopPage.tsx)
2. Ticket detail and order detail states already driven by customer APIs
3. Existing customer API module in [customer.ts](D:/AIProjects/BookingApi/frontend/src/api/customer.ts)

### Android

The Android client already has:

1. Coupon API wrapper
2. Coupon selector component
3. Order summary and payment UI foundations

### Admin

Admin already has ticket, banner, flash sale, user, stats, and order management, but does not yet expose a complete coupon management workflow.

## Solution Overview

Implement a unified sharing capability on the consumer side and complete coupon management plus coupon consumption workflows on top of the existing coupon model.

The work splits into four surfaces:

1. Backend: extend admin coupon management and strengthen coupon-order lifecycle handling.
2. Web consumer: add share panel, coupon center, my coupons, and checkout coupon selection.
3. Web admin: add coupon management and manual issuance flows.
4. Android: add matching share entry points and coupon discovery flows without native SDK integration.

## Share Feature Design

### Supported Platforms

The share panel shows these platforms:

1. WeChat
2. QQ
3. Weibo
4. Xiaohongshu
5. Douyin
6. Kuaishou
7. Copy Link

### Share Payload

Each share action uses a normalized payload built from ticket data:

1. Ticket title
2. Subtitle if available
3. Show date and time
4. Venue name
5. Ticket cover image
6. Price range or starting price
7. Public consumer detail link

Optional fields reserved for future share tracking but not enabled in phase one:

1. `shareChannel`
2. `shareSource`
3. `shareToken`

These parameters may be appended to the generated URL but should not require backend tracking support in phase one.

### Share Entry Points

#### Home Ticket Cards

Each ticket card in the consumer home flow adds a compact share trigger. This must not interfere with the existing card click behavior that enters ticket details.

#### Ticket Detail Page

The detail page adds a primary visible share action near the ticket summary and purchase controls.

#### Order Detail Page

The order detail page adds a share action in the action area. It shares the related public ticket detail page and never includes order number, attendee data, contact info, or payment state in the public URL.

### Web Behavior

Clicking share opens a bottom sheet style panel with platform icons and short labels. The panel should feel native to the current consumer UI rather than like an admin modal.

Platform behavior:

1. `Copy Link`: always available, copies the generated URL and shows success feedback.
2. Other platforms: first try the relevant share URL, schema, or `window.open` strategy appropriate for the platform.
3. If the browser cannot complete the action, show a friendly fallback message indicating that the share text and link are ready and suggest copying the link.

### Android Behavior

Android gets the same set of visible share options and branding. Because native SDK integration is explicitly out of scope, the first phase behavior is:

1. Prefer system share intent where suitable
2. Fallback to copy-link behavior
3. Keep the platform icon choices visible so future SDK upgrades do not require a UX redesign

### Error Handling

Share should fail softly:

1. Missing ticket image should not block sharing
2. Missing optional fields fall back to available ticket basics
3. Failed clipboard access or popup blocking should surface a toast and a retry path

## Coupon Feature Design

### Coupon Management In Admin

Add a dedicated coupon management area to admin with these capabilities:

1. Create coupon
2. Edit coupon
3. Activate or deactivate coupon
4. End coupon distribution when needed
5. View coupon distribution and usage summary
6. Manually issue coupons

### Coupon Fields

Use the current coupon model as the source of truth:

1. Name
2. Coupon type
3. Coupon value
4. Minimum order amount
5. Maximum discount amount for discount coupons
6. Total count
7. Remaining count
8. Per-user limit
9. Receive type
10. Validity type
11. Fixed validity dates or valid days after claim
12. Applicable scope
13. Applicable tickets
14. Applicable categories
15. Description
16. Notice
17. Receive start time
18. Receive end time
19. Status

### Manual Issuance

Admin manual issuance supports:

1. Single user issuance
2. Batch issuance to selected user IDs

The first phase does not require dynamic user segmentation or rule-based mass campaigns. Batch issuance can be driven by explicit user selection from the admin side.

### Consumer Coupon Center

Add a coupon center page where consumers can:

1. Browse claimable coupons
2. See scope, threshold, amount, and validity
3. Claim eligible coupons

### Consumer My Coupons

Add a "My Coupons" area with three states:

1. Available
2. Used
3. Expired

The page should clearly surface:

1. Coupon type
2. Discount value
3. Minimum spend
4. Validity window
5. Status text
6. Scope description

### Checkout Coupon Usage

During checkout:

1. Load eligible coupons for the current order
2. Filter by order amount, ticket scope, category scope, status, and validity
3. Let the user choose one coupon
4. Recalculate discount and payable amount in real time
5. Persist coupon data into the resulting order

The first phase supports one coupon per order.

### Order Data Expectations

Orders should continue to store:

1. Coupon identifier
2. Coupon name
3. Coupon deduction amount
4. Final payable amount

The frontend must display those fields consistently on order detail and payment result screens.

## Coupon Lifecycle Rules

### Claim Validation

Claim flow must enforce:

1. Coupon exists
2. Coupon is active
3. Remaining stock is greater than zero
4. Current time is within receive window
5. User has not exceeded per-user claim limit

### Pending Payment Timeout

Orders already have a 15-minute unpaid timeout requirement. When a pending order expires and is cancelled:

1. Restore held inventory using the existing order expiration path
2. Return the coupon to unused state if that coupon had been bound to the order
3. If the coupon validity already elapsed by timeout handling time, mark it expired rather than reusable

### User Cancellation Or Refund

Coupon return behavior must stay consistent with refund logic:

1. If the order is cancelled before successful payment and the coupon was locked, return it to unused unless it has expired
2. If a paid order is refunded and coupon return is allowed in the current business path, restore it to unused unless it has expired
3. If coupon validity has ended at refund-processing time, transition it to expired rather than unused

This preserves financial correctness and avoids reviving invalid coupons.

## Permissions And Security

### Admin

Only admin-authenticated users can:

1. Create or edit coupons
2. Activate or deactivate coupons
3. Manually issue coupons
4. View coupon issuance summaries beyond their own account

### Consumer

Consumers can only:

1. View public claimable coupons
2. Claim their own coupons
3. View their own coupon lists and details
4. Use coupons bound to their own account

### Share Privacy

The share feature must never expose:

1. Order number
2. Contact name or phone
3. Identity card or attendee data
4. Payment channel or payment status

## Frontend UX Notes

### Web Consumer

The share panel should visually match the current consumer-side ticketing experience rather than the admin layout. It should be compact, icon-led, and optimized for quick tap targets.

Coupon pages should feel consumer-friendly:

1. Strong value display
2. Clear threshold text
3. Clear scope text
4. Obvious claim or use state

### Web Admin

Coupon management should align with existing admin table patterns:

1. Filterable list
2. Status chips
3. Batch-friendly row actions where useful
4. Drawer or modal form consistent with current admin editing patterns

### Android

Android should mirror the Web user mental model:

1. Coupon center
2. My coupons
3. Checkout selector
4. Share panel

No redesign of unrelated Android tabs is required in this phase.

## API And Data Flow

### Share Data Flow

1. Frontend loads ticket detail through existing ticket APIs
2. Frontend builds share payload locally
3. Frontend executes platform action
4. No mandatory backend write occurs in phase one

### Coupon Consumer Data Flow

1. User opens coupon center
2. Frontend requests claimable coupons
3. User claims coupon
4. Backend validates and creates `user_coupon`
5. Checkout requests available coupons for current order
6. User selects coupon
7. Order creation stores coupon fields
8. Payment timeout, cancel, or refund path updates coupon state

### Coupon Admin Data Flow

1. Admin creates or edits coupon
2. Backend persists coupon metadata
3. Admin issues coupon to one or more users
4. Backend creates `user_coupon` rows with source indicating admin issuance

## Testing Strategy

### Backend Tests

Add or extend tests for:

1. Admin create coupon validation
2. Admin edit coupon validation
3. Manual issuance to a single user
4. Batch issuance to multiple users
5. Claim flow happy path
6. Claim per-user limit rejection
7. Claim time-window rejection
8. Checkout available-coupon filtering by amount
9. Checkout available-coupon filtering by ticket scope
10. Checkout available-coupon filtering by category scope
11. Order timeout coupon return
12. Refund coupon return and expiry branch

### Web Tests

Add tests for:

1. Share entry rendering on home cards
2. Share entry rendering on ticket detail
3. Share entry rendering on order detail
4. Share URL generation
5. Copy-link success and failure handling
6. Coupon center list rendering
7. Coupon claim action feedback
8. Checkout coupon filtering and amount recalculation

### Android Tests

Add focused tests for:

1. Share panel rendering
2. Coupon center rendering
3. Coupon selection updates order summary

### Regression Areas

Regression verification must include:

1. Ticket detail purchase flow
2. Order creation
3. Mock payment
4. Timeout cancellation
5. Refund flow
6. Existing identity validation flow

## Risks And Mitigations

### Risk 1: Coupon State Drift

Risk:
Order, payment, timeout, and refund paths may drift if coupon updates are split across multiple code paths.

Mitigation:
Concentrate coupon state transitions in service-level methods and cover them with order lifecycle tests.

### Risk 2: Share UX Degrades On Browsers Without Native Support

Risk:
Some browsers will not honor share popup or schema behavior.

Mitigation:
Always keep copy-link as a guaranteed fallback and provide clear user feedback.

### Risk 3: Existing Coupon Model And Android Types Are Not Perfectly Aligned

Risk:
Some Android-side coupon UI currently expects shape differences from backend DTOs.

Mitigation:
Normalize response contracts at API wrapper level rather than altering the domain model unnecessarily.

## Recommended Delivery Order

1. Finalize spec
2. Write implementation plan
3. Extend backend coupon admin and lifecycle support
4. Add Web consumer sharing and coupon flows
5. Add Web admin coupon management
6. Add Android share and coupon pages
7. Run targeted regression

## Acceptance Criteria

The feature is complete when:

1. A consumer can share a ticket from home card, ticket detail, and order detail.
2. Copy-link always works on Web.
3. Admin can create, edit, activate, deactivate, and manually issue coupons.
4. Consumers can claim coupons and view their coupon states.
5. Checkout can apply one eligible coupon and display correct payable amount.
6. Coupon state remains consistent after order timeout, cancellation, and refund.
7. No private order information is exposed through shared links.
