import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/guards/auth.guard';
import { LoginComponent } from './features/auth/login.component';
import { RegisterComponent } from './features/auth/register.component';
import { HomeComponent } from './features/debts/home.component';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    component: LoginComponent,
    data: { label: 'Login' }
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    component: RegisterComponent,
    data: { label: 'Cadastro' }
  },
  {
    path: '',
    canActivate: [authGuard],
    component: HomeComponent,
    data: { label: 'Área principal' }
  },
  { path: '**', redirectTo: '' }
];
