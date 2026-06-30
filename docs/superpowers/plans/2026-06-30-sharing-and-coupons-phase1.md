# Sharing And Coupons Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Phase 1 sharing flow and coupon flow for the ticketing platform across backend, Web consumer, Web admin, Android, and automated regression coverage.

**Architecture:** Sharing is a pure client capability that always points to the public ticket detail page, so we keep it in UI utilities plus page-level entry points. Coupons are a domain capability and must be corrected in the order lifecycle first: admin creates/issues coupons, users claim/select coupons, orders bind a claimed `userCouponId`, payment marks coupon usage, cancel/timeout/refund returns or expires the coupon consistently. Although sharing and coupons are separate subsystems, they meet at the same consumer ticket and order pages, so this plan sequences backend contract stabilization before UI work.

**Tech Stack:** Spring Boot + MyBatis-Plus, React + React Query + React Router, React Native, Python API tests, source-level `.mjs` UI regression tests.

---

## File Structure

### Backend
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderRequest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderResponse.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/OrderDetailResponse.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/coupon/CouponController.java`
- Modify: `booking-api/src/main/java/com/ticket/app/entity/Order.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderExpirationService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/coupon/CouponService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/coupon/CouponServiceImpl.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminCouponController.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponCreateRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponIssueRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponStatusRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/response/AdminCouponListItemResponse.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponService.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponServiceImpl.java`
- Create: `booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java`
- Create: `booking-api/src/test/java/com/ticket/app/service/admin/AdminCouponServiceTest.java`

### Web Consumer + Shared UI
- Modify: `frontend/src/api/customer.ts`
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Modify: `frontend/src/pages/order/OrderDetailPage.tsx`
- Modify: `frontend/src/pages/order/OrderListPage.tsx`
- Create: `frontend/src/features/share/shareTargets.ts`
- Create: `frontend/src/features/share/ShareDialog.tsx`
- Create: `frontend/src/features/coupon/CouponCenterPanel.tsx`
- Create: `frontend/src/features/coupon/CouponPicker.tsx`
- Create: `frontend/src/features/coupon/couponHelpers.ts`
- Create: `frontend/tests/shareDialogSource.test.mjs`
- Create: `frontend/tests/customerCouponApiSource.test.mjs`
- Create: `frontend/tests/shopCouponShareSource.test.mjs`

### Web Admin
- Modify: `frontend/src/api/admin.ts`
- Modify: `frontend/src/App.tsx`
- Create: `frontend/src/pages/coupon/AdminCouponListPage.tsx`
- Create: `frontend/src/pages/coupon/index.ts`
- Create: `frontend/tests/adminCouponPageSource.test.mjs`

### Android
- Modify: `booking-app/src/services/api/couponApi.ts`
- Modify: `booking-app/src/screens/cart/CheckoutScreen.tsx`
- Modify: `booking-app/src/screens/home/HomeScreen.tsx`
- Modify: `booking-app/src/screens/ticket/TicketDetailScreen.tsx`
- Modify: `booking-app/src/screens/order/OrderDetailScreen.tsx`
- Modify: `booking-app/src/screens/profile/ProfileScreen.tsx`
- Modify: `booking-app/src/components/order/CouponSelector.tsx`
- Create: `booking-app/src/utils/shareTicket.ts`
- Create: `booking-app/tests/mobileCouponShareSource.test.mjs`

### API Automation
- Modify: `automation-tests/api-tests/tests/test_coupon.py`
- Modify: `automation-tests/api-tests/tests/test_order.py`
- Create: `automation-tests/api-tests/tests/test_admin_coupon.py`

## Task 1: Stabilize Coupon Domain Contracts In Backend

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderRequest.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderResponse.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/OrderDetailResponse.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/coupon/CouponController.java`
- Modify: `booking-api/src/main/java/com/ticket/app/entity/Order.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/coupon/CouponService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/coupon/CouponServiceImpl.java`
- Test: `booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java`

- [ ] **Step 1: Write the failing backend lifecycle test**

