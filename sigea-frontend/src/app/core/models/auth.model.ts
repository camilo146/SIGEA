export interface LoginRequest {
  numeroDocumento: string;
  contrasena: string;
}

export interface LoginResponse {
  token: string;
  tipo: string;
  nombreCompleto: string;
  rol: string;
}

export interface UserSession {
  nombreCompleto: string;
  correoElectronico: string;
  numeroDocumento?: string;
  rol: string;
  token: string;
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
