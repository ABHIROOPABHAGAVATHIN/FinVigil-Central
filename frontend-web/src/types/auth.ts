export interface AuthResponse {
  token: string;
  tokenType: string;
  customerUuid: string;
  email: string;
  name: string;
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  phone: string;
  password: string;
}

export interface UserSession {
  token: string;
  customerUuid: string;
  email: string;
  name: string;
  expiresAt: number;
}
