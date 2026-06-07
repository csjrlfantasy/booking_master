import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { transform } from 'esbuild';

const source = await readFile(new URL('../src/pages/order/adminOrderActions.ts', import.meta.url), 'utf8');
const { code } = await transform(source, {
  loader: 'ts',
  format: 'esm',
});

const moduleUrl = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
const {
  getBatchActionConfirmText,
  getBatchCancelableOrderIds,
  getBatchRefundableOrderIds,
} = await import(moduleUrl);

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
