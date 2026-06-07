import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/api/admin.ts', 'utf8');

assert.match(source, /export async function batchUpdateAdminTicketStatus/);
assert.match(source, /export async function batchAdminCancelOrders/);
assert.match(source, /export async function batchAdminRefundOrders/);
assert.match(source, /successIds/);
assert.match(source, /failed/);

console.log('admin batch action source tests passed');
