import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/ticket/TicketListPage.tsx', 'utf8');

assert.match(source, /batchUpdateAdminTicketStatus/);
assert.match(source, /setSearchParams\(\{ keyword: search, page: '1', status: String\(nextStatus\) \}\)/);
assert.match(source, /未分类/);
assert.match(source, /待定/);
assert.match(source, /ticket\.name/);

console.log('admin ticket list source tests passed');
