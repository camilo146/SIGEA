package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoEnvio
// =============================================================================
// Indica si una notificación fue enviada exitosamente o no (RF-NOT-05).
// Esto es fundamental para TRAZABILIDAD: si un usuario dice "nunca me llegó
// la notificación", podemos verificar en el log si fue ENVIADA o FALLIDA.
// =============================================================================

public enum EstadoEnvio {

    // La notificación está en cola, aún no se ha intentado enviar
    PENDIENTE,

    // La notificación fue enviada exitosamente al destinatario
    ENVIADA,

    // Hubo un error al enviar (correo no válido, SMTP caído, etc.)
    FALLIDA
}
