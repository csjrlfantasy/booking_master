import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const contracts = {
  'frontend/src/pages/auth/LoginPage.tsx': [
    'admin-login-form', 'admin-username', 'admin-password', 'admin-login-submit'
  ],
  'frontend/src/pages/shop/ShopLoginPage.tsx': [
    'shop-login-form', 'shop-username', 'shop-password', 'shop-login-submit'
  ],
  'frontend/src/pages/shop/ShopPage.tsx': [
    'shop-search-input', 'shop-search-submit'
  ],
  'frontend/src/pages/user/UserListPage.tsx': [
    'admin-user-page', 'admin-user-search', 'admin-user-table', 'admin-user-create'
  ],
  'frontend/src/pages/user/UserCreatePage.tsx': [
    'admin-user-create-form', 'admin-user-create-submit'
  ],
  'frontend/src/pages/ticket/TicketListPage.tsx': ['admin-ticket-table'],
  'frontend/src/pages/order/OrderListPage.tsx': ['admin-order-table'],
  'frontend/src/pages/coupon/AdminCouponListPage.tsx': ['admin-coupon-table']
};

for (const [file, testIds] of Object.entries(contracts)) {
  const source = readFileSync(file, 'utf8');
  for (const testId of testIds) {
    assert.match(source, new RegExp(`data-testid=["']${testId}["']`), `${file} is missing data-testid=${testId}`);
  }
}

const shopSource = readFileSync('frontend/src/pages/shop/ShopPage.tsx', 'utf8');
assert.match(shopSource, /gridTestId\?: string/);
assert.match(shopSource, /emptyTestId\?: string/);
assert.match(shopSource, /data-testid=\{gridTestId\}/);
assert.match(shopSource, /data-testid=\{emptyTestId\}/);
assert.match(shopSource, /gridTestId="shop-ticket-grid" emptyTestId="shop-empty-result"/);

console.log('course Playwright contract source tests passed');
