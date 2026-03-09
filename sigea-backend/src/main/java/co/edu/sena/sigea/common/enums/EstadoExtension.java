package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoExtension
// =============================================================================
// Estados de una solicitud de extensión de préstamo (RF-PRE-07 / RN-02).
// Un préstamo puede extenderse máximo 2 veces, y cada extensión pasa por este ciclo.
// =============================================================================

public enum EstadoExtension {

    // El usuario solicitó extender su préstamo, esperando respuesta del admin
    SOLICITADA,

    // El administrador aprobó la extensión, la nueva fecha de devolución se actualiza
    APROBADA,

    // El administrador rechazó la extensión, la fecha original se mantiene
    RECHAZADA
}
