import { environment } from '../../../environments/environment';

export const API_ENDPOINTS = {
  baseUrl: environment.apiUrl,
  user: `${environment.apiUrl}/api/user`,
  userLogin: `${environment.apiUrl}/api/user/auth/login`,
  currentUser: `${environment.apiUrl}/api/user/me`,
  userRegister: `${environment.apiUrl}/api/user/register`,
  debt: `${environment.apiUrl}/api/divida`
} as const;
