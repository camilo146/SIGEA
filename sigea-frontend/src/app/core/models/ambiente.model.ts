export interface Ambiente {
  id: number;
  nombre: string;
  ubicacion?: string;
  descripcion?: string;
  direccion?: string;
  instructorResponsableId: number;
  instructorResponsableNombre: string;
  activo: boolean;
  rutaFoto?: string;
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
}
