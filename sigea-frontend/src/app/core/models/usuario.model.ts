export type Rol = 'ADMINISTRADOR' | 'INSTRUCTOR' | 'ALIMENTADOR_EQUIPOS' | 'APRENDIZ' | 'FUNCIONARIO' | 'USUARIO_ESTANDAR';
export type TipoDocumento = 'CC' | 'TI' | 'CE' | 'PP' | 'PEP';
export type EstadoAprobacion = 'PENDIENTE' | 'APROBADO';

export interface Usuario {
  id: number;
  nombreCompleto: string;
  tipoDocumento: string;
  numeroDocumento: string;
  correoElectronico: string;
  telefono?: string;
  programaFormacion?: string;
  ficha?: string;
  rol: string;
  esSuperAdmin: boolean;
  activo: boolean;
  estadoAprobacion?: EstadoAprobacion;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface UsuarioCrear {
  nombreCompleto: string;
  tipoDocumento: TipoDocumento;
  numeroDocumento: string;
  correoElectronico: string;
  programaFormacion?: string;
  numeroFicha?: string;
  telefono?: string;
  contrasena: string;
  rol: Rol;
}
