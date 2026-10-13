import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');

assert.match(source, /const shareTargets = \[/);
assert.match(source, /wechat/);
assert.match(source, /qq/);
assert.match(source, /weibo/);
assert.match(source, /xiaohongshu/);
assert.match(source, /douyin/);
assert.match(source, /kuaishou/);
assert.match(source, /copy/);
assert.match(source, /SharePanel/);
assert.match(source, /handleShareAction/);

console.log('shop share source tests passed');
