import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /getCustomerIdentities/);
assert.match(source, /createCustomerIdentity/);
assert.match(source, /deleteCustomerIdentity/);
assert.match(source, /常用观演人/);
assert.match(source, /身份证/);
assert.match(source, /selectedIdentityIds/);
assert.match(source, /attendees/);
assert.match(source, /identityId/);

console.log('shop identity selection source tests passed');
