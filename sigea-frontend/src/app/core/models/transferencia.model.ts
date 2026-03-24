export interface Transferencia {
  id: number;
  equipoId: number;
  nombreEquipo: string;
  codigoEquipo: string;
  inventarioOrigenInstructorId: number;
  nombreInventarioOrigenInstructor: string;
  inventarioDestinoInstructorId: number;
  nombreInventarioDestinoInstructor: string;
  propietarioEquipoId?: number;
  nombrePropietarioEquipo?: string;
  ubicacionDestinoId?: number;
  nombreUbicacionDestino?: string;
  cantidad: number;
  administradorAutorizaId: number;
  nombreAdministrador: string;
  motivo?: string;
  fechaTransferencia: string;
  fechaCreacion?: string;
}

export interface TransferenciaCrear {
  equipoId: number;
  instructorDestinoId: number;
  ubicacionDestinoId?: number;
  cantidad: number;
  motivo?: string;
  fechaTransferencia: string;
}
