export interface LoginRequest {
  numeroDocumento: string;
  contrasena: string;
}

export interface LoginResponse {
  id: number;
  token: string;
  tipo: string;
  nombreCompleto: string;
  correoElectronico: string;
  rol: string;
  esSuperAdmin: boolean;
}

export interface UserSession {
  id: number;
  nombreCompleto: string;
  correoElectronico: string;
  numeroDocumento?: string;
  rol: string;
  token: string;
  esSuperAdmin: boolean;
}

export interface RegisterRequest {
  nombre: string;
  tipoDocumento: string;
  numeroDocumento: string;
  correoElectronico: string;
  programaFormacion?: string;
  telefono?: string;
  numeroFicha?: string;
  contrasena: string;
}