```java
@Test
void should_bind_user_coupon_and_calculate_discount_before_payment() {
    CreateOrderRequest request = new CreateOrderRequest();
    request.setTicketId(11L);
    request.setSessionId(21L);
    request.setUserCouponId(301L);
    request.setContactName("测试用户");
    request.setContactPhone("13800138000");
    request.setSource("web");

    CreateOrderRequest.OrderItemRequest item = new CreateOrderRequest.OrderItemRequest();
    item.setPriceLevelId(41L);
    item.setQuantity(2);
    item.setUnitPrice(new BigDecimal("280.00"));
    request.setItems(List.of(item));

    CreateOrderResponse response = orderService.createOrder(1001L, request);

    Order saved = orderRepository.selectById(response.getOrderId());
    assertThat(saved.getUserCouponId()).isEqualTo(301L);
    assertThat(saved.getCouponId()).isEqualTo(101L);
    assertThat(saved.getCouponAmount()).isEqualTo(new BigDecimal("30.00"));
    assertThat(response.getDiscountAmount()).isEqualTo(new BigDecimal("30.00"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -pl booking-api -Dtest=OrderCouponLifecycleTest test`

Expected: FAIL because `userCouponId` does not exist in request/entity and order creation does not validate or calculate coupon deductions.

- [ ] **Step 3: Add explicit claimed-coupon fields to request/entity/response**

```java
// CreateOrderRequest.java
@Schema(description = "用户优惠券ID", example = "301")
private Long userCouponId;

// Order.java
private Long userCouponId;
private String couponName;

// CreateOrderResponse.java
private Long userCouponId;
private Long couponId;
private BigDecimal couponAmount;
```

- [ ] **Step 4: Add coupon preview/claim validation contract in coupon service**

```java
public interface CouponService {
    PageResult<CouponCenterResponse> getCouponCenter(Long userId, Integer page, Integer pageSize);
    ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long ticketId, Integer categoryId);
    void markCouponUsed(Long userId, Long userCouponId, Long orderId);
    void returnCoupon(Long userId, Long orderId, boolean forceExpireIfOutOfWindow);
}
```

- [ ] **Step 5: Expose separate “领券中心” and “我的优惠券” controller endpoints**

```java
@GetMapping("/center")
public Result<PageResult<CouponCenterResponse>> getCouponCenter(
    @RequestParam(defaultValue = "1") Integer page,
    @RequestParam(defaultValue = "10") Integer pageSize,
    HttpServletRequest request
) {
    Long userId = getUserIdFromRequest(request);
    return Result.success(couponService.getCouponCenter(userId, page, pageSize));
}

@GetMapping("/list")
public Result<PageResult<UserCouponResponse>> getUserCouponList(...) { ... }
```

- [ ] **Step 6: Implement service logic so validation uses `userCouponId`, not template id**

```java
@Override
public ValidateCouponResponse validateUserCoupon(Long userId, Long userCouponId, BigDecimal orderAmount, Long ticketId, Integer categoryId) {
    UserCoupon userCoupon = userCouponRepository.selectById(userCouponId);
    if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
        throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "用户优惠券不存在");
    }
    Coupon coupon = couponRepository.selectById(userCoupon.getCouponId());
    if (coupon == null) {
        throw new BusinessException(ResultCode.COUPON_NOT_FOUND, "优惠券模板不存在");
    }
    // 继续复用现有金额、范围、有效期校验，并返回 discountAmount / couponId / couponName
}
```

- [ ] **Step 7: Run backend test again**

Run: `.\mvnw.cmd -pl booking-api -Dtest=OrderCouponLifecycleTest test`

Expected: PASS for the new contract test.

- [ ] **Step 8: Commit**

```bash
git add booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderRequest.java booking-api/src/main/java/com/ticket/app/controller/order/dto/CreateOrderResponse.java booking-api/src/main/java/com/ticket/app/controller/order/dto/OrderDetailResponse.java booking-api/src/main/java/com/ticket/app/controller/coupon/CouponController.java booking-api/src/main/java/com/ticket/app/entity/Order.java booking-api/src/main/java/com/ticket/app/service/coupon/CouponService.java booking-api/src/main/java/com/ticket/app/service/coupon/CouponServiceImpl.java booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java
git commit -m "feat: stabilize coupon order contract"
```

## Task 2: Connect Coupon Lifecycle To Order Create, Pay, Cancel, Timeout, Refund

