import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const source = readFileSync('booking-app/src/screens/order/OrderListScreen.tsx', 'utf8');

assert.match(source, /Alert/);
assert.match(source, /orderApi\.cancelOrder/);
assert.match(source, /取消成功/);
assert.doesNotMatch(source, /取消订单功能开发中/);

console.log('android order cancel source tests passed');
