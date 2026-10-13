import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const storageSource = fs.readFileSync(
  new URL('../src/services/storage.ts', import.meta.url),
  'utf8',
);

const appSource = fs.readFileSync(
  new URL('../src/app/App.tsx', import.meta.url),
  'utf8',
);

test('mobile storage uses AsyncStorage-backed cache', () => {
  assert.match(storageSource, /@react-native-async-storage\/async-storage/);
  assert.match(storageSource, /hydrate:\s*\(\): Promise<void> => storage\.hydrate\(\)/);
  assert.match(storageSource, /void AsyncStorage\.setItem/);
});

test('app waits for storage hydration before rendering navigators', () => {
  assert.match(appSource, /storageService\s*\.\s*hydrate\(\)/);
  assert.match(appSource, /if \(!storageReady\)\s*\{\s*return <Loading \/>;/);
});
