export type EstadoReserva = 'ACTIVA' | 'CANCELADA' | 'COMPLETADA' | 'CUMPLIDA' | 'VENCIDA' | 'EXPIRADA' | 'PRESTADO';

import type { TipoUsoEquipo } from './equipo.model';

export interface Reserva {
  id: number;
  usuarioId: number;
  nombreUsuario: string;
  correoUsuario: string;
  equipoId: number;
  nombreEquipo: string;
  codigoEquipo: string;
  tipoUso?: TipoUsoEquipo;
  cantidad: number;
  fechaHoraInicio: string;
  fechaHoraFin: string;
  estado: EstadoReserva;
  fechaCreacion?: string;
}

export interface ReservaCrear {
  equipoId: number;
  cantidad: number;
  fechaHoraInicio: string;
}
