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
  placa?: string;
  serial?: string;
  modelo?: string;
  marcaId?: number;
  marcaNombre?: string;
  estadoEquipoEscala?: number;
  categoriaId: number;
  categoriaNombre: string;
  ambienteId: number;
  ambienteNombre: string;
  subUbicacionId?: number;
  subUbicacionNombre?: string;
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
  placa?: string;
  serial?: string;
  modelo?: string;
  marcaId?: number | null;
  categoriaId: number;
  ambienteId: number;
  subUbicacionId?: number | null;
  propietarioId?: number | null;
  cantidadTotal: number;
  tipoUso: TipoUsoEquipo;
  umbralMinimo: number;
}
