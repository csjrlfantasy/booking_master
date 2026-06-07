import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/order/OrderListPage.tsx', 'utf8');

assert.match(source, /selectedIds/);
assert.match(source, /batchAdminCancelOrders/);
assert.match(source, /batchAdminRefundOrders/);
assert.match(source, /批量取消/);
assert.match(source, /批量退款/);

console.log('admin order list source tests passed');
