import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const apiSource = readFileSync('frontend/src/api/admin.ts', 'utf8');
const appSource = readFileSync('frontend/src/App.tsx', 'utf8');
const sidebarSource = readFileSync('frontend/src/components/layout/Sidebar.tsx', 'utf8');

assert.match(apiSource, /export interface AdminCoupon/);
assert.match(apiSource, /export interface AdminCouponRequest/);
assert.match(apiSource, /export async function getAdminCoupons\(/);
assert.match(apiSource, /export async function getAdminCouponDetail\(/);
assert.match(apiSource, /export async function createAdminCoupon\(/);
assert.match(apiSource, /export async function updateAdminCoupon\(/);
assert.match(apiSource, /export async function updateAdminCouponStatus\(/);
assert.match(apiSource, /export async function issueAdminCoupon\(/);

assert.match(appSource, /AdminCouponListPage/);
assert.match(appSource, /path="coupons"/);

assert.match(sidebarSource, /id: 'coupons'/);
assert.match(sidebarSource, /path: '\/coupons'/);

const pageSource = readFileSync('frontend/src/pages/coupon/AdminCouponListPage.tsx', 'utf8');
assert.match(pageSource, /优惠券管理/);
assert.match(pageSource, /createAdminCoupon/);
assert.match(pageSource, /getAdminCouponDetail/);
assert.match(pageSource, /updateAdminCoupon/);
assert.match(pageSource, /issueAdminCoupon/);
assert.match(pageSource, /updateAdminCouponStatus/);
assert.match(pageSource, /定向发券/);
assert.match(pageSource, /用户ID/);

console.log('admin coupon page source tests passed');
