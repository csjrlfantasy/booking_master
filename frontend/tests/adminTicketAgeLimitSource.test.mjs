import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';

const createPage = readFileSync('frontend/src/pages/ticket/TicketCreatePage.tsx', 'utf8');
const editPage = readFileSync('frontend/src/pages/ticket/TicketEditPage.tsx', 'utf8');
const adminApi = readFileSync('frontend/src/api/admin.ts', 'utf8');

for (const source of [createPage, editPage]) {
  assert.match(source, /name="ageLimit"/);
  assert.match(source, /适合观看年龄/);
  assert.match(source, /18\+/);
}

assert.match(adminApi, /ageLimit\?: string/);
assert.match(adminApi, /ageLimit: raw\.ageLimit/);

console.log('admin ticket age limit source tests passed');
