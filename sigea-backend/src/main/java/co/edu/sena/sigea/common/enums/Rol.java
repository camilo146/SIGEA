package co.edu.sena.sigea.common.enums;

// =============================================================================
// ENUM: Rol
// =============================================================================
// ¿Qué es un Enum en Java?
// Es un tipo especial que define un conjunto FIJO de valores posibles.
// Es como decir: "El rol de un usuario SOLO puede ser uno de estos dos valores,
// no hay un tercer, cuarto o quinto rol posible."
//
// ¿Por qué un Enum y no un String?
// Si usáramos String, alguien podría escribir "administrador", "Administrador",
// "ADMIN", "admin"... y eso genera errores. Con un Enum, Java te OBLIGA a usar
// exactamente Rol.ADMINISTRADOR o Rol.USUARIO_ESTANDAR. Si escribes mal,
// el compilador te avisa ANTES de ejecutar. Eso es SEGURIDAD DE TIPOS.
//
// Principio SOLID aplicado:
// - Open/Closed: Si mañana el SENA pide un rol "AUDITOR", solo agregas una línea
//   aquí. No modificas el resto del código (bueno, casi... los guards y filtros
//   sí se actualizan, pero la estructura base no cambia).
// =============================================================================

public enum Rol {

    // Administrador del sistema / centro.
    // Puede: gestionar inventario, usuarios, ambientes, préstamos, reportes.
    ADMINISTRADOR,

    // Instructor encargado de un ambiente de formación.
    // Puede: gestionar inventario del ambiente, préstamos, reservas.
    INSTRUCTOR,

    // Aprendices (usuarios estándar).
    // Puede: consultar equipos, solicitar préstamos, reservas, ver su historial.
    APRENDIZ,

    // Funcionarios del centro (usuarios estándar).
    // Puede: consultar equipos, solicitar préstamos, reservas, ver su historial.
    FUNCIONARIO,

    // Rol genérico para compatibilidad.
    USUARIO_ESTANDAR
}
