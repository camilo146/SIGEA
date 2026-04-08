export interface Marca {
  id: number;
  nombre: string;
  descripcion?: string;
  activo: boolean;
  fechaCreacion?: string;
  fechaActualizacion?: string;
}

export interface MarcaCrear {
  nombre: string;
  descripcion?: string;
}
