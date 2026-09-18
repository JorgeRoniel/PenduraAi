import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

const TOKEN_KEY = 'token';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem(TOKEN_KEY);

  if (!token || !isApiRequest(request.url)) {
    return next(request);
  }

  return next(request.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  }));
};

function isApiRequest(requestUrl: string): boolean {
  try {
    return new URL(requestUrl, window.location.origin).origin === new URL(environment.apiUrl).origin;
  } catch {
    return false;
  }
}
