export interface User {
  id: number;
  nome: string;
  email: string;
  role: string;
  createdAt?: string;
}

export interface LoginCredentials {
  email: string;
  senha: string;
}

export interface RegisterUserPayload {
  nome: string;
  email: string;
  senha: string;
}

export interface RegisterResponse {
  message: string;
}
