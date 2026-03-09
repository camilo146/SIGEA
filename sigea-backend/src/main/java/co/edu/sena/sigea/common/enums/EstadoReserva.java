package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoReserva
// =============================================================================
// Ciclo de vida de una reserva anticipada de equipos (RF-RES-01 a RF-RES-04).
//
// Ciclo:
//   ACTIVA → COMPLETADA  (el usuario recogió el equipo a tiempo)
//   ACTIVA → EXPIRADA    (pasaron 2 horas y no recogió → RN-08, cancelación automática)
//   ACTIVA → CANCELADA   (el usuario canceló antes de la hora de inicio → RF-RES-03)
// =============================================================================

public enum EstadoReserva {

    // La reserva está vigente, el equipo está apartado para el usuario
    ACTIVA,

    // El usuario canceló la reserva voluntariamente antes de la hora de inicio
    CANCELADA,

    // El usuario recogió el equipo, la reserva se convirtió en préstamo
    COMPLETADA,

    // Pasaron 2 horas sin que el usuario recogiera el equipo (RN-08)
    // El equipo vuelve a estar disponible automáticamente
    EXPIRADA
}
