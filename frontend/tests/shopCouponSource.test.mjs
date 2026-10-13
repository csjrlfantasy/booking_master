import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /getCouponCenter,/);
assert.match(source, /getMyCoupons,/);
assert.match(source, /getAvailableCoupons,/);
assert.match(source, /receiveCoupon,/);
assert.match(source, /validateCustomerCoupon,/);
assert.match(source, /const \[selectedCouponId, setSelectedCouponId\] = useState<number \| null>\(null\);/);
assert.match(source, /userCouponId: selectedCouponId \?\? undefined,/);
assert.match(source, /CouponCenterSection/);
assert.match(source, /CouponPicker/);
assert.match(source, /优惠券中心/);
assert.match(source, /我的优惠券/);

console.log('shop coupon source tests passed');
