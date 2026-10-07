export const XSRF_HEADER_NAME = 'X-XSRF-TOKEN';
const XSRF_COOKIE_NAME = 'XSRF-TOKEN';

export function readXsrfToken(): string | null {
  const prefix = `${XSRF_COOKIE_NAME}=`;

  const cookie = document.cookie
    .split(';')
    .map((item) => item.trim())
    .find((item) => item.startsWith(prefix));

  if (!cookie) {
    return null;
  }

  return decodeURIComponent(cookie.substring(prefix.length));
}
