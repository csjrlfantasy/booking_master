import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { transform } from 'esbuild';

const source = await readFile(new URL('../src/pages/shop/orderRules.ts', import.meta.url), 'utf8');
const { code } = await transform(source, {
  loader: 'ts',
  format: 'esm',
});

const moduleUrl = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
const {
  canCancelCustomerOrder,
  canPayCustomerOrder,
  canRefundCustomerOrder,
  customerOrderPrimaryAction,
  customerOrderStatusText,
} = await import(moduleUrl);

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

assert.equal(customerOrderPrimaryAction(pending), '去支付');
assert.equal(customerOrderPrimaryAction(paid), '申请退款');
assert.equal(customerOrderPrimaryAction(cancelled), null);

console.log('orderRules tests passed');
