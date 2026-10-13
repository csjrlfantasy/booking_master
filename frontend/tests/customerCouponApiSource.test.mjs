import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/api/customer.ts', 'utf8');

assert.match(source, /export interface CouponCenterItem/);
assert.match(source, /export interface UserCouponItem/);
assert.match(source, /export function getCouponCenter\(/);
assert.match(source, /\/coupons\/center/);
assert.match(source, /export function getMyCoupons\(/);
assert.match(source, /\/coupons\/list/);
assert.match(source, /export function receiveCoupon\(/);
assert.match(source, /\/coupons\/receive/);
assert.match(source, /export function getAvailableCoupons\(/);
assert.match(source, /get<UserCouponItem\[]>\(/);
assert.match(source, /export function validateCustomerCoupon\(/);
assert.match(source, /\/coupons\/validate/);

console.log('customer coupon API source tests passed');
