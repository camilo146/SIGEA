export type TipoMantenimiento = 'PREVENTIVO' | 'CORRECTIVO';

export interface Mantenimiento {
  id: number;
  equipoId: number;
  nombreEquipo: string;
  codigoEquipo: string;
  tipo: TipoMantenimiento;
  descripcion: string;
  fechaInicio: string;
  fechaFin?: string;
  responsable: string;
  observaciones?: string;
  fechaCreacion?: string;
}

export interface MantenimientoCrear {
  equipoId: number;
  tipo: TipoMantenimiento;
  descripcion: string;
  fechaInicio: string;
  fechaFin?: string;
  responsable: string;
  observaciones?: string;
}

export interface MantenimientoCerrar {
  fechaFin: string;
  observaciones?: string;
}
