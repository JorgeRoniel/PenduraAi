import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthStateService } from '../services/auth-state.service';
import { SessionRefreshService } from '../services/session-refresh.service';

const RETRIED_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const injector = inject(Injector);
  const refreshService = inject(SessionRefreshService);
  if (!isApiRequest(request.url)) {
    return next(request);
  }

  const authenticatedRequest = request.clone({ withCredentials: true });
  if (isAuthRequest(request.url) || request.context.get(RETRIED_AFTER_REFRESH)) {
    return next(authenticatedRequest);
  }

  return next(authenticatedRequest).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse) || error.status !== 401) {
        return throwError(() => error);
      }

      return refreshService.refresh().pipe(
        switchMap(() => next(authenticatedRequest.clone({
          context: authenticatedRequest.context.set(RETRIED_AFTER_REFRESH, true)
        }))),
        catchError(() => {
          injector.get(AuthStateService).clear();
          void injector.get(Router).navigateByUrl('/login');
          return throwError(() => error);
        })
      );
    })
  );
};

function isApiRequest(requestUrl: string): boolean {
  try {
    return new URL(requestUrl, window.location.origin).origin === new URL(environment.apiUrl).origin;
  } catch {
    return false;
  }
}

function isAuthRequest(requestUrl: string): boolean {
  return requestUrl === API_ENDPOINTS.userLogin
    || requestUrl === API_ENDPOINTS.userRefresh
    || requestUrl === API_ENDPOINTS.userLogout
    || requestUrl === API_ENDPOINTS.userRegister;
}
