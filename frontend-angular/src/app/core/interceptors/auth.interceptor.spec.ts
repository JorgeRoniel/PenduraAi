import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { HttpClient } from '@angular/common/http';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthStateService } from '../services/auth-state.service';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('sends API requests with credentials for the HttpOnly auth cookie', () => {
    client.get(API_ENDPOINTS.user).subscribe();

    const request = http.expectOne(API_ENDPOINTS.user);
    expect(request.request.withCredentials).toBeTrue();
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('does not add credentials to another origin', () => {
    client.get('/assets/config.json').subscribe();

    const request = http.expectOne('/assets/config.json');
    expect(request.request.withCredentials).toBeFalse();
    request.flush({});
  });

  it('refreshes the session once and retries an unauthorized API request', () => {
    let completed = false;
    client.get(API_ENDPOINTS.user).subscribe(() => completed = true);

    http.expectOne(API_ENDPOINTS.user).flush(null, { status: 401, statusText: 'Unauthorized' });
    const refreshRequest = http.expectOne(API_ENDPOINTS.userRefresh);
    expect(refreshRequest.request.withCredentials).toBeTrue();
    refreshRequest.flush(null, { status: 204, statusText: 'No Content' });

    http.expectOne(API_ENDPOINTS.user).flush({});
    expect(completed).toBeTrue();
  });

  it('shares one refresh request between simultaneous unauthorized requests', () => {
    client.get(`${API_ENDPOINTS.user}/1`).subscribe();
    client.get(`${API_ENDPOINTS.user}/2`).subscribe();

    const unauthorized = http.match((request) => request.url.startsWith(API_ENDPOINTS.user));
    unauthorized.forEach((request) => request.flush(null, { status: 401, statusText: 'Unauthorized' }));

    const refreshRequests = http.match(API_ENDPOINTS.userRefresh);
    expect(refreshRequests.length).toBe(1);
    refreshRequests[0].flush(null, { status: 204, statusText: 'No Content' });
    http.match((request) => request.url.startsWith(API_ENDPOINTS.user)).forEach((request) => request.flush({}));
  });

  it('clears the session when refresh fails', () => {
    const state = TestBed.inject(AuthStateService);
    state.user.set({ id: 1, nome: 'Ana', email: 'ana@example.com', role: 'USER' });
    client.get(API_ENDPOINTS.user).subscribe({ error: () => undefined });

    http.expectOne(API_ENDPOINTS.user).flush(null, { status: 401, statusText: 'Unauthorized' });
    http.expectOne(API_ENDPOINTS.userRefresh).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(state.user()).toBeNull();
  });
});
