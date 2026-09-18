import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from './auth.interceptor';
import { HttpClient } from '@angular/common/http';
import { API_ENDPOINTS } from '../api/api-endpoints';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('adds the bearer token when one exists', () => {
    localStorage.setItem('token', 'abc123');
    client.get(API_ENDPOINTS.user).subscribe();

    const request = http.expectOne(API_ENDPOINTS.user);
    expect(request.request.headers.get('Authorization')).toBe('Bearer abc123');
    request.flush({});
  });

  it('does not add authorization when there is no token', () => {
    client.get(API_ENDPOINTS.user).subscribe();

    const request = http.expectOne(API_ENDPOINTS.user);
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });

  it('does not send the token to another origin', () => {
    localStorage.setItem('token', 'abc123');
    client.get('/assets/config.json').subscribe();

    const request = http.expectOne('/assets/config.json');
    expect(request.request.headers.has('Authorization')).toBeFalse();
    request.flush({});
  });
});
