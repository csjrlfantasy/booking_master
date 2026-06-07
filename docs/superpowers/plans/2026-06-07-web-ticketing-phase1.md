# Web Ticketing Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the first stable Web ticketing slice: customer order operations, ticket detail layout stability, admin ticket/order operations, and regression tests.

**Architecture:** Keep the existing Spring Boot API and React/Vite frontend. Prefer small frontend helpers and focused tests around API routing, order actions, status mapping, and UI state transitions; only add backend endpoints if the frontend cannot call an existing API safely.

**Tech Stack:** Spring Boot 3, MyBatis Plus, Java 17, React 18, Vite, TypeScript, TanStack Query, plain Node test scripts.

---

## Scope

This plan implements the first phase from `docs/superpowers/specs/2026-06-07-ticket-platform-roadmap-design.md`.

Included:
- Web customer order center polish.
- Customer cancel order API wrapper.
- Customer refund and pay action refresh.
- Ticket detail poster/layout stability.
- Admin ticket list filters and batch operations verification.
- Admin order batch action foundation.
- Automated tests for the behavior that can be tested without a browser.

Not included:
- Android API replacement.
- Real WeChat/Alipay gateway integration.
- Complex seat-map selection.
- Full anti-scalper risk model.

## File Map

- Modify: `frontend/src/api/customer.ts`
  - Add `cancelCustomerOrder`.
  - Normalize order query parameter naming.
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
  - Wire cancel order in the customer order center.
  - Refresh order/profile data after pay/refund/cancel.
  - Keep ticket detail image dimensions stable.
  - Keep comments only in ticket detail context.
- Create: `frontend/src/pages/shop/orderRules.ts`
  - Small pure helpers for order status/action availability.
- Test: `frontend/tests/orderRules.test.mjs`
  - Verify order action visibility and status labels.
- Test: `frontend/tests/customerApiPaths.test.mjs`
  - Verify API wrappers build expected customer endpoint paths through a mock transport.
- Modify: `frontend/src/pages/order/OrderListPage.tsx`
  - Add selected-order checkboxes and batch cancel/refund controls.
- Create: `frontend/src/pages/order/adminOrderActions.ts`
  - Pure helper for batch action eligibility and confirmation text.
- Test: `frontend/tests/adminOrderActions.test.mjs`
  - Verify eligible order IDs for cancel/refund.
- Modify: `frontend/src/pages/ticket/TicketListPage.tsx`
  - Preserve status/category/search params together.
  - Add category filter if categories are available through existing admin/customer category API.
- Test: `frontend/tests/ticketFilterState.test.mjs`
  - Verify URL search param merge logic through a pure helper.
- Modify: `frontend/src/api/admin.ts`
  - Add batch helper functions that call existing single-item endpoints with result summaries.
- Test: `frontend/tests/adminBatchActions.test.mjs`
  - Verify batch summaries include successes and failures.

---

## Task 1: Customer Order Rules Helpers

**Files:**
- Create: `frontend/src/pages/shop/orderRules.ts`
- Test: `frontend/tests/orderRules.test.mjs`

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/orderRules.test.mjs`:

```js
import assert from 'node:assert/strict';
import {
  canCancelCustomerOrder,
  canPayCustomerOrder,
  canRefundCustomerOrder,
  customerOrderStatusText,
} from '../src/pages/shop/orderRules.ts';

const pending = { orderStatus: 1, payStatus: 0 };
const paid = { orderStatus: 2, payStatus: 1 };
const cancelled = { orderStatus: 4, payStatus: 0 };
const refunding = { orderStatus: 6, payStatus: 1 };

assert.equal(customerOrderStatusText(pending), '待支付');
assert.equal(customerOrderStatusText(paid), '已支付');
assert.equal(customerOrderStatusText(cancelled), '已取消');
assert.equal(customerOrderStatusText(refunding), '退款中');

assert.equal(canPayCustomerOrder(pending), true);
assert.equal(canPayCustomerOrder(paid), false);
assert.equal(canCancelCustomerOrder(pending), true);
assert.equal(canCancelCustomerOrder(paid), false);
assert.equal(canRefundCustomerOrder(paid), true);
assert.equal(canRefundCustomerOrder(refunding), false);