**Files:**
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java`
- Modify: `booking-api/src/main/java/com/ticket/app/service/order/OrderExpirationService.java`
- Modify: `booking-api/src/main/java/com/ticket/app/controller/order/dto/OrderDetailResponse.java`
- Test: `booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java`

- [ ] **Step 1: Extend the failing test with cancel/timeout/payment cases**

```java
@Test
void should_return_coupon_when_pending_order_is_cancelled_or_expired() {
    Long orderId = createPendingOrderWithCoupon();
    orderService.cancelOrder(1001L, cancelRequest(orderId));
    UserCoupon userCoupon = userCouponRepository.selectById(301L);
    assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.STATUS_UNUSED);
    assertThat(userCoupon.getOrderId()).isNull();
}

@Test
void should_mark_coupon_used_after_payment_success() {
    Long orderId = createPendingOrderWithCoupon();
    orderService.paySuccess("ORD202606300001", "wechat", "WX-MOCK-1");
    UserCoupon userCoupon = userCouponRepository.selectById(301L);
    assertThat(userCoupon.getStatus()).isEqualTo(UserCoupon.STATUS_USED);
    assertThat(userCoupon.getOrderId()).isEqualTo(orderId);
}
```

- [ ] **Step 2: Run the lifecycle test**

Run: `.\mvnw.cmd -pl booking-api -Dtest=OrderCouponLifecycleTest test`

Expected: FAIL because `createOrder`, `paySuccess`, and expiration handling do not yet synchronize `user_coupon`.

- [ ] **Step 3: Wire coupon validation into order creation before stock deduction**

```java
ValidateCouponResponse couponValidation = null;
if (request.getUserCouponId() != null) {
    couponValidation = couponService.validateUserCoupon(
        userId,
        request.getUserCouponId(),
        totalAmount,
        ticket.getId(),
        ticket.getCategoryId()
    );
    discountAmount = couponValidation.getDiscountAmount();
    couponAmount = couponValidation.getDiscountAmount();
}
```

- [ ] **Step 4: Persist coupon fields and mark usage only after pay success**

```java
order.setUserCouponId(request.getUserCouponId());
order.setCouponId(couponValidation != null ? couponValidation.getCouponId() : null);
order.setCouponName(couponValidation != null ? couponValidation.getCouponName() : null);
order.setCouponAmount(couponAmount);

// paySuccess(...)
if (order.getUserCouponId() != null) {
    couponService.markCouponUsed(order.getUserId(), order.getUserCouponId(), order.getId());
}
```

- [ ] **Step 5: Return coupons on cancel and 15-minute timeout**

```java
if (order.getUserCouponId() != null) {
    couponService.returnCoupon(userId, order.getId(), false);
}

// OrderExpirationService.java
expiredOrders.forEach(order -> {
    orderRepository.markExpired(order.getId(), LocalDateTime.now());
    couponService.returnCoupon(order.getUserId(), order.getId(), false);
});
```

- [ ] **Step 6: Return or expire coupons on refund based on business rule**

```java
boolean forceExpire = refundAfterCouponWindow(order);
couponService.returnCoupon(order.getUserId(), order.getId(), forceExpire);
```

- [ ] **Step 7: Expose coupon fields in order detail/list response**

```java
response.setUserCouponId(order.getUserCouponId());
response.setCouponId(order.getCouponId());
response.setCouponName(order.getCouponName());
response.setCouponAmount(order.getCouponAmount());
```

- [ ] **Step 8: Run tests**

Run: `.\mvnw.cmd -pl booking-api -Dtest=OrderCouponLifecycleTest test`

Expected: PASS.

- [ ] **Step 9: Commit**

```bash
git add booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java booking-api/src/main/java/com/ticket/app/service/order/OrderExpirationService.java booking-api/src/main/java/com/ticket/app/controller/order/dto/OrderDetailResponse.java booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java
git commit -m "feat: connect coupon lifecycle to orders"
```

## Task 3: Build Admin Coupon Management And Manual Issuance APIs

**Files:**
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/AdminCouponController.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponCreateRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponIssueRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponStatusRequest.java`
- Create: `booking-api/src/main/java/com/ticket/app/controller/admin/dto/response/AdminCouponListItemResponse.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponService.java`
- Create: `booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponServiceImpl.java`
- Create: `booking-api/src/test/java/com/ticket/app/service/admin/AdminCouponServiceTest.java`

