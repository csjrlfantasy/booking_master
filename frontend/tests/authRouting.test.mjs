import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { transform } from 'esbuild';

const source = await readFile(new URL('../src/services/authRouting.ts', import.meta.url), 'utf8');
const { code } = await transform(source, {
  loader: 'ts',
  format: 'esm',
});

const moduleUrl = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
const { shouldAttachAdminToken, shouldRedirectToAdminLogin } = await import(moduleUrl);

assert.equal(shouldAttachAdminToken('/admin/tickets'), true);
assert.equal(shouldAttachAdminToken('/tickets'), false);
assert.equal(shouldAttachAdminToken('/payment/mock-pay'), false);

assert.equal(shouldRedirectToAdminLogin('/tickets', '/shop'), false);
assert.equal(shouldRedirectToAdminLogin('/admin/tickets', '/tickets'), true);
assert.equal(shouldRedirectToAdminLogin('/orders', '/orders'), true);

console.log('auth routing tests passed');
