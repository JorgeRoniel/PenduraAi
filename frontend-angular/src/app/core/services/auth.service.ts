import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { catchError, finalize, map, Observable, of, ReplaySubject, take, tap } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { LoginCredentials, RegisterUserPayload, User } from '../models/user.model';
import { AuthStateService } from './auth-state.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly state = inject(AuthStateService);
  readonly user = this.state.user;
  readonly loading = signal(true);
  private readonly sessionReady = new ReplaySubject<void>(1);

  constructor(private readonly http: HttpClient) {
    this.restoreSession();
  }

  login(credentials: LoginCredentials): Observable<User> {
    return this.http.post<User>(API_ENDPOINTS.userLogin, credentials).pipe(
      tap((user) => this.user.set(user))
    );
  }

  register(payload: RegisterUserPayload): Observable<string> {
    return this.http.post(API_ENDPOINTS.userRegister, payload, { responseType: 'text' });
  }

  logout(): Observable<void> {
    this.state.clear();
    return this.http.post<void>(API_ENDPOINTS.userLogout, {}).pipe(
      catchError(() => of(undefined))
    );
  }

  isAuthenticated(): boolean {
    return this.user() !== null;
  }

  whenReady(): Observable<boolean> {
    return this.sessionReady.pipe(
      take(1),
      map(() => this.isAuthenticated())
    );
  }

  private restoreSession(): void {
    this.http.get<User>(API_ENDPOINTS.currentUser).pipe(
      finalize(() => {
        this.loading.set(false);
        this.sessionReady.next();
        this.sessionReady.complete();
      })
    ).subscribe({
      next: (user) => {
        if (this.isValidUser(user)) this.user.set(user);
      },
      error: () => this.state.clear()
    });
  }

  private isValidUser(value: User): boolean {
    return typeof value?.id === 'number' && typeof value.nome === 'string'
      && typeof value.email === 'string' && typeof value.role === 'string';
  }

}
