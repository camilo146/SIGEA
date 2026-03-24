export type EstadoPrestamo = 'SOLICITADO' | 'APROBADO' | 'ACTIVO' | 'DEVUELTO' | 'RECHAZADO' | 'EN_MORA';

import type { TipoUsoEquipo } from './equipo.model';

export interface DetallePrestamoRespuesta {
  equipoId: number;
  nombreEquipo: string;
  codigoEquipo: string;
  cantidad: number;
  tipoUso?: TipoUsoEquipo;
  cantidadDevuelta?: number;
}

export interface Prestamo {
  id: number;
  usuarioSolicitanteId: number;
  nombreUsuarioSolicitante: string;
  correoUsuarioSolicitante: string;
  nombreAdministradorAprueba?: string;
  nombreAdministradorRecibe?: string;
  fechaHoraSolicitud: string;
  fechaHoraAprobacion?: string;
  fechaHoraSalida?: string;
  fechaHoraDevolucionEstimada: string;
  fechaHoraDevolucionReal?: string;
  estado: EstadoPrestamo;
  observacionesGenerales?: string;
  extensionesRealizadas: number;
  detalles: DetallePrestamoRespuesta[];
}

export interface DetallePrestamoCrear {
  equipoId: number;
  cantidad: number;
}

export interface PrestamoCrear {
  fechaHoraDevolucionEstimada: string;
  observacionesGenerales?: string;
  detalles: DetallePrestamoCrear[];
}
