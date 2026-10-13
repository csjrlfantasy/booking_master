import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

test('mobile checkout uses available coupon api and userCouponId', () => {
  const checkout = fs.readFileSync('booking-app/src/screens/cart/CheckoutScreen.tsx', 'utf8');
  const selector = fs.readFileSync('booking-app/src/components/order/CouponSelector.tsx', 'utf8');

  assert.match(selector, /couponApi\.getAvailableCoupons/);
  assert.match(checkout, /userCouponId:\s*selectedCoupon\?\.id/);
  assert.match(checkout, /CouponSelector/);
});

test('mobile app wires sharing entry points and helper', () => {
  const helper = fs.readFileSync('booking-app/src/utils/shareTicket.ts', 'utf8');
  const home = fs.readFileSync('booking-app/src/screens/home/HomeScreen.tsx', 'utf8');
  const detail = fs.readFileSync('booking-app/src/screens/ticket/TicketDetailScreen.tsx', 'utf8');
  const orderDetail = fs.readFileSync('booking-app/src/screens/order/OrderDetailScreen.tsx', 'utf8');

  assert.match(helper, /Share\.share/);
  assert.match(helper, /\/shop\/tickets\//);
  assert.match(home, /shareTicket/);
  assert.match(detail, /shareTicket/);
  assert.match(orderDetail, /shareTicket/);
});

test('profile coupon screen is real and navigator no longer uses coupon placeholder', () => {
  const navigator = fs.readFileSync('booking-app/src/navigation/ProfileNavigator.tsx', 'utf8');
  const profile = fs.readFileSync('booking-app/src/screens/profile/ProfileScreen.tsx', 'utf8');
  const couponScreen = fs.readFileSync('booking-app/src/screens/profile/CouponScreen.tsx', 'utf8');

  assert.match(navigator, /component=\{CouponScreen\}/);
  assert.match(profile, /couponApi\.getUserCoupons/);
  assert.match(couponScreen, /couponApi\.getCouponCenter/);
  assert.match(couponScreen, /couponApi\.receiveCoupon/);
});
