import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    http.expectOne(API_ENDPOINTS.currentUser).flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('stores the token and user after a successful login', () => {
    let resultId: number | undefined;

    service.login({ email: 'ana@example.com', senha: '123456' }).subscribe((user) => {
      resultId = user.id;
    });

    const request = http.expectOne(API_ENDPOINTS.userLogin);
    expect(request.request.method).toBe('POST');
    request.flush({ token: 'token-123', id: 7, email: 'ana@example.com', nome: 'Ana', role: 'USER' });

    expect(resultId).toBe(7);
    expect(service.user()?.nome).toBe('Ana');
    expect(localStorage.length).toBe(0);
  });

  it('clears corrupted session data during restoration', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [AuthService, provideHttpClient(), provideHttpClientTesting()] });
    const restored = TestBed.inject(AuthService);
    const restoredHttp = TestBed.inject(HttpTestingController);
    restoredHttp.expectOne(API_ENDPOINTS.currentUser).flush(null, { status: 401, statusText: 'Unauthorized' });

    expect(restored.user()).toBeNull();
    expect(restored.loading()).toBeFalse();
  });

  it('removes the complete session on logout', () => {
    service.user.set({ id: 1, nome: 'Ana', email: 'ana@example.com', role: 'USER' });

    service.logout();

    expect(service.user()).toBeNull();
    expect(localStorage.length).toBe(0);
  });
});
