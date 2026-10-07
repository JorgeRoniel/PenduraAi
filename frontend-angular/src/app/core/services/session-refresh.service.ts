import { HttpBackend, HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { finalize, map, Observable, shareReplay } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { readXsrfToken, XSRF_HEADER_NAME } from '../security/xsrf-token';

@Injectable({ providedIn: 'root' })
export class SessionRefreshService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private refreshInFlight?: Observable<void>;

  refresh(): Observable<void> {
    if (!this.refreshInFlight) {
      const csrfToken = readXsrfToken();

      const headers = csrfToken
        ? new HttpHeaders().set(XSRF_HEADER_NAME, csrfToken)
        : new HttpHeaders();

      this.refreshInFlight = this.http
        .post<void>(API_ENDPOINTS.userRefresh, {}, { withCredentials: true, headers })
        .pipe(
          map(() => undefined),
          finalize(() => (this.refreshInFlight = undefined)),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.refreshInFlight;
  }
}
