import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/api/customer.ts', 'utf8');

assert.match(source, /import \{ get, post, put \} from '@\/services\/http';/);
assert.match(source, /export function cancelCustomerOrder\(/);
assert.match(source, /put<void>\(\s*`\/orders\/\$\{id\}\/cancel`/);
assert.match(source, /cancelReason/);
assert.match(source, /Authorization: `Bearer \$\{token\}`/);

console.log('customer API path tests passed');
