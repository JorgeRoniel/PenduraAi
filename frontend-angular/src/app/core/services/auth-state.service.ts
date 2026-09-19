import { Injectable, signal } from '@angular/core';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  readonly user = signal<User | null>(null);

  clear(): void {
    this.user.set(null);
  }
}
