package co.edu.sena.sigea.prestamoambiente.enums;

public enum EstadoPrestamoAmbiente {
    /** Solicitud enviada, pendiente de revisión por el propietario del ambiente */
    SOLICITADO,
    /** El propietario aprobó el préstamo */
    APROBADO,
    /** El propietario rechazó la solicitud */
    RECHAZADO,
    /** El préstamo está activo (en uso) */
    ACTIVO,
    /** El ambiente fue devuelto */
    DEVUELTO,
    /** El solicitante canceló la solicitud */
    CANCELADO
}