console.log('orderRules tests passed');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/orderRules.test.mjs
```

Expected: FAIL because `frontend/src/pages/shop/orderRules.ts` does not exist.

- [ ] **Step 3: Write minimal implementation**

Create `frontend/src/pages/shop/orderRules.ts`:

```ts
export type CustomerOrderState = {
  orderStatus?: number;
  payStatus?: number;
  orderStatusText?: string;
};

export function customerOrderStatusText(order: CustomerOrderState) {
  if (order.orderStatusText) return order.orderStatusText;
  if (order.orderStatus === 1 && order.payStatus === 0) return '待支付';
  if (order.orderStatus === 6) return '退款中';
  if (order.orderStatus === 2 || order.payStatus === 1) return '已支付';
  if (order.orderStatus === 4) return '已取消';
  if (order.orderStatus === 5) return '已退款';
  return '处理中';
}

export function canPayCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 1 && order.payStatus === 0;
}

export function canCancelCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 1 && order.payStatus === 0;
}

export function canRefundCustomerOrder(order: CustomerOrderState) {
  return order.orderStatus === 2 && order.payStatus === 1;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
node frontend/tests/orderRules.test.mjs
```

Expected: PASS with `orderRules tests passed`.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/pages/shop/orderRules.ts frontend/tests/orderRules.test.mjs
git commit -m "test: cover customer order rules"
```

---

## Task 2: Customer Cancel Order API

**Files:**
- Modify: `frontend/src/api/customer.ts`
- Test: `frontend/tests/customerApiPaths.test.mjs`

- [ ] **Step 1: Write the failing test**

Create `frontend/tests/customerApiPaths.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/api/customer.ts', 'utf8');

assert.match(source, /export function cancelCustomerOrder\(/);
assert.match(source, /put<void>\(`\/orders\/\$\{id\}\/cancel`/);
assert.match(source, /cancelReason/);

console.log('customer API path tests passed');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/customerApiPaths.test.mjs
```

Expected: FAIL because `cancelCustomerOrder` is not exported.

- [ ] **Step 3: Write minimal implementation**

In `frontend/src/api/customer.ts`, change the import:

```ts
import { get, post, put } from '@/services/http';
```

Add this function after `getCustomerOrderDetail`:

```ts
export function cancelCustomerOrder(token: string, id: number | string, cancelReason = '用户主动取消') {
  return put<void>(
    `/orders/${id}/cancel`,
    { cancelReason },
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```powershell
node frontend/tests/customerApiPaths.test.mjs
```

Expected: PASS with `customer API path tests passed`.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/api/customer.ts frontend/tests/customerApiPaths.test.mjs
git commit -m "feat: add customer cancel order API"
```

---

## Task 3: Wire Customer Order Center Actions

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Modify: `frontend/src/pages/shop/orderRules.ts`
- Test: `frontend/tests/orderRules.test.mjs`

- [ ] **Step 1: Extend failing test for action labels**

Append to `frontend/tests/orderRules.test.mjs`:

```js
import { customerOrderPrimaryAction } from '../src/pages/shop/orderRules.ts';

assert.equal(customerOrderPrimaryAction(pending), '去支付');
assert.equal(customerOrderPrimaryAction(paid), '申请退款');
assert.equal(customerOrderPrimaryAction(cancelled), null);
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/orderRules.test.mjs
```

Expected: FAIL because `customerOrderPrimaryAction` is not exported.

- [ ] **Step 3: Implement helper**

Add to `frontend/src/pages/shop/orderRules.ts`:

```ts
export function customerOrderPrimaryAction(order: CustomerOrderState) {
  if (canPayCustomerOrder(order)) return '去支付';
  if (canRefundCustomerOrder(order)) return '申请退款';
  return null;
}
```

- [ ] **Step 4: Wire ShopPage imports and mutations**

In `frontend/src/pages/shop/ShopPage.tsx`:

```ts
import {
  canCancelCustomerOrder,
  canPayCustomerOrder,
  canRefundCustomerOrder,
  customerOrderStatusText,
} from './orderRules';
```

Add `cancelCustomerOrder` to the existing customer API import list.

Inside the order center component, add a cancel mutation:

```ts
const cancelMutation = useMutation({
  mutationFn: (order: CustomerOrderListItem) => cancelCustomerOrder(customerToken, order.id),
  onSuccess: () => {
    toast.success('订单已取消');
    onRefresh();
  },
  onError: (err: unknown) => {
    const error = err as { message?: string };
    toast.error(error.message || '取消订单失败');
  },
});
```

Replace inline status/action checks:

```tsx
{canPayCustomerOrder(selectedOrder) ? (
  <button type="button" onClick={() => onPayAgain(selectedOrder)} className="btn-primary">
    去支付
  </button>
) : null}
{canCancelCustomerOrder(selectedOrder) ? (
  <button type="button" disabled={cancelMutation.isPending} onClick={() => cancelMutation.mutate(selectedOrder)} className="btn-secondary">
    取消订单
  </button>
) : null}
{canRefundCustomerOrder(selectedOrder) ? (
  <button type="button" disabled={refundMutation.isPending} onClick={() => refundMutation.mutate()} className="btn-secondary">
    申请退款
  </button>
) : null}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
node frontend/tests/orderRules.test.mjs
npm.cmd run build
```

Expected: test passes and frontend build exits 0.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/pages/shop/ShopPage.tsx frontend/src/pages/shop/orderRules.ts frontend/tests/orderRules.test.mjs
git commit -m "feat: wire customer order actions"
```

---

## Task 4: Stable Ticket Detail Media Layout

**Files:**
- Modify: `frontend/src/pages/shop/ShopPage.tsx`
- Test: `frontend/tests/shopLayoutSource.test.mjs`

- [ ] **Step 1: Write source-level failing test**

Create `frontend/tests/shopLayoutSource.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /aspect-\[4\/5\]/);
assert.match(source, /object-cover/);
assert.match(source, /items-start/);
assert.doesNotMatch(source, /items-stretch/);

