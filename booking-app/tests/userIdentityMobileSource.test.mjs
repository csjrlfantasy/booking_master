import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve('booking-app');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

const userApi = read('src/services/api/userApi.ts');
assert.match(userApi, /export interface UserIdentity/);
assert.match(userApi, /export interface UserIdentityPayload/);
assert.match(userApi, /getIdentities:\s*async/);
assert.match(userApi, /createIdentity:\s*async/);
assert.match(userApi, /deleteIdentity:\s*async/);
assert.match(userApi, /\/user\/identities/);

const navTypes = read('src/navigation/types.ts');
assert.match(navTypes, /Identity:\s*undefined/);

const profileNavigator = read('src/navigation/ProfileNavigator.tsx');
assert.match(profileNavigator, /IdentityScreen/);
assert.match(profileNavigator, /name="Identity"/);

const profileScreen = read('src/screens/profile/ProfileScreen.tsx');
assert.match(profileScreen, /常用观演人/);
assert.match(profileScreen, /screen:\s*'Identity'/);

const identityScreenPath = path.join(root, 'src/screens/profile/IdentityScreen.tsx');
assert.ok(fs.existsSync(identityScreenPath), 'IdentityScreen should exist');

const identityScreen = fs.readFileSync(identityScreenPath, 'utf8');
assert.match(identityScreen, /身份证/);
assert.match(identityScreen, /真实姓名/);
assert.match(identityScreen, /userApi\.getIdentities/);
assert.match(identityScreen, /userApi\.createIdentity/);
assert.match(identityScreen, /userApi\.deleteIdentity/);

console.log('mobile identity source tests passed');
