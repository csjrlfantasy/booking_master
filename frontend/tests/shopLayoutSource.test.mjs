import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /aspect-\[4\/5\]/);
assert.match(source, /object-cover/);
assert.match(source, /items-start/);
assert.match(source, /lg:grid-cols-\[minmax\(260px,360px\)_1fr\]/);
assert.match(source, /min-w-0/);
assert.doesNotMatch(source, /items-stretch/);

console.log('shop layout source tests passed');
