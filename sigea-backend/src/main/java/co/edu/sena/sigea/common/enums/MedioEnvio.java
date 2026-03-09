package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: MedioEnvio
// =============================================================================
// Define por qué canal se envía una notificación (RF-NOT-06).
//
// Regla de negocio:
// - Si el usuario tiene correo registrado → se envía por EMAIL.
// - Si NO tiene correo → se muestra como alerta INTERNA dentro del sistema.
// =============================================================================

public enum MedioEnvio {

    // Correo electrónico enviado por SMTP (JavaMailSender)
    EMAIL,

    // Alerta visible dentro del sistema (no requiere correo)
    INTERNA
}
