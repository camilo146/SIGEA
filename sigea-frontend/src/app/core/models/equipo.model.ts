export type EstadoEquipo =
  | 'ACTIVO'
  | 'EN_MANTENIMIENTO'
  | 'DISPONIBLE'
  | 'EN_PRESTAMO'
  | 'DADO_DE_BAJA';

export type TipoUsoEquipo = 'CONSUMIBLE' | 'NO_CONSUMIBLE';

export interface Equipo {
  id: number;
  nombre: string;
  descripcion?: string;
  codigoUnico: string;
  categoriaId: number;
  categoriaNombre: string;
  ambienteId: number;
  ambienteNombre: string;
  propietarioId?: number;
  propietarioNombre?: string;
  inventarioActualInstructorId?: number;
  inventarioActualInstructorNombre?: string;
  estado: EstadoEquipo;
  cantidadTotal: number;
  cantidadDisponible: number;
  tipoUso: TipoUsoEquipo;
  umbralMinimo: number;
  activo: boolean;
  fotos?: { id: number; rutaArchivo: string; nombreArchivo?: string }[];
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface EquipoCrear {
  nombre: string;
  descripcion?: string;
  codigoUnico: string;
  categoriaId: number;
  ambienteId: number;
  propietarioId?: number;
  cantidadTotal: number;
  tipoUso: TipoUsoEquipo;
  umbralMinimo: number;
}
