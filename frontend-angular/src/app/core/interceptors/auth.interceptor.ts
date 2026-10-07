import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthStateService } from '../services/auth-state.service';
import { SessionRefreshService } from '../services/session-refresh.service';
import { readXsrfToken, XSRF_HEADER_NAME } from '../security/xsrf-token';

const RETRIED_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const injector = inject(Injector);
  const refreshService = inject(SessionRefreshService);
  if (!isApiRequest(request.url)) {
    return next(request);
  }

  let authenticatedRequest = request.clone({ withCredentials: true });

  if (requiresCsrfToken(request.method)) {
    const csrfToken = readXsrfToken();

    if (csrfToken) {
      authenticatedRequest = authenticatedRequest.clone({
        setHeaders: {
          [XSRF_HEADER_NAME]: csrfToken,
        },
      });
    }
  }
  if (isAuthRequest(request.url) || request.context.get(RETRIED_AFTER_REFRESH)) {
    return next(authenticatedRequest);
  }

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      return refreshService.refresh().pipe(
        switchMap(() =>
          next(
            authenticatedRequest.clone({
              context: authenticatedRequest.context.set(RETRIED_AFTER_REFRESH, true),
            }),
          ),
        ),
        catchError(() => {
          injector.get(AuthStateService).clear();
          void injector.get(Router).navigateByUrl('/login');
          return throwError(() => error);
        }),
      );
    }),
  );
};

function requiresCsrfToken(method: string): boolean {
  return !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase());
}

function isApiRequest(requestUrl: string): boolean {
  try {
    const url = new URL(requestUrl, window.location.origin);
    const apiOrigin = new URL(environment.apiUrl || '/', window.location.origin).origin;
    return (
      url.origin === apiOrigin && url.pathname.startsWith('/api/')
    );
  } catch {
    return false;
  }
}

function isAuthRequest(requestUrl: string): boolean {
  return (
    requestUrl === API_ENDPOINTS.userLogin ||
    requestUrl === API_ENDPOINTS.userRefresh ||
    requestUrl === API_ENDPOINTS.userLogout ||
    requestUrl === API_ENDPOINTS.userRegister
  );
}