- [ ] **Step 1: Write the failing admin coupon service tests**

```java
@Test
void should_create_coupon_and_issue_to_specific_users() {
    Long couponId = adminCouponService.create(buildCreateRequest());
    adminCouponService.issue(couponId, List.of(1001L, 1002L), "manual");

    assertThat(userCouponRepository.selectCount(
        new LambdaQueryWrapper<UserCoupon>().eq(UserCoupon::getCouponId, couponId)
    )).isEqualTo(2);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\mvnw.cmd -pl booking-api -Dtest=AdminCouponServiceTest test`

Expected: FAIL because no admin coupon service/controller exists.

- [ ] **Step 3: Create admin service and CRUD/status/issue contracts**

```java
public interface AdminCouponService {
    Long create(AdminCouponCreateRequest request);
    void update(Long id, AdminCouponCreateRequest request);
    PageResult<AdminCouponListItemResponse> list(String keyword, Integer status, Integer page, Integer pageSize);
    void updateStatus(Long id, Integer status);
    void issue(Long couponId, List<Long> userIds, String issueMode);
}
```

- [ ] **Step 4: Create controller routes under `/admin/coupons`**

```java
@RestController
@RequestMapping("/admin/coupons")
public class AdminCouponController {
    @PostMapping public Result<Long> create(@Valid @RequestBody AdminCouponCreateRequest request) { ... }
    @PutMapping("/{id}") public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminCouponCreateRequest request) { ... }
    @GetMapping public Result<PageResult<AdminCouponListItemResponse>> list(...) { ... }
    @PutMapping("/{id}/status") public Result<Void> updateStatus(...) { ... }
    @PostMapping("/{id}/issue") public Result<Void> issue(@PathVariable Long id, @Valid @RequestBody AdminCouponIssueRequest request) { ... }
}
```

- [ ] **Step 5: Ensure issuance respects stock, per-user limit, and validity window**

```java
for (Long userId : request.getUserIds()) {
    if (alreadyReachedPerLimit(userId, coupon)) {
        continue;
    }
    userCouponRepository.insert(buildIssuedUserCoupon(userId, coupon, request.getIssueMode()));
}
coupon.setRemainCount(coupon.getRemainCount() - issuedCount);
couponRepository.updateById(coupon);
```

- [ ] **Step 6: Run tests**

Run: `.\mvnw.cmd -pl booking-api -Dtest=AdminCouponServiceTest test`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add booking-api/src/main/java/com/ticket/app/controller/admin/AdminCouponController.java booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponCreateRequest.java booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponIssueRequest.java booking-api/src/main/java/com/ticket/app/controller/admin/dto/request/AdminCouponStatusRequest.java booking-api/src/main/java/com/ticket/app/controller/admin/dto/response/AdminCouponListItemResponse.java booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponService.java booking-api/src/main/java/com/ticket/app/service/admin/AdminCouponServiceImpl.java booking-api/src/test/java/com/ticket/app/service/admin/AdminCouponServiceTest.java
git commit -m "feat: add admin coupon management api"
```

## Task 4: Finish Web Consumer Coupon Center, Coupon Picker, And Checkout Deduction

**Files:**
- Modify: `frontend/src/api/customer.ts`
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Modify: `frontend/src/pages/order/OrderDetailPage.tsx`
- Modify: `frontend/src/pages/order/OrderListPage.tsx`
- Create: `frontend/src/features/coupon/CouponCenterPanel.tsx`
- Create: `frontend/src/features/coupon/CouponPicker.tsx`
- Create: `frontend/src/features/coupon/couponHelpers.ts`
- Test: `frontend/tests/customerCouponApiSource.test.mjs`
- Test: `frontend/tests/shopCouponShareSource.test.mjs`

- [ ] **Step 1: Write failing source-level tests for coupon API and shop usage**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('customer api exposes coupon center functions', () => {
  const source = fs.readFileSync('frontend/src/api/customer.ts', 'utf8');
  assert.match(source, /getCouponCenter/);
  assert.match(source, /receiveCoupon/);
  assert.match(source, /getMyCoupons/);
});
```

- [ ] **Step 2: Run tests to verify failure**

Run: `node --test frontend/tests/customerCouponApiSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs`

