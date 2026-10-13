# Commerce Closeout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish the remaining customer commerce flows across web, admin, and Android so coupons, sharing, order payment, refund, and order management run on real data instead of partial mock fallbacks.

**Architecture:** Keep the existing backend contracts and close the gaps closest to the user journey first: web customer flow, then admin management gaps, then Android parity. Reuse existing controllers/services/pages where possible, add focused tests around each repaired workflow, and avoid broad refactors outside the touched modules.

**Tech Stack:** Spring Boot + MyBatis, React + React Router + TanStack Query, React Native + TypeScript, Node source tests, Maven JUnit tests

---

### Task 1: Baseline the Web Customer Commerce Chain

**Files:**
- Modify: `D:\AIProjects\BookingApi\frontend\src\pages\shop\ShopPage.tsx`
- Modify: `D:\AIProjects\BookingApi\frontend\src\api\customer.ts`
- Test: `D:\AIProjects\BookingApi\frontend\tests\shopCouponSource.test.mjs`
- Test: `D:\AIProjects\BookingApi\frontend\tests\shopShareSource.test.mjs`

- [ ] **Step 1: Write or extend failing source checks for the live web chain**

```js
import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /createCustomerOrder/);
assert.match(source, /mockPayOrder/);
assert.match(source, /applyRefund/);
assert.match(source, /receiveCoupon/);
assert.match(source, /navigator\.clipboard\.writeText|window\.location\.href/);
```

- [ ] **Step 2: Run tests to verify the current baseline**

Run: `node --test frontend/tests/shopCouponSource.test.mjs frontend/tests/shopShareSource.test.mjs`  
Expected: pass if wiring exists, or fail with the missing callsite that still needs repair.

- [ ] **Step 3: Repair the page flow with minimal focused changes**

```ts
const submitOrder = async () => {
  const order = await createCustomerOrder(customerToken, payload);
  setLatestOrder(order);
  setPaidResult(null);
};

const submitPayment = async (channel: 'wechat' | 'alipay') => {
  const result = await mockPayOrder(customerToken, {
    orderNo: latestOrder.orderNo,
    amount: Number(latestOrder.payAmount),
    payChannel: channel,
  });
  setPaidResult(result);
};

const submitRefund = async () => {
  await applyRefund(customerToken, {
    orderNo: selectedOrder.orderNo,
    refundAmount: selectedOrder.payAmount,
    reason: '客户自助申请退款',
  });
};
```

- [ ] **Step 4: Re-run the source checks and frontend build**

Run: `node --test frontend/tests/shopCouponSource.test.mjs frontend/tests/shopShareSource.test.mjs`  
Expected: PASS

Run: `npm --prefix frontend run build`  
Expected: `build` succeeds without type or bundling errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/shop/ShopPage.tsx frontend/src/api/customer.ts frontend/tests/shopCouponSource.test.mjs frontend/tests/shopShareSource.test.mjs
git commit -m "fix: complete web commerce flow wiring"
```

### Task 2: Verify Coupon Lifecycle and Refund Consistency in the Backend

**Files:**
- Modify: `D:\AIProjects\BookingApi\booking-api\src\main\java\com\ticket\app\service\payment\PaymentServiceImpl.java`
- Modify: `D:\AIProjects\BookingApi\booking-api\src\main\java\com\ticket\app\service\order\OrderServiceImpl.java`
- Test: `D:\AIProjects\BookingApi\booking-api\src\test\java\com\ticket\app\service\payment\PaymentRefundRuleTest.java`
- Test: `D:\AIProjects\BookingApi\booking-api\src\test\java\com\ticket\app\service\payment\PaymentServiceMockPayTest.java`

- [ ] **Step 1: Write or update failing backend tests around coupon return and payment state**

```java
@Test
void userRefundShouldReturnBoundCouponWhenOrderUsedCoupon() {
    paymentService.refundOrder(userId, request);
    verify(couponService).returnCoupon(userId, orderId, true);
}

