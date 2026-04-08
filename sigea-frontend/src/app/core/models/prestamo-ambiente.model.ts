export type EstadoPrestamoAmbiente =
  | 'SOLICITADO'
  | 'APROBADO'
  | 'RECHAZADO'
  | 'ACTIVO'
  | 'DEVUELTO'
  | 'CANCELADO';

export type TipoActividad = 'CLASE' | 'TALLER' | 'EVALUACION' | 'REUNION' | 'OTRO';

export interface PrestamoAmbiente {
  id: number;
  ambienteId: number;
  ambienteNombre: string;
  solicitanteId: number;
  solicitanteNombre: string;
  fechaInicio: string;
  fechaFin: string;
  horaInicio: string;
  horaFin: string;
  proposito: string;
  numeroParticipantes?: number;
  tipoActividad?: TipoActividad;
  observacionesSolicitud?: string;
  estado: EstadoPrestamoAmbiente;
  observacionesDevolucion?: string;
  estadoDevolucionAmbiente?: number;
  fechaSolicitud?: string;
  fechaAprobacion?: string;
  fechaDevolucion?: string;
}

export interface PrestamoAmbienteSolicitud {
  ambienteId: number;
  fechaInicio: string;
  fechaFin: string;
  horaInicio: string;
  horaFin: string;
  proposito: string;
  numeroParticipantes?: number;
  tipoActividad?: TipoActividad;
  observacionesSolicitud?: string;
}

export interface PrestamoAmbienteDevolucion {
  observacionesDevolucion?: string;
  estadoDevolucionAmbiente?: number;
}