Expected: FAIL because these functions and UI hooks do not exist yet.

- [ ] **Step 3: Add coupon interfaces and endpoints to customer API**

```ts
export interface CustomerCoupon {
  id: number;
  couponId: number;
  couponName: string;
  couponAmount?: number;
  couponType: number;
  status: number;
  statusText?: string;
  validEndTime?: string;
}

export interface CouponCenterItem {
  id: number;
  name: string;
  couponType: number;
  couponValue: number;
  remainCount: number;
  receiveEndTime?: string;
}

export function getCouponCenter(token: string, params?: { page?: number; pageSize?: number }) {
  return get<PageResult<CouponCenterItem>>('/coupons/center', params, { headers: { Authorization: `Bearer ${token}` } });
}

export function getMyCoupons(token: string, params?: { status?: number; page?: number; pageSize?: number }) {
  return get<PageResult<CustomerCoupon>>('/coupons/list', params, { headers: { Authorization: `Bearer ${token}` } });
}

export function receiveCoupon(token: string, couponId: number) {
  return post<number>('/coupons/receive', undefined, { params: { couponId }, headers: { Authorization: `Bearer ${token}` } });
}
```

- [ ] **Step 4: Add coupon helpers and picker UI**

```ts
export function resolveCouponDiscount(orderAmount: number, coupon?: CustomerCoupon) {
  if (!coupon) return 0;
  return Number(coupon.couponAmount ?? 0);
}
```

```tsx
<CouponPicker
  token={customerToken}
  ticketId={detail?.id}
  categoryId={detail?.categoryId}
  orderAmount={totalAmount}
  selectedCoupon={selectedCoupon}
  onSelect={setSelectedCoupon}
/>
```

- [ ] **Step 5: Pass `userCouponId` in order creation and render deduction in order detail**

```ts
createCustomerOrder(customerToken, {
  ticketId: detail.id,
  sessionId: selectedSessionId!,
  userCouponId: selectedCoupon?.id,
  items,
  contactName,
  contactPhone,
  source: 'web',
});
```

```tsx
{order.couponAmount ? (
  <div className="text-sm text-rose-600">优惠券抵扣 -{currency(order.couponAmount)}</div>
) : null}
```

- [ ] **Step 6: Add coupon center entry to “我的” and order list**

```tsx
<button type="button" onClick={() => setOpenCouponCenter(true)} className="...">
  优惠券
</button>
<CouponCenterPanel open={openCouponCenter} token={customerToken} />
```

- [ ] **Step 7: Run tests**

Run: `node --test frontend/tests/customerCouponApiSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add frontend/src/api/customer.ts frontend/src/pages/shop/ShopPage.tsx frontend/src/pages/order/OrderDetailPage.tsx frontend/src/pages/order/OrderListPage.tsx frontend/src/features/coupon/CouponCenterPanel.tsx frontend/src/features/coupon/CouponPicker.tsx frontend/src/features/coupon/couponHelpers.ts frontend/tests/customerCouponApiSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs
git commit -m "feat: add web coupon center and checkout deduction"
```

## Task 5: Add Web Share Dialog And Share Entry Points

**Files:**
- Create: `frontend/src/features/share/shareTargets.ts`
- Create: `frontend/src/features/share/ShareDialog.tsx`
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Modify: `frontend/src/pages/order/OrderDetailPage.tsx`
- Test: `frontend/tests/shareDialogSource.test.mjs`
- Test: `frontend/tests/shopCouponShareSource.test.mjs`

- [ ] **Step 1: Write the failing source-level sharing tests**

```js
test('share dialog includes all configured targets', () => {
  const source = fs.readFileSync('frontend/src/features/share/ShareDialog.tsx', 'utf8');
  for (const target of ['wechat', 'qq', 'weibo', 'xiaohongshu', 'douyin', 'kuaishou', 'copy']) {
    assert.match(source, new RegExp(target));
  }
});
```

- [ ] **Step 2: Run tests to verify failure**

Run: `node --test frontend/tests/shareDialogSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs`

Expected: FAIL because share feature files do not exist.

- [ ] **Step 3: Create target registry with public-ticket URLs only**