@Test
void mockPayShouldMarkPaidOrderAndPreserveWalletChannel() {
    PaymentResultResponse result = paymentService.mockPay(userId, request);
    assertThat(result.getPayChannel()).isEqualTo("wechat");
}
```

- [ ] **Step 2: Run the targeted Maven tests**

Run: `mvn -pl booking-api "-Dtest=PaymentRefundRuleTest,PaymentServiceMockPayTest,OrderCouponLifecycleTest" test`  
Expected: at least one failing assertion if coupon lifecycle still drifts.

- [ ] **Step 3: Keep refund and mock-pay behavior aligned with order state changes**

```java
if (order.getUserCouponId() != null) {
    couponService.returnCoupon(order.getUserId(), order.getId(), true);
}

order.setPayStatus(1);
order.setOrderStatus(2);
order.setPayChannel(request.getPayChannel());
orderRepository.updateById(order);
```

- [ ] **Step 4: Re-run the targeted Maven tests**

Run: `mvn -pl booking-api "-Dtest=PaymentRefundRuleTest,PaymentServiceMockPayTest,OrderCouponLifecycleTest" test`  
Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add booking-api/src/main/java/com/ticket/app/service/payment/PaymentServiceImpl.java booking-api/src/main/java/com/ticket/app/service/order/OrderServiceImpl.java booking-api/src/test/java/com/ticket/app/service/payment/PaymentRefundRuleTest.java booking-api/src/test/java/com/ticket/app/service/payment/PaymentServiceMockPayTest.java
git commit -m "fix: align coupon lifecycle with payment and refund flow"
```

### Task 3: Fill Admin Order and User Management Gaps

**Files:**
- Modify: `D:\AIProjects\BookingApi\booking-api\src\main\java\com\ticket\app\controller\admin\AdminOrderController.java`
- Modify: `D:\AIProjects\BookingApi\booking-api\src\main\java\com\ticket\app\controller\admin\AdminUserController.java`
- Modify: `D:\AIProjects\BookingApi\frontend\src\api\admin.ts`
- Modify: `D:\AIProjects\BookingApi\frontend\src\App.tsx`
- Test: `D:\AIProjects\BookingApi\booking-api\src\test\java\com\ticket\app\controller\admin\AdminCouponControllerTest.java`

- [ ] **Step 1: Add failing coverage for any admin endpoint that still has TODO behavior**

```java
@Test
void updateOrderStatusShouldInvokeAdminService() throws Exception {
    mockMvc.perform(put("/api/admin/orders/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":2}"))
        .andExpect(status().isOk());
}
```

- [ ] **Step 2: Run the targeted controller tests**

Run: `mvn -pl booking-api "-Dtest=Admin*ControllerTest" test`  
Expected: fail on any unimplemented admin endpoint touched by the request path.

- [ ] **Step 3: Replace TODO handlers with actual service calls and wire the frontend client**

```java
@PutMapping("/orders/{id}/status")
public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody AdminOrderStatusRequest request) {
    adminOrderService.updateOrderStatus(id, request);
    return Result.success();
}
```

```ts
export async function updateAdminOrderStatus(id: number, payload: {status: number}) {
  return request.put(`/admin/orders/${id}/status`, payload);
}
```

- [ ] **Step 4: Re-run backend tests and frontend build**

Run: `mvn -pl booking-api "-Dtest=Admin*ControllerTest" test`  
Expected: `BUILD SUCCESS`

Run: `npm --prefix frontend run build`  
Expected: `build` succeeds.

- [ ] **Step 5: Commit**

```bash
git add booking-api/src/main/java/com/ticket/app/controller/admin/AdminOrderController.java booking-api/src/main/java/com/ticket/app/controller/admin/AdminUserController.java frontend/src/api/admin.ts frontend/src/App.tsx
git commit -m "feat: finish admin order and user management actions"
```

### Task 4: Remove Remaining Android Mock Order Data

