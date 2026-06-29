import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { transform } from 'esbuild';

const source = await readFile(new URL('../src/pages/shop/customerSession.ts', import.meta.url), 'utf8');
const { code } = await transform(source, {
  loader: 'ts',
  format: 'esm',
});

const store = new Map();
globalThis.localStorage = {
  getItem: (key) => store.get(key) ?? null,
  setItem: (key, value) => store.set(key, String(value)),
  removeItem: (key) => store.delete(key),
};

const moduleUrl = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
const {
  CUSTOMER_EXPIRES_AT_KEY,
  CUSTOMER_TOKEN_KEY,
  clearCustomerSession,
  readCustomerSession,
  saveCustomerSession,
} = await import(moduleUrl);

clearCustomerSession();
const saved = saveCustomerSession({ token: 'valid-token', username: 'alice', expiresIn: 3600 }, 'alice');
assert.equal(saved.token, 'valid-token');
assert.equal(readCustomerSession().token, 'valid-token');

store.set(CUSTOMER_TOKEN_KEY, 'expired-token');
store.set(CUSTOMER_EXPIRES_AT_KEY, String(Date.now() - 1000));
assert.equal(readCustomerSession().token, '');
assert.equal(store.has(CUSTOMER_TOKEN_KEY), false);

store.set(CUSTOMER_TOKEN_KEY, 'legacy-token-without-expiry');
assert.equal(readCustomerSession().token, '');

console.log('customer session expiry tests passed');