```ts
export const shareTargets = [
  { key: 'wechat', label: '微信', icon: WechatIcon },
  { key: 'qq', label: 'QQ', icon: QQIcon },
  { key: 'weibo', label: '微博', icon: WeiboIcon },
  { key: 'xiaohongshu', label: '小红书', icon: XiaohongshuIcon },
  { key: 'douyin', label: '抖音', icon: DouyinIcon },
  { key: 'kuaishou', label: '快手', icon: KuaishouIcon },
  { key: 'copy', label: '复制链接', icon: CopyIcon },
] as const;

export function buildTicketShareUrl(ticketId: number) {
  return `${window.location.origin}/shop/tickets/${ticketId}`;
}
```

- [ ] **Step 4: Create dialog with native share / open URL / clipboard fallback**

```tsx
async function handleShare(target: ShareTargetKey) {
  if (target === 'copy') {
    await navigator.clipboard.writeText(url);
    toast.success('链接已复制');
    return;
  }
  const text = `${title} ${url}`;
  if (navigator.share) {
    await navigator.share({ title, text, url });
    return;
  }
  window.open(buildTargetHref(target, title, url), '_blank', 'noopener,noreferrer');
}
```

- [ ] **Step 5: Mount share entry points on ticket cards, ticket detail, order detail**

```tsx
<button type="button" onClick={() => openShareDialog(ticket.id)} aria-label="分享票务">
  <Share2 className="h-4 w-4" />
</button>
```

```tsx
<ShareDialog
  open={Boolean(shareTicket)}
  title={shareTicket?.name || shareTicket?.title || ''}
  url={shareTicket ? buildTicketShareUrl(shareTicket.id) : ''}
  onClose={() => setShareTicket(null)}
/>
```

- [ ] **Step 6: Run tests**

Run: `node --test frontend/tests/shareDialogSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/features/share/shareTargets.ts frontend/src/features/share/ShareDialog.tsx frontend/src/pages/shop/ShopPage.tsx frontend/src/pages/order/OrderDetailPage.tsx frontend/tests/shareDialogSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs
git commit -m "feat: add web share dialog and entry points"
```

## Task 6: Build Web Admin Coupon Page

**Files:**
- Modify: `frontend/src/api/admin.ts`
- Modify: `frontend/src/App.tsx`
- Create: `frontend/src/pages/coupon/AdminCouponListPage.tsx`
- Create: `frontend/src/pages/coupon/index.ts`
- Test: `frontend/tests/adminCouponPageSource.test.mjs`

- [ ] **Step 1: Write the failing source-level admin coupon page test**

```js
test('admin app registers coupon route and api methods', () => {
  assert.match(fs.readFileSync('frontend/src/App.tsx', 'utf8'), /coupon/i);
  assert.match(fs.readFileSync('frontend/src/api/admin.ts', 'utf8'), /getAdminCoupons/);
  assert.match(fs.readFileSync('frontend/src/pages/coupon/AdminCouponListPage.tsx', 'utf8'), /发放优惠券/);
});
```

- [ ] **Step 2: Run test to verify failure**

Run: `node --test frontend/tests/adminCouponPageSource.test.mjs`

Expected: FAIL because the route/page/api do not exist.

- [ ] **Step 3: Add admin coupon API methods**

```ts
export function getAdminCoupons(params: { keyword?: string; status?: number; pageNum?: number; pageSize?: number }) {
  return get<PageResult<AdminCoupon>>('/admin/coupons', params);
}

export function issueAdminCoupon(id: number, data: { userIds: number[]; issueMode: 'manual' | 'batch' }) {
  return post<void>(`/admin/coupons/${id}/issue`, data);
}
```

- [ ] **Step 4: Create list page with create/edit/status/issue actions**

```tsx
<Table
  rowKey="id"
  columns={[
    { title: '券名', dataIndex: 'name' },
    { title: '类型', dataIndex: 'couponTypeText' },
    { title: '剩余库存', dataIndex: 'remainCount' },
    { title: '状态', dataIndex: 'statusText' },
    { title: '操作', render: (_, row) => <IssueButton coupon={row} /> },
  ]}
/>
```

- [ ] **Step 5: Register the protected admin route**

```tsx
<Route path="/coupons" element={<AdminCouponListPage />} />
```

- [ ] **Step 6: Run test**