console.log('shop layout source tests passed');
```

- [ ] **Step 2: Run test to verify current behavior**

Run:

```powershell
node frontend/tests/shopLayoutSource.test.mjs
```

Expected: FAIL if detail layout uses stretch behavior or lacks stable poster aspect ratio in the detail section.

- [ ] **Step 3: Implement stable layout**

In the ticket detail layout container, use:

```tsx
<section className="grid items-start gap-6 lg:grid-cols-[minmax(260px,360px)_1fr]">
  <div className="overflow-hidden rounded-lg bg-gray-100">
    <div className="aspect-[4/5]">
      {detail.poster ? (
        <img src={detail.poster} alt={detail.name} className="h-full w-full object-cover" />
      ) : (
        <div className="grid h-full w-full place-items-center text-rose-500">
          <Ticket className="h-12 w-12" />
        </div>
      )}
    </div>
  </div>
  <div className="min-w-0 space-y-4">
    {/* existing title, metadata, description, purchase controls */}
  </div>
</section>
```

- [ ] **Step 4: Run test and build**

Run:

```powershell
node frontend/tests/shopLayoutSource.test.mjs
npm.cmd run build
```

Expected: source test passes and frontend build exits 0.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/pages/shop/ShopPage.tsx frontend/tests/shopLayoutSource.test.mjs
git commit -m "fix: keep ticket detail poster stable"
```

---

## Task 5: Admin Batch Action Helpers

**Files:**
- Modify: `frontend/src/api/admin.ts`
- Create: `frontend/src/pages/order/adminOrderActions.ts`
- Test: `frontend/tests/adminOrderActions.test.mjs`
- Test: `frontend/tests/adminBatchActions.test.mjs`

- [ ] **Step 1: Write failing helper test**

Create `frontend/tests/adminOrderActions.test.mjs`:

```js
import assert from 'node:assert/strict';
import {
  getBatchCancelableOrderIds,
  getBatchRefundableOrderIds,
  getBatchActionConfirmText,
} from '../src/pages/order/adminOrderActions.ts';

const orders = [
  { id: 1, orderStatus: 1, orderNo: 'A' },
  { id: 2, orderStatus: 2, orderNo: 'B' },
  { id: 3, orderStatus: 4, orderNo: 'C' },
  { id: 4, orderStatus: 6, orderNo: 'D' },
];

assert.deepEqual(getBatchCancelableOrderIds(orders), [1]);
assert.deepEqual(getBatchRefundableOrderIds(orders), [2, 4]);
assert.equal(getBatchActionConfirmText('cancel', 2), '确认批量取消 2 个订单吗？');
assert.equal(getBatchActionConfirmText('refund', 3), '确认批量全额退款 3 个订单吗？');

console.log('admin order action tests passed');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/adminOrderActions.test.mjs
```

