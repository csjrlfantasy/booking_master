export function shouldAttachAdminToken(url?: string) {
  if (!url) return false;
  return url.startsWith('/admin') || url.includes('/api/admin');
}

export function shouldRedirectToAdminLogin(requestUrl?: string, currentPath = window.location.pathname) {
  if (currentPath.startsWith('/shop')) {
    return false;
  }
  if (requestUrl) {
    return shouldAttachAdminToken(requestUrl) || !requestUrl.startsWith('/shop');
  }
  return true;
}