Run: `node --test frontend/tests/adminCouponPageSource.test.mjs`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/api/admin.ts frontend/src/App.tsx frontend/src/pages/coupon/AdminCouponListPage.tsx frontend/src/pages/coupon/index.ts frontend/tests/adminCouponPageSource.test.mjs
git commit -m "feat: add admin coupon management page"
```

## Task 7: Finish Android Coupon And Share Experience

**Files:**
- Modify: `booking-app/src/services/api/couponApi.ts`
- Modify: `booking-app/src/components/order/CouponSelector.tsx`
- Modify: `booking-app/src/screens/cart/CheckoutScreen.tsx`
- Modify: `booking-app/src/screens/home/HomeScreen.tsx`
- Modify: `booking-app/src/screens/ticket/TicketDetailScreen.tsx`
- Modify: `booking-app/src/screens/order/OrderDetailScreen.tsx`
- Modify: `booking-app/src/screens/profile/ProfileScreen.tsx`
- Create: `booking-app/src/utils/shareTicket.ts`
- Test: `booking-app/tests/mobileCouponShareSource.test.mjs`

- [ ] **Step 1: Write the failing mobile source test**

```js
test('android source wires coupons and share entry points', () => {
  const checkout = fs.readFileSync('booking-app/src/screens/cart/CheckoutScreen.tsx', 'utf8');
  const detail = fs.readFileSync('booking-app/src/screens/ticket/TicketDetailScreen.tsx', 'utf8');
  assert.match(checkout, /couponApi\.getAvailableCoupons/);
  assert.match(detail, /shareTicket/);
});
```

- [ ] **Step 2: Run test to verify failure**

Run: `node --test booking-app/tests/mobileCouponShareSource.test.mjs`

Expected: FAIL because current Android checkout still uses mock coupon data and no share utility exists.

- [ ] **Step 3: Replace mock coupon behavior with real API-backed selection**

```ts
const couponRes = await couponApi.getAvailableCoupons({
  orderAmount: originalAmount,
  ticketId: toApiId(firstItem?.ticketId),
});
setAvailableCoupons(couponRes);
```

```ts
userCouponId: selectedCoupon ? Number(selectedCoupon.id) : undefined,
```

- [ ] **Step 4: Add reusable native share helper**

```ts
import { Share } from 'react-native';

export async function shareTicket(title: string, ticketId: number) {
  const url = `${APP_PUBLIC_ORIGIN}/shop/tickets/${ticketId}`;
  return Share.share({ title, message: `${title} ${url}`, url });
}
```

- [ ] **Step 5: Add share actions on home cards, ticket detail, and order detail**

```tsx
<TouchableOpacity onPress={() => shareTicket(ticket.name, Number(ticket.id))}>
  <Text style={styles.shareText}>分享</Text>
</TouchableOpacity>
```

- [ ] **Step 6: Add “我的优惠券” real navigation and count refresh in profile**

```tsx
const couponSummary = await couponApi.getUserCoupons({ status: 'available', page: 1, pageSize: 20 });
setAvailableCouponCount(couponSummary.total);
```

- [ ] **Step 7: Run tests**

Run: `node --test booking-app/tests/mobileCouponShareSource.test.mjs`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add booking-app/src/services/api/couponApi.ts booking-app/src/components/order/CouponSelector.tsx booking-app/src/screens/cart/CheckoutScreen.tsx booking-app/src/screens/home/HomeScreen.tsx booking-app/src/screens/ticket/TicketDetailScreen.tsx booking-app/src/screens/order/OrderDetailScreen.tsx booking-app/src/screens/profile/ProfileScreen.tsx booking-app/src/utils/shareTicket.ts booking-app/tests/mobileCouponShareSource.test.mjs
git commit -m "feat: add android coupon and share flows"
```

## Task 8: Refresh API Automation And Run Cross-Stack Regression

**Files:**
- Modify: `automation-tests/api-tests/tests/test_coupon.py`
- Modify: `automation-tests/api-tests/tests/test_order.py`
- Create: `automation-tests/api-tests/tests/test_admin_coupon.py`
- Test: `frontend/tests/customerCouponApiSource.test.mjs`
- Test: `frontend/tests/shareDialogSource.test.mjs`
- Test: `frontend/tests/shopCouponShareSource.test.mjs`
- Test: `frontend/tests/adminCouponPageSource.test.mjs`
- Test: `booking-app/tests/mobileCouponShareSource.test.mjs`
- Test: `booking-api/src/test/java/com/ticket/app/service/order/OrderCouponLifecycleTest.java`
- Test: `booking-api/src/test/java/com/ticket/app/service/admin/AdminCouponServiceTest.java`

