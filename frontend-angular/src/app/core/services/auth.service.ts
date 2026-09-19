import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { finalize, map, Observable, ReplaySubject, take, tap } from 'rxjs';
import { API_ENDPOINTS } from '../api/api-endpoints';
import { LoginCredentials, LoginResponse, RegisterUserPayload, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<User | null>(null);
  readonly loading = signal(true);
  private readonly sessionReady = new ReplaySubject<void>(1);

  constructor(private readonly http: HttpClient) {
    this.restoreSession();
  }

  login(credentials: LoginCredentials): Observable<User> {
    return this.http.post<LoginResponse>(API_ENDPOINTS.userLogin, credentials).pipe(
      tap((response) => this.saveSession(response)),
      map((response) => this.toUser(response)),
      tap((user) => this.user.set(user))
    );
  }

  register(payload: RegisterUserPayload): Observable<string> {
    return this.http.post(API_ENDPOINTS.userRegister, payload, { responseType: 'text' });
  }

  logout(): void {
    this.user.set(null);
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
      error: () => this.user.set(null)
    });
  }

  private saveSession(response: LoginResponse): void {
    // The API stores the token in a Secure, HttpOnly cookie. JavaScript must
    // not copy that token or the user profile into browser storage.
  }

  private toUser(response: LoginResponse): User {
    return { id: response.id, nome: response.nome, email: response.email, role: response.role };
  }

  private isValidUser(value: User): boolean {
    return typeof value?.id === 'number' && typeof value.nome === 'string'
      && typeof value.email === 'string' && typeof value.role === 'string';
  }

}
