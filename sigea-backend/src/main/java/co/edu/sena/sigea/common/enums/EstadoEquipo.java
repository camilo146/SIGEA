package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoEquipo
// =============================================================================
// Representa el estado operativo de un equipo en el inventario.
//
// IMPORTANTE: No confundir con el campo "activo" (boolean).
// - "activo" = ¿el equipo existe en el sistema? (soft delete)
// - "estado" = ¿el equipo está disponible para prestar o está en reparación?
//
// Son dos conceptos diferentes:
// Un equipo puede estar ACTIVO (existe en el sistema, activo=true)
// pero EN_MANTENIMIENTO (no se puede prestar porque está en reparación).
// =============================================================================

public enum EstadoEquipo {

    // El equipo funciona correctamente y puede ser prestado
    ACTIVO,

    // El equipo está en reparación o mantenimiento, NO se puede prestar
    EN_MANTENIMIENTO
}
