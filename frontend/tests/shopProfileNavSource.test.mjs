import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /function ProfileView/);
assert.match(source, /activeTab === 'me'/);
assert.match(source, /setActiveTab\('me'\)/);
assert.doesNotMatch(source, /onMe=\{\(\) => \(customerToken \? setActiveTab\('orders'\) : goLogin\(\)\)\}/);
assert.match(source, /微信余额/);
assert.match(source, /支付宝余额/);

console.log('shop profile nav source tests passed');
