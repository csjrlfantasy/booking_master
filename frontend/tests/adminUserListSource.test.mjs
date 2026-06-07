import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/user/UserListPage.tsx', 'utf8');

assert.match(source, /userType/);
assert.match(source, /status: nextStatus/);
assert.match(source, /userType: nextUserType/);
assert.match(source, /setSearchParams\(buildUserListSearchParams/);
assert.match(source, /user\.totalOrderAmount/);

console.log('admin user list source tests passed');
