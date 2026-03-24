export interface Notificacion {
  id: number;
  usuarioDestinoId: number;
  nombreUsuarioDestino: string;
  tipoNotificacion: string;
  mensaje: string;
  titulo: string;
  medioEnvio: string;
  estadoEnvio: string;
  leida: boolean;
  fechaEnvio: string;
  fechaCreacion?: string;
}
