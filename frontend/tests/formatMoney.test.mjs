import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';
import { transform } from 'esbuild';

const source = await readFile(new URL('../src/utils/index.ts', import.meta.url), 'utf8');
const { code } = await transform(source, {
  loader: 'ts',
  format: 'esm',
});

const moduleUrl = `data:text/javascript;base64,${Buffer.from(code).toString('base64')}`;
const { formatMoney } = await import(moduleUrl);

assert.equal(formatMoney(12.3), '\u00a512.30');
assert.equal(formatMoney(undefined), '\u00a5--');
assert.equal(formatMoney(null), '\u00a5--');
assert.equal(formatMoney(Number.NaN), '\u00a5--');

console.log('formatMoney fallback tests passed');
