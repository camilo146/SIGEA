package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoPrestamo
// =============================================================================
// Representa el ciclo de vida completo de un préstamo en SIGEA.
//
// Ciclo de vida:
//
//   SOLICITADO → APROBADO → ACTIVO → DEVUELTO
//        │                    │
//        ↓                    ↓
//     RECHAZADO            EN_MORA → DEVUELTO
//        │
//        ↓
//     CANCELADO
//
// Cada transición tiene reglas:
// - Solo el ADMINISTRADOR puede mover de SOLICITADO → APROBADO/RECHAZADO (RN-06)
// - ACTIVO → EN_MORA lo hace automáticamente una tarea programada (@Scheduled)
// - ACTIVO/EN_MORA → DEVUELTO lo hace el ADMINISTRADOR al registrar la devolución
// - Solo el USUARIO puede CANCELAR su propia solicitud (antes de ser aprobada)
// =============================================================================

public enum EstadoPrestamo {

    // El usuario creó la solicitud, está esperando que un admin la apruebe
    SOLICITADO,

    // El admin autorizó el préstamo, pero aún no se ha entregado físicamente
    APROBADO,

    // El admin rechazó la solicitud (puede incluir un motivo)
    RECHAZADO,

    // El equipo fue entregado físicamente al usuario
    ACTIVO,

    // El equipo fue devuelto y recibido por un administrador
    DEVUELTO,

    // La fecha de devolución estimada pasó sin que el usuario devolviera el equipo
    // (RF-PRE-06: el sistema debe identificar y listar los préstamos vencidos)
    EN_MORA,

    // El usuario canceló la solicitud antes de que fuera aprobada
    CANCELADO
}
