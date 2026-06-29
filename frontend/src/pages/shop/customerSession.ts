import type { CustomerLoginResponse } from '@/api/customer';

export const CUSTOMER_TOKEN_KEY = 'booking_customer_token';
export const CUSTOMER_NAME_KEY = 'booking_customer_name';
export const CUSTOMER_USER_ID_KEY = 'booking_customer_user_id';
export const CUSTOMER_EXPIRES_AT_KEY = 'booking_customer_expires_at';

export function readCustomerSession() {
  const token = localStorage.getItem(CUSTOMER_TOKEN_KEY) || '';
  const expiresAt = Number(localStorage.getItem(CUSTOMER_EXPIRES_AT_KEY) || 0);
  if (token && (!expiresAt || Date.now() > expiresAt)) {
    clearCustomerSession();
    return { token: '', name: '', userId: '' };
  }

  return {
    token,
    name: localStorage.getItem(CUSTOMER_NAME_KEY) || '',
    userId: localStorage.getItem(CUSTOMER_USER_ID_KEY) || '',
  };
}

export function saveCustomerSession(data: CustomerLoginResponse, fallbackName: string) {
  const userId = String(data.userId ?? data.id ?? '');
  const name = data.nickname || data.username || fallbackName;

  localStorage.setItem(CUSTOMER_TOKEN_KEY, data.token);
  localStorage.setItem(CUSTOMER_NAME_KEY, name);
  localStorage.setItem(CUSTOMER_EXPIRES_AT_KEY, String(Date.now() + (data.expiresIn ?? 86400) * 1000));
  if (userId) {
    localStorage.setItem(CUSTOMER_USER_ID_KEY, userId);
  } else {
    localStorage.removeItem(CUSTOMER_USER_ID_KEY);
  }

  return { token: data.token, name, userId };
}

export function clearCustomerSession() {
  localStorage.removeItem(CUSTOMER_TOKEN_KEY);
  localStorage.removeItem(CUSTOMER_NAME_KEY);
  localStorage.removeItem(CUSTOMER_USER_ID_KEY);
  localStorage.removeItem(CUSTOMER_EXPIRES_AT_KEY);
}
