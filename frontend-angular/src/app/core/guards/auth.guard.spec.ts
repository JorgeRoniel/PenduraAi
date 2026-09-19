import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard, guestGuard } from './auth.guard';
import { API_ENDPOINTS } from '../api/api-endpoints';

describe('auth guards', () => {
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [AuthService, provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(AuthService);
    TestBed.inject(HttpTestingController).expectOne(API_ENDPOINTS.currentUser)
      .flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  it('redirects unauthenticated users to login', () => {
    const result = TestBed.runInInjectionContext(() => authGuard(route, state));
    const router = TestBed.inject(Router);

    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  it('allows authenticated users to access private routes', () => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [AuthService, provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(AuthService);
    TestBed.inject(HttpTestingController).expectOne(API_ENDPOINTS.currentUser)
      .flush({ id: 1, nome: 'Ana', email: 'ana@example.com', role: 'USER' });

    const result = TestBed.runInInjectionContext(() => authGuard(route, state));

    expect(result).toBeTrue();
  });

  it('redirects authenticated users away from guest routes', () => {
    TestBed.inject(HttpTestingController).verify();
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [AuthService, provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    TestBed.inject(AuthService);
    TestBed.inject(HttpTestingController).expectOne(API_ENDPOINTS.currentUser)
      .flush({ id: 1, nome: 'Ana', email: 'ana@example.com', role: 'USER' });
    const result = TestBed.runInInjectionContext(() => guestGuard(route, state));
    const router = TestBed.inject(Router);

    expect(router.serializeUrl(result as UrlTree)).toBe('/');
  });
});
