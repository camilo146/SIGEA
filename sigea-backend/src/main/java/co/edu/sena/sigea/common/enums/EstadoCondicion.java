package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: EstadoCondicion
// =============================================================================
// Describe la condición FÍSICA de un equipo en un momento específico.
// Se usa en DOS momentos:
//   1. Al ENTREGAR el equipo (RF-PRE-03 / RN-03): "¿En qué estado sale?"
//   2. Al DEVOLVER el equipo (RF-PRE-04 / RN-04): "¿En qué estado regresa?"
//
// ¿Por qué importa?
// Si un equipo sale en estado "EXCELENTE" y regresa en estado "MALO", el sistema
// puede generar un ReporteDano (RF-PRE-08) automáticamente o alertar al admin.
// Esto da TRAZABILIDAD: sabes exactamente quién tenía el equipo cuando se dañó.
// =============================================================================

public enum EstadoCondicion {

    // Sin ningún defecto visible, funciona perfectamente
    EXCELENTE,

    // Funciona bien, puede tener desgaste normal por uso
    BUENO,

    // Funciona pero tiene defectos visibles o parciales
    REGULAR,

    // No funciona correctamente o tiene daños significativos
    MALO
}