Expected: FAIL because helper file does not exist.

- [ ] **Step 3: Implement helper**

Create `frontend/src/pages/order/adminOrderActions.ts`:

```ts
export type AdminOrderActionItem = {
  id: number;
  orderStatus: number;
  orderNo?: string;
};

export type BatchOrderAction = 'cancel' | 'refund';

export function getBatchCancelableOrderIds(orders: AdminOrderActionItem[]) {
  return orders.filter((order) => order.orderStatus === 1).map((order) => order.id);
}

export function getBatchRefundableOrderIds(orders: AdminOrderActionItem[]) {
  return orders.filter((order) => order.orderStatus === 2 || order.orderStatus === 6).map((order) => order.id);
}

export function getBatchActionConfirmText(action: BatchOrderAction, count: number) {
  if (action === 'cancel') return `确认批量取消 ${count} 个订单吗？`;
  return `确认批量全额退款 ${count} 个订单吗？`;
}
```

- [ ] **Step 4: Add admin API batch test**

Create `frontend/tests/adminBatchActions.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/api/admin.ts', 'utf8');

assert.match(source, /export async function batchUpdateAdminTicketStatus/);
assert.match(source, /export async function batchAdminCancelOrders/);
assert.match(source, /export async function batchAdminRefundOrders/);
assert.match(source, /successIds/);
assert.match(source, /failed/);

console.log('admin batch action source tests passed');
```

- [ ] **Step 5: Run test to verify it fails**

Run:

```powershell
node frontend/tests/adminBatchActions.test.mjs
```

Expected: FAIL until batch helper functions are added.

- [ ] **Step 6: Implement admin API batch helpers**

In `frontend/src/api/admin.ts`, add:

```ts
export interface BatchActionResult {
  successIds: number[];
  failed: Array<{ id: number; message: string }>;
}

async function runBatchAction(ids: number[], action: (id: number) => Promise<void>): Promise<BatchActionResult> {
  const result: BatchActionResult = { successIds: [], failed: [] };
  for (const id of ids) {
    try {
      await action(id);
      result.successIds.push(id);
    } catch (err) {
      const error = err as { message?: string };
      result.failed.push({ id, message: error.message || '操作失败' });
    }
  }
  return result;
}

export function batchUpdateAdminTicketStatus(ids: number[], status: number) {
  return runBatchAction(ids, (id) => updateAdminTicketStatus(id, status));
}

export function batchAdminCancelOrders(ids: number[]) {
  return runBatchAction(ids, (id) => adminCancelOrder(id));
}

export function batchAdminRefundOrders(ids: number[]) {
  return runBatchAction(ids, (id) => adminRefundOrder(id, {}));
}
```

- [ ] **Step 7: Run tests**

Run:

```powershell
node frontend/tests/adminOrderActions.test.mjs
node frontend/tests/adminBatchActions.test.mjs
npm.cmd run build
```

Expected: both tests pass and frontend build exits 0.

- [ ] **Step 8: Commit**

```powershell
git add frontend/src/api/admin.ts frontend/src/pages/order/adminOrderActions.ts frontend/tests/adminOrderActions.test.mjs frontend/tests/adminBatchActions.test.mjs
git commit -m "feat: add admin batch action helpers"
```

---

## Task 6: Wire Admin Order Batch UI

**Files:**
- Modify: `frontend/src/pages/order/OrderListPage.tsx`
- Test: `frontend/tests/adminOrderListSource.test.mjs`

- [ ] **Step 1: Write source-level failing test**

Create `frontend/tests/adminOrderListSource.test.mjs`:

```js
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/order/OrderListPage.tsx', 'utf8');

assert.match(source, /selectedIds/);
assert.match(source, /batchAdminCancelOrders/);
assert.match(source, /batchAdminRefundOrders/);
assert.match(source, /批量取消/);
assert.match(source, /批量退款/);

console.log('admin order list source tests passed');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/adminOrderListSource.test.mjs
```

Expected: FAIL until batch UI is wired.

- [ ] **Step 3: Implement selected IDs state**

In `frontend/src/pages/order/OrderListPage.tsx`, import:

```ts
import { batchAdminCancelOrders, batchAdminRefundOrders } from '@/api/admin';
import {
  getBatchActionConfirmText,
  getBatchCancelableOrderIds,
  getBatchRefundableOrderIds,
} from './adminOrderActions';
```