- [ ] **Step 1: Replace stale API cases with current coupon/order endpoints**

```python
def test_receive_coupon_success(self, auth_client: HttpClient):
    response = auth_client.post("/api/coupons/receive", params={"couponId": 1001})
    assert_response(response, "领取优惠券").success()

def test_get_my_coupons(self, auth_client: HttpClient):
    response = auth_client.get("/api/coupons/list", params={"status": 0, "page": 1, "pageSize": 10})
    assert_response(response, "我的优惠券").success()
```

- [ ] **Step 2: Add admin coupon API tests**

```python
def test_admin_create_coupon(admin_client: HttpClient):
    payload = {
        "name": "暑期满减券",
        "couponType": 1,
        "couponValue": 30,
        "totalCount": 100,
        "remainCount": 100,
        "perLimit": 1,
        "status": 1,
    }
    response = admin_client.post("/api/admin/coupons", json_data=payload)
    assert_response(response, "创建优惠券").success()
```

- [ ] **Step 3: Add order tests covering `userCouponId` and coupon return on cancel**

```python
def test_create_order_with_user_coupon(self, auth_client: HttpClient):
    payload = dict(OrderTestData.CREATE_ORDER_DATA)
    payload["userCouponId"] = 301
    response = auth_client.post("/api/orders", json_data=payload)
    assert_response(response, "创建带券订单").success()
```

- [ ] **Step 4: Run backend Java tests**

Run: `.\mvnw.cmd -pl booking-api -Dtest=OrderCouponLifecycleTest,AdminCouponServiceTest test`

Expected: PASS.

- [ ] **Step 5: Run Web source tests**

Run: `node --test frontend/tests/customerCouponApiSource.test.mjs frontend/tests/shareDialogSource.test.mjs frontend/tests/shopCouponShareSource.test.mjs frontend/tests/adminCouponPageSource.test.mjs`

Expected: PASS.

- [ ] **Step 6: Run Android source tests**

Run: `node --test booking-app/tests/mobileCouponShareSource.test.mjs`

Expected: PASS.

- [ ] **Step 7: Run API automation**

Run: `pytest automation-tests/api-tests/tests/test_coupon.py automation-tests/api-tests/tests/test_order.py automation-tests/api-tests/tests/test_admin_coupon.py -q`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add automation-tests/api-tests/tests/test_coupon.py automation-tests/api-tests/tests/test_order.py automation-tests/api-tests/tests/test_admin_coupon.py
git commit -m "test: cover sharing and coupon regression"
```

## Task 9: Manual Smoke Verification And Delivery Notes

**Files:**
- Modify: `docs/superpowers/plans/2026-06-30-sharing-and-coupons-phase1.md`

- [ ] **Step 1: Start backend**

Run: `.\mvnw.cmd -pl booking-api spring-boot:run`

Expected: backend starts without coupon/order migration errors.

- [ ] **Step 2: Start web frontend**

Run: `npm.cmd --prefix frontend run dev`

Expected: consumer shop and admin console both render with coupon/share controls.

- [ ] **Step 3: Start Android packager**

Run: `npm.cmd --prefix booking-app start`

Expected: Metro starts and coupon/share-enabled screens load in emulator/device.

- [ ] **Step 4: Manual smoke checklist**

```text
1. Admin creates a coupon and issues it to one named user.
2. Consumer opens /shop, claims a public coupon, selects it in checkout, and sees reduced pay amount.
3. Consumer pays the order; order detail shows coupon deduction and coupon becomes used.
4. Create another unpaid order with coupon; wait or force expiration; coupon returns to unused.
5. Open share dialog from shop card, ticket detail, and order detail; verify every target button renders and copy-link works.
6. Android home/ticket/order/profile flows expose the same coupon/share capabilities.
```

- [ ] **Step 5: Final commit if smoke notes changed**

```bash
git add docs/superpowers/plans/2026-06-30-sharing-and-coupons-phase1.md
git commit -m "docs: record sharing and coupon delivery checklist"
```
