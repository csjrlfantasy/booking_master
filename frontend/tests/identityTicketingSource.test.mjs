import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const customerApi = readFileSync('frontend/src/api/customer.ts', 'utf8');

assert.match(customerApi, /CustomerIdentity/);
assert.match(customerApi, /getCustomerIdentities/);
assert.match(customerApi, /createCustomerIdentity/);
assert.match(customerApi, /updateCustomerIdentity/);
assert.match(customerApi, /deleteCustomerIdentity/);
assert.match(customerApi, /attendees/);
assert.match(customerApi, /identityId/);
assert.match(customerApi, /idCardNo/);

console.log('identity ticketing api source tests passed');