Add state:

```ts
const [selectedIds, setSelectedIds] = useState<number[]>([]);
const currentOrders = data?.list ?? [];
const currentIds = currentOrders.map((order) => order.id);
const selectedOrders = currentOrders.filter((order) => selectedIds.includes(order.id));
const allCurrentSelected = currentIds.length > 0 && currentIds.every((id) => selectedIds.includes(id));
```

Add handlers:

```ts
const toggleSelected = (id: number) => {
  setSelectedIds((current) => (current.includes(id) ? current.filter((item) => item !== id) : [...current, id]));
};

const toggleCurrentPage = () => {
  setSelectedIds((current) => {
    if (allCurrentSelected) return current.filter((id) => !currentIds.includes(id));
    return Array.from(new Set([...current, ...currentIds]));
  });
};
```

- [ ] **Step 4: Implement batch handlers**

Add:

```ts
const handleBatchCancel = async () => {
  const ids = getBatchCancelableOrderIds(selectedOrders);
  if (ids.length === 0) {
    toast.error('请选择待支付订单');
    return;
  }
  if (!confirm(getBatchActionConfirmText('cancel', ids.length))) return;
  const result = await batchAdminCancelOrders(ids);
  toast.success(`批量取消完成：成功 ${result.successIds.length} 个，失败 ${result.failed.length} 个`);
  setSelectedIds([]);
  refetch();
};

const handleBatchRefund = async () => {
  const ids = getBatchRefundableOrderIds(selectedOrders);
  if (ids.length === 0) {
    toast.error('请选择已支付或退款中订单');
    return;
  }
  if (!confirm(getBatchActionConfirmText('refund', ids.length))) return;
  const result = await batchAdminRefundOrders(ids);
  toast.success(`批量退款完成：成功 ${result.successIds.length} 个，失败 ${result.failed.length} 个`);
  setSelectedIds([]);
  refetch();
};
```

- [ ] **Step 5: Add UI controls**

Above the order table, render:

```tsx
{selectedIds.length > 0 && (
  <div className="flex flex-wrap items-center justify-between gap-3 border-b border-gray-200 bg-primary-50 px-4 py-3">
    <span className="text-sm font-medium text-primary-700">已选择 {selectedIds.length} 个订单</span>
    <div className="flex flex-wrap gap-2">
      <button type="button" onClick={handleBatchCancel} className="btn-secondary">
        批量取消
      </button>
      <button type="button" onClick={handleBatchRefund} className="btn-secondary text-danger-600">
        批量退款
      </button>
    </div>
  </div>
)}
```

Add a checkbox column to table header/body:

```tsx
<th>
  <input type="checkbox" checked={allCurrentSelected} onChange={toggleCurrentPage} />
</th>
```

```tsx
<td>
  <input type="checkbox" checked={selectedIds.includes(order.id)} onChange={() => toggleSelected(order.id)} />
</td>
```

- [ ] **Step 6: Run test and build**

Run:

```powershell
node frontend/tests/adminOrderListSource.test.mjs
npm.cmd run build
```

Expected: source test passes and frontend build exits 0.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/pages/order/OrderListPage.tsx frontend/tests/adminOrderListSource.test.mjs
git commit -m "feat: add admin order batch controls"
```

---

## Task 7: Ticket Filter State Helper

**Files:**
- Create: `frontend/src/pages/ticket/ticketFilterState.ts`
- Modify: `frontend/src/pages/ticket/TicketListPage.tsx`
- Test: `frontend/tests/ticketFilterState.test.mjs`

- [ ] **Step 1: Write failing test**

Create `frontend/tests/ticketFilterState.test.mjs`:

```js
import assert from 'node:assert/strict';
import { buildTicketFilterParams } from '../src/pages/ticket/ticketFilterState.ts';

assert.deepEqual(
  buildTicketFilterParams({ keyword: '演唱会', status: 2, categoryId: 1, page: 3 }),
  { keyword: '演唱会', status: '2', categoryId: '1', page: '3' }
);

assert.deepEqual(
  buildTicketFilterParams({ keyword: '', status: undefined, categoryId: undefined, page: 1 }),
  { page: '1' }
);