**Files:**
- Modify: `D:\AIProjects\BookingApi\booking-app\src\screens\cart\CheckoutScreen.tsx`
- Modify: `D:\AIProjects\BookingApi\booking-app\src\screens\order\OrderListScreen.tsx`
- Modify: `D:\AIProjects\BookingApi\booking-app\src\screens\order\OrderDetailScreen.tsx`
- Modify: `D:\AIProjects\BookingApi\booking-app\src\screens\payment\PaymentScreen.tsx`
- Test: `D:\AIProjects\BookingApi\booking-app\tests\checkoutIdentitySource.test.mjs`
- Test: `D:\AIProjects\BookingApi\booking-app\tests\orderListCancelSource.test.mjs`
- Test: `D:\AIProjects\BookingApi\booking-app\tests\mobileCouponShareSource.test.mjs`

- [ ] **Step 1: Write or extend source tests that forbid mock order fallback in live screens**

```js
import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';

const source = readFileSync('booking-app/src/screens/order/OrderListScreen.tsx', 'utf8');
assert.doesNotMatch(source, /setOrders\\(mockOrders\\)/);
assert.match(source, /orderApi\\.getOrderList/);
assert.match(source, /orderApi\\.cancelOrder/);
```

- [ ] **Step 2: Run the mobile source tests**

Run: `node --test booking-app/tests/checkoutIdentitySource.test.mjs booking-app/tests/orderListCancelSource.test.mjs booking-app/tests/mobileCouponShareSource.test.mjs`  
Expected: fail anywhere the old mock fallback still leaks into the live order path.

- [ ] **Step 3: Replace mock lists and mock detail state with API-backed empty-state handling**

```ts
try {
  const res = await orderApi.getOrderList({status: statusParam, page: currentPage, pageSize: 10});
  setOrders(reset ? res.list : [...ordersRef.current, ...res.list]);
  setHasMore(res.hasMore);
} catch (error) {
  toast.error('加载订单失败');
  if (reset) {
    setOrders([]);
  }
}
```

- [ ] **Step 4: Re-run TypeScript, source tests, and Android debug build**

Run: `npx --prefix booking-app tsc -p booking-app/tsconfig.json --noEmit`  
Expected: PASS

Run: `node --test booking-app/tests/checkoutIdentitySource.test.mjs booking-app/tests/orderListCancelSource.test.mjs booking-app/tests/mobileCouponShareSource.test.mjs booking-app/tests/mobileStorageSource.test.mjs`  
Expected: PASS

Run: `gradle.bat -p booking-app/android assembleDebug`  
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add booking-app/src/screens/cart/CheckoutScreen.tsx booking-app/src/screens/order/OrderListScreen.tsx booking-app/src/screens/order/OrderDetailScreen.tsx booking-app/src/screens/payment/PaymentScreen.tsx booking-app/tests/checkoutIdentitySource.test.mjs booking-app/tests/orderListCancelSource.test.mjs booking-app/tests/mobileCouponShareSource.test.mjs booking-app/tests/mobileStorageSource.test.mjs
git commit -m "fix: remove android mock order fallback"
```

### Task 5: Final Verification Sweep

**Files:**
- Modify: `D:\AIProjects\BookingApi\BUG_FIX_REPORT.md`

- [ ] **Step 1: Run the targeted backend, frontend, and mobile verification commands**

```bash
mvn -pl booking-api "-Dtest=PaymentRefundRuleTest,PaymentServiceMockPayTest,OrderCouponLifecycleTest,Admin*ControllerTest" test
npm --prefix frontend run build
node --test frontend/tests/shopCouponSource.test.mjs frontend/tests/shopShareSource.test.mjs
npx --prefix booking-app tsc -p booking-app/tsconfig.json --noEmit
node --test booking-app/tests/checkoutIdentitySource.test.mjs booking-app/tests/orderListCancelSource.test.mjs booking-app/tests/mobileCouponShareSource.test.mjs booking-app/tests/mobileStorageSource.test.mjs
gradle.bat -p booking-app/android assembleDebug
```

- [ ] **Step 2: Record the repaired scope and any explicit residual risk**

```md
## 2026-07-09 Commerce Closeout
- Web: coupon receive, order submit, mock pay, refund, share verified
- Admin: order/user action endpoints completed
- Android: order list/detail/payment no longer fall back to mock data
- Residual risk: full manual payment UX still depends on local API environment
```

- [ ] **Step 3: Commit**

```bash
git add BUG_FIX_REPORT.md
git commit -m "docs: record commerce closeout verification"
```

