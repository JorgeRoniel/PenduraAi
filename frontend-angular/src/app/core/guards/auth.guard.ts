import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { map } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.loading()) {
    return auth.isAuthenticated() ? true : router.createUrlTree(['/login']);
  }
  return auth.whenReady().pipe(
    map((authenticated) => authenticated ? true : router.createUrlTree(['/login']))
  );
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.loading()) {
    return auth.isAuthenticated() ? router.createUrlTree(['/']) : true;
  }
  return auth.whenReady().pipe(
    map((authenticated) => authenticated ? router.createUrlTree(['/']) : true)
  );
};
