export interface SubUbicacionResumen {
  id: number;
  nombre: string;
  ubicacion?: string;
  descripcion?: string;
  activo: boolean;
}

export interface Ambiente {
  id: number;
  nombre: string;
  ubicacion?: string;
  descripcion?: string;
  direccion?: string;
  instructorResponsableId: number;
  instructorResponsableNombre: string;
  propietarioId?: number;
  propietarioNombre?: string;
  activo: boolean;
  rutaFoto?: string;
  /** ID del ambiente padre (solo si es sub-ubicación). */
  padreId?: number;
  /** Nombre del ambiente padre (solo si es sub-ubicación). */
  padreNombre?: string;
  /** Sub-ubicaciones hijas de este ambiente. */
  subUbicaciones?: SubUbicacionResumen[];
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface AmbienteCrear {
  nombre: string;
  ubicacion?: string;
  descripcion?: string;
  direccion?: string;
  /** Obligatorio para admin; null cuando un instructor crea (el backend lo asigna). */
  idInstructorResponsable: number | null;
  /** ID del ambiente padre; si se proporciona, crea como sub-ubicación. */
  padreId?: number | null;
}
