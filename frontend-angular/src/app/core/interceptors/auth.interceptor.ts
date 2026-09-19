import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isApiRequest(request.url)) {
    return next(request);
  }

  return next(request.clone({ withCredentials: true }));
};

function isApiRequest(requestUrl: string): boolean {
  try {
    return new URL(requestUrl, window.location.origin).origin === new URL(environment.apiUrl).origin;
  } catch {
    return false;
  }
}
