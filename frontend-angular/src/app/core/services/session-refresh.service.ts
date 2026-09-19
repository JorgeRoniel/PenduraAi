import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { finalize, map, Observable, shareReplay } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';

@Injectable({ providedIn: 'root' })
export class SessionRefreshService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private refreshInFlight?: Observable<void>;

  refresh(): Observable<void> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http.post<void>(API_ENDPOINTS.userRefresh, {}, { withCredentials: true }).pipe(
        map(() => undefined),
        finalize(() => this.refreshInFlight = undefined),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.refreshInFlight;
  }
}
