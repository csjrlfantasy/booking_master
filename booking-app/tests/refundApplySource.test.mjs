import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const navigatorSource = readFileSync('booking-app/src/navigation/OrderNavigator.tsx', 'utf8');
const refundSource = readFileSync('booking-app/src/screens/order/RefundApplyScreen.tsx', 'utf8');

assert.match(navigatorSource, /RefundApplyScreen/);
assert.doesNotMatch(navigatorSource, /RefundApplyPlaceholder|功能开发中/);

assert.match(refundSource, /orderApi\.getOrderDetail/);
assert.match(refundSource, /orderApi\.refundOrder/);
assert.match(refundSource, /退款规则/);
assert.match(refundSource, /退票原因/);
assert.match(refundSource, /退款申请已提交/);

console.log('android refund apply source tests passed');
