import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve('booking-app');

const requiredFiles = [
  'android/settings.gradle',
  'android/build.gradle',
  'android/gradle.properties',
  'android/app/build.gradle',
  'android/app/src/main/AndroidManifest.xml',
  'android/app/src/main/java/com/ticketmore/app/MainActivity.kt',
  'android/app/src/main/java/com/ticketmore/app/MainApplication.kt',
];

for (const file of requiredFiles) {
  assert.ok(fs.existsSync(path.join(root, file)), `${file} should exist`);
}

const packageJson = fs.readFileSync(path.join(root, 'package.json'), 'utf8');
assert.match(packageJson, /gradle\.bat assembleDebug/);
assert.match(packageJson, /gradle\.bat assembleRelease/);

const rnConfig = fs.readFileSync(path.join(root, 'react-native.config.js'), 'utf8');
assert.match(rnConfig, /packageName: 'com\.ticketmore\.app'/);

const appConfig = fs.readFileSync(path.join(root, 'src/constants/index.ts'), 'utf8');
assert.match(appConfig, /http:\/\/10\.0\.2\.2:8088\/api/);
assert.doesNotMatch(appConfig, /http:\/\/localhost:8088\/api/);

const manifest = fs.readFileSync(path.join(root, 'android/app/src/main/AndroidManifest.xml'), 'utf8');
assert.match(manifest, /android\.permission\.INTERNET/);
assert.match(manifest, /usesCleartextTraffic="true"/);

const appGradle = fs.readFileSync(path.join(root, 'android/app/build.gradle'), 'utf8');
assert.match(appGradle, /namespace "com\.ticketmore\.app"/);
assert.match(appGradle, /applicationId "com\.ticketmore\.app"/);

console.log('android packaging source tests passed');
