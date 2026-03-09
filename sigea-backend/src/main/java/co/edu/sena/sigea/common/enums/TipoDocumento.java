package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: TipoDocumento
// =============================================================================
// Tipos de documento de identidad válidos en Colombia.
// Estos son los que el SENA maneja para identificar a sus aprendices e instructores.
//
// ¿Por qué un Enum?
// Porque estos tipos están definidos por ley en Colombia y no cambian frecuentemente.
// Si cambiaran (ej: se crea un nuevo tipo de documento), agregas una línea aquí.
// =============================================================================

public enum TipoDocumento {

    // Cédula de Ciudadanía - Para mayores de 18 años colombianos
    CC,

    // Tarjeta de Identidad - Para menores de 18 años colombianos
    TI,

    // Cédula de Extranjería - Para extranjeros residentes en Colombia
    CE,

    // Pasaporte - Documento internacional
    PP,

    // Permiso Especial de Permanencia - Para migrantes venezolanos principalmente
    PEP
}