console.log('ticket filter state tests passed');
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
node frontend/tests/ticketFilterState.test.mjs
```

Expected: FAIL because helper does not exist.

- [ ] **Step 3: Implement helper**

Create `frontend/src/pages/ticket/ticketFilterState.ts`:

```ts
export type TicketFilterState = {
  keyword?: string;
  status?: number;
  categoryId?: number;
  page: number;
};

export function buildTicketFilterParams(state: TicketFilterState) {
  const params: Record<string, string> = { page: String(state.page) };
  if (state.keyword?.trim()) params.keyword = state.keyword.trim();
  if (state.status != null) params.status = String(state.status);
  if (state.categoryId != null) params.categoryId = String(state.categoryId);
  return params;
}
```

- [ ] **Step 4: Wire TicketListPage**

In `frontend/src/pages/ticket/TicketListPage.tsx`, import:

```ts
import { buildTicketFilterParams } from './ticketFilterState';
```

Use it when setting URL params:

```ts
setSearchParams(buildTicketFilterParams({ keyword: search, status, page: 1 }));
```

and:

```ts
setSearchParams(buildTicketFilterParams({ keyword: search, status, page: newPage }));
```

If category filter is added in the same pass, include `categoryId`.

- [ ] **Step 5: Run tests and build**

Run:

```powershell
node frontend/tests/ticketFilterState.test.mjs
npm.cmd run build
```

Expected: helper test passes and frontend build exits 0.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/pages/ticket/ticketFilterState.ts frontend/src/pages/ticket/TicketListPage.tsx frontend/tests/ticketFilterState.test.mjs
git commit -m "fix: preserve ticket filter state"
```

---

## Task 8: Regression Verification

**Files:**
- No code files unless fixes are required.
- Verify: frontend tests, frontend build, backend smoke API.

- [ ] **Step 1: Run frontend helper tests**

Run:

```powershell
node frontend/tests/orderRules.test.mjs
node frontend/tests/customerApiPaths.test.mjs
node frontend/tests/shopLayoutSource.test.mjs
node frontend/tests/adminOrderActions.test.mjs
node frontend/tests/adminBatchActions.test.mjs
node frontend/tests/adminOrderListSource.test.mjs
node frontend/tests/ticketFilterState.test.mjs
node frontend/tests/authRouting.test.mjs
node frontend/tests/customerSession.test.mjs
node frontend/tests/formatMoney.test.mjs
```

Expected: every script exits 0.

- [ ] **Step 2: Run frontend build**

Run:

```powershell
npm.cmd run build
```

Expected: TypeScript and Vite build exit 0.

- [ ] **Step 3: Run backend smoke endpoints**

Run:

```powershell
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8088/api/tickets/upcoming?limit=1' -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:8088/api/tickets/flash-sale?limit=2' -TimeoutSec 10
Invoke-WebRequest -UseBasicParsing -Uri 'http://127.0.0.1:3000/shop' -TimeoutSec 10
```

Expected: all return HTTP 200.

- [ ] **Step 4: Manual browser smoke**

Open:

```text
http://127.0.0.1:3000/shop
http://127.0.0.1:3000/login
```

Expected:
- Customer page does not redirect to admin login.
- Ticket cards open detail context.
- Order tab prompts login if logged out.
- Admin login page loads separately.

- [ ] **Step 5: Commit verification-only fixes if needed**

If verification required fixes, commit them:

```powershell
git add <changed-files>
git commit -m "fix: stabilize web ticketing phase one"
```

---

## Self-Review

Spec coverage:
- Web customer order center: Tasks 1, 2, 3.
- Ticket detail image stability: Task 4.
- Admin order batch controls: Tasks 5, 6.
- Admin ticket filter state: Task 7.
- Automated regression: Task 8.

Known gaps intentionally deferred:
- Android real API replacement belongs to a separate plan.
- Anti-scalper rules belong to a separate plan.
- Full homepage module configuration belongs to a separate plan.
- Real payment gateway integration is out of scope.

Placeholder scan:
- This plan contains no deferred implementation placeholders.
- Each code-changing task includes target files, concrete snippets, commands, and expected result.

Type consistency:
- Customer order state helpers use `orderStatus`, `payStatus`, and `orderStatusText`, matching `CustomerOrderListItem`.
- Admin order helper uses `id`, `orderStatus`, and `orderNo`, matching admin `Order` list fields used by `OrderListPage`.
- Batch API result type is shared by ticket and order batch helpers.
