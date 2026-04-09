package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: TipoNotificacion
// =============================================================================
// Clasifica el tipo de notificación que el sistema envía.
// Esto permite filtrar, buscar y generar estadísticas sobre las notificaciones.
//
// Ejemplo de uso: "¿Cuántas notificaciones de mora se enviaron este mes?"
// → SELECT COUNT(*) FROM notificacion WHERE tipo = 'MORA' AND fecha >= ...
// =============================================================================

public enum TipoNotificacion {

    // Recordatorio ANTES del vencimiento del préstamo (RF-NOT-01)
    // Se envía según la lógica escalonada:
    // >= 5 días → 2 días antes
    // 1-4 días → 5 horas antes
    // < 1 día → 45 minutos antes
    RECORDATORIO_VENCIMIENTO,

    // Notificación DESPUÉS del vencimiento (RF-NOT-02)
    // Se envía diariamente mientras el préstamo siga vencido (RN-17)
    MORA,

    // Alerta cuando un equipo llega al umbral mínimo de stock (RF-NOT-04)
    STOCK_BAJO,

    // Notificación al admin cuando llega una nueva solicitud de préstamo
    SOLICITUD_PRESTAMO,

    // Reserva creada: el usuario recibe confirmación por correo
    RESERVA_CREADA,

    // Reserva cancelada antes de la entrega
    RESERVA_CANCELADA,

    // Reserva expirada por no recogida
    RESERVA_EXPIRADA,

    // Equipo recogido: el usuario recibe confirmación de que su reserva pasó a
    // préstamo
    EQUIPO_RECOGIDO,

    // Movimiento de salida del préstamo
    PRESTAMO_SALIDA,

    // Solicitud de préstamo cancelada
    PRESTAMO_CANCELADO,

    // Movimiento de devolución del préstamo
    PRESTAMO_DEVUELTO,

    // Flujo de ambientes
    SOLICITUD_PRESTAMO_AMBIENTE,
    PRESTAMO_AMBIENTE_APROBADO,
    PRESTAMO_AMBIENTE_RECHAZADO,
    PRESTAMO_AMBIENTE_CANCELADO,
    PRESTAMO_AMBIENTE_DEVUELTO,

    // Notificaciones genéricas del sistema
    GENERAL
}
