import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const root = path.resolve(path.dirname(__filename), '..');

const orderTypes = fs.readFileSync(path.join(root, 'src/types/order.ts'), 'utf8');
assert.match(orderTypes, /attendees\??:\s*CreateOrderAttendee\[\]/);
assert.match(orderTypes, /export interface CreateOrderAttendee/);
assert.match(orderTypes, /identityId\??:\s*number/);

const checkout = fs.readFileSync(path.join(root, 'src/screens/cart/CheckoutScreen.tsx'), 'utf8');
assert.match(checkout, /userApi\.getIdentities/);
assert.match(checkout, /selectedIdentityIds/);
assert.match(checkout, /selectedIdentityIds\.length !== totalQuantity/);
assert.match(checkout, /attendees:\s*itemIdentityIds\.map/);
assert.match(checkout, /identityId:\s*Number\(identityId\)/);
assert.match(checkout, /常用观演人/);

console.log('checkout identity source tests passed');
