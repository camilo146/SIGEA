package co.edu.sena.sigea.usuario.entity;

// =============================================================================
// ENTIDAD: Usuario
// =============================================================================
// Representa a una persona que interactúa con el sistema SIGEA.
// Puede ser un ADMINISTRADOR (instructor) o un USUARIO_ESTANDAR (aprendiz).
//
// TABLA EN BD: usuario
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-USR-01: Registro con nombre, documento, correo, teléfono, programa, ficha, rol
//   RF-USR-02: Actualización de información
//   RF-USR-03: Desactivación (eliminación lógica → campo "activo")
//   RF-USR-04: Roles: ADMINISTRADOR o USUARIO_ESTANDAR
//   RS-AUT-01: Autenticación con usuario y contraseña
//   RS-AUT-03: Bloqueo tras 3 intentos fallidos
//   RS-CIF-01: Contraseña almacenada como hash BCrypt
//   RF-AMB-05: SuperAdmin puede gestionar todos los ambientes
//
// PRINCIPIOS APLICADOS:
// ─────────────────────
// POO - Herencia:
//   Extiende EntidadBase → hereda id, fechaCreacion, fechaActualizacion.
//
// POO - Encapsulamiento:
//   Todos los campos son private (-). Se accede mediante getters/setters
//   generados por Lombok (@Getter, @Setter).
//
// SOLID - Single Responsibility:
//   Esta clase SOLO se encarga de representar los datos de un usuario.
//   NO contiene lógica de negocio (eso va en UsuarioService).
//   NO contiene lógica de acceso a datos (eso va en UsuarioRepository).
//   NO contiene lógica de presentación (eso va en UsuarioController).
//
// Clean Code - Nombres descriptivos:
//   "contrasenaHash" en vez de "pwd" o "pass".
//   "intentosFallidos" en vez de "fail" o "attempts".
//   Cualquiera que lea el código entiende qué hace cada campo.
//
// ANOTACIONES CLAVE (explicadas en cada campo):
// ──────────────────────────────────────────────
// @Entity: Le dice a JPA "esta clase es una tabla en la base de datos"
// @Table:  Especifica el nombre exacto de la tabla en la BD
// @Column: Configura las propiedades de cada columna (nombre, nullable, unique, etc.)
// @Enumerated(EnumType.STRING): Guarda el enum como texto ("ADMINISTRADOR")
//   en vez de número (0). Si usamos ORDINAL y reorganizamos el enum, se rompe todo.
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.enums.TipoDocumento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity // JPA: "Esta clase es una tabla"
@Table(name = "usuario") // JPA: "La tabla se llama 'usuario' en la BD"
@Getter // Lombok: genera getters para TODOS los campos
@Setter // Lombok: genera setters para TODOS los campos
@NoArgsConstructor // Lombok: genera constructor vacío → public Usuario() {}
                   // JPA REQUIERE un constructor sin argumentos para crear instancias
@AllArgsConstructor // Lombok: genera constructor con TODOS los campos como parámetros
@Builder // Lombok: genera el patrón Builder para crear objetos de forma legible
         // Ejemplo:
         // Usuario.builder().nombreCompleto("Juan").rol(Rol.ADMINISTRADOR).build();
public class Usuario extends EntidadBase {
    // "extends EntidadBase" = hereda id, fechaCreacion, fechaActualizacion

    // =========================================================================
    // CAMPO: nombreCompleto
    // =========================================================================
    // Nombre completo del usuario tal como aparece en su documento de identidad.
    //
    // @Column:
    // name = "nombre_completo" → Nombre de la columna en la BD (snake_case)
    // nullable = false → NO puede ser NULL (es obligatorio)
    // length = 150 → Máximo 150 caracteres (suficiente para nombres colombianos
    // que pueden ser largos: "Juan Pablo García de los Santos")
    // =========================================================================
    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    // =========================================================================
    // CAMPO: numeroDocumento
    // =========================================================================
    // Número del documento de identidad del usuario.
    //
    // unique = true → No pueden existir dos usuarios con el mismo documento.
    // En la BD esto crea un índice UNIQUE que previene duplicados.
    // Si intentas insertar un duplicado, la BD lanza una excepción.
    //
    // ¿Por qué String y no Long?
    // Porque algunos documentos tienen letras (ej: CE, pasaportes)
    // y porque los documentos pueden empezar con ceros (ej: 0012345678).
    // Si fuera Long, los ceros iniciales se perderían.
    // =========================================================================
    @Column(name = "numero_documento", nullable = false, unique = true, length = 20)
    private String numeroDocumento;

    // =========================================================================
    // CAMPO: tipoDocumento
    // =========================================================================
    // Tipo de documento de identidad: CC, TI, CE, PP, PEP
    //
    // @Enumerated(EnumType.STRING):
    // Guarda el valor como TEXTO en la BD: "CC", "TI", "CE", etc.
    //
    // ¿Por qué STRING y no ORDINAL?
    // ORDINAL guardaría 0, 1, 2, 3, 4 (la posición del enum).
    // Problema: si reordenas el enum o agregas un valor en medio,
    // los números cambian y los datos existentes se rompen.
    // STRING es más seguro: aunque reordenes, "CC" siempre es "CC".
    // =========================================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 5)
    private TipoDocumento tipoDocumento;

    // =========================================================================
    // CAMPO: correoElectronico
    // =========================================================================
    // Dirección de correo electrónico del usuario.
    //
    // nullable = true → El correo es OPCIONAL.
    // ¿Por qué? RF-NOT-06 dice: "Si el usuario no tiene correo registrado,
    // la notificación se mostrará dentro del sistema como alerta interna."
    // Entonces el sistema funciona sin correo.
    //
    // unique = true → Dos usuarios no pueden tener el mismo correo.
    // Pero como es nullable, MariaDB permite múltiples NULLs (NULL != NULL).
    // =========================================================================
    @Column(name = "correo_electronico", unique = true, length = 100)
    private String correoElectronico;

    // =========================================================================
    // CAMPO: telefono
    // =========================================================================
    // Número de teléfono del usuario (opcional).
    //
    // ¿Por qué String y no Integer/Long?
    // Porque los teléfonos pueden tener formato: "+57 310 123 4567"
    // y porque pueden empezar con "+" o "0".
    // =========================================================================
    @Column(name = "telefono", length = 20)
    private String telefono;

    // =========================================================================
    // CAMPO: programaFormacion
    // =========================================================================
    // Nombre del programa de formación del SENA al que pertenece el aprendiz.
    // Ejemplo: "Tecnólogo en Gestión de Redes de Telecomunicaciones"
    //
    // Puede ser NULL porque un instructor no está inscrito en un programa.
    // =========================================================================
    @Column(name = "programa_formacion", length = 200)
    private String programaFormacion;

    // =========================================================================
    // CAMPO: ficha
    // =========================================================================
    // Identificador del grupo de formación al que pertenece el aprendiz.
    // Ejemplo: "2889321"
    //
    // Puede ser NULL porque un instructor o empleado no tiene ficha.
    // =========================================================================
    @Column(name = "ficha", length = 20)
    private String ficha;

    // =========================================================================
    // CAMPO: contrasenaHash
    // =========================================================================
    // Contraseña del usuario almacenada como HASH BCrypt.
    //
    // RS-CIF-01: "Las contraseñas deben almacenarse utilizando BCrypt, NUNCA en
    // texto plano."
    //
    // ¿Qué es un hash?
    // Es una función matemática de UNA sola vía:
    // "MiContraseña123!@" → "$2a$10$Xk4B3... (60 caracteres aleatorios)"
    // No se puede revertir: del hash NO puedes obtener la contraseña original.
    // Para verificar, se hashea lo que el usuario escribe y se compara con el
    // hash almacenado.
    //
    // length = 255 → BCrypt genera hashes de ~60 caracteres, pero dejamos margen.
    // =========================================================================
    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    // =========================================================================
    // CAMPO: rol
    // =========================================================================
    // Rol del usuario en el sistema: ADMINISTRADOR o USUARIO_ESTANDAR
    //
    // RF-USR-04: "El sistema debe asignar roles: Administrador o Usuario Estándar."
    //
    // Este campo determina QUÉ puede hacer el usuario en el sistema.
    // Spring Security usa este valor para proteger las rutas/endpoints.
    // =========================================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Rol rol;

    // =========================================================================
    // CAMPO: esSuperAdmin
    // =========================================================================
    // Indica si este usuario es el superadministrador del sistema.
    //
    // RF-AMB-05: "Un superadministrador (el primer administrador del sistema)
    // podrá gestionar todos los ambientes."
    //
    // Un admin normal solo ve/gestiona SU ambiente.
    // El superadmin ve y gestiona TODOS los ambientes.
    //
    // columnDefinition = "BOOLEAN DEFAULT FALSE":
    // Le dice a la BD que el valor por defecto es FALSE.
    // Así, si no se especifica al crear, será false automáticamente.
    // =========================================================================
    @Column(name = "es_super_admin", nullable = false)
    private Boolean esSuperAdmin = false;

    // =========================================================================
    // CAMPO: activo
    // =========================================================================
    // Indica si el usuario está activo o ha sido "eliminado".
    //
    // RF-USR-03: "El sistema debe permitir desactivar usuarios (eliminación
    // lógica)."
    //
    // ¿Qué es eliminación lógica (soft delete)?
    // En vez de borrar el registro de la BD (DELETE FROM usuario WHERE id = 5),
    // simplemente ponemos activo = false.
    // El usuario "desaparece" del sistema, pero sus datos se conservan para
    // historial, auditoría y trazabilidad.
    //
    // RN-13: "Un usuario desactivado no puede iniciar sesión ni realizar
    // solicitudes."
    // =========================================================================
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // =========================================================================
    // CAMPO: intentosFallidos
    // =========================================================================
    // Contador de intentos fallidos consecutivos de inicio de sesión.
    //
    // RS-AUT-03: "Tras 3 intentos fallidos consecutivos, la cuenta se bloqueará
    // temporalmente durante 5 minutos. Después de un segundo bloqueo consecutivo,
    // el tiempo incrementa a 15 minutos."
    //
    // Cuando el usuario ingresa la contraseña correcta, se resetea a 0.
    // Cuando se equivoca, se incrementa en 1.
    // Si llega a 3, la cuenta se bloquea y se fija cuentaBloqueadaHasta.
    // =========================================================================
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    // =========================================================================
    // CAMPO: cuentaBloqueadaHasta
    // =========================================================================
    // Fecha y hora hasta la cual la cuenta está bloqueada por intentos fallidos.
    //
    // Si es NULL → la cuenta NO está bloqueada.
    // Si tiene un valor en el futuro → la cuenta está bloqueada hasta esa hora.
    // Si tiene un valor en el pasado → el bloqueo ya expiró, puede intentar de
    // nuevo.
    //
    // Ejemplo: si a las 10:00 se bloquea por 5 minutos, este campo tendrá 10:05.
    // A las 10:03 → sigue bloqueada (10:03 < 10:05)
    // A las 10:06 → ya puede intentar (10:06 > 10:05)
    // =========================================================================
    @Column(name = "cuenta_bloqueada_hasta")
    private LocalDateTime cuentaBloqueadaHasta;

    // =========================================================================
    // CAMPO: emailVerificado
    // =========================================================================
    // Indica si el usuario verificó su correo mediante el enlace enviado al
    // registrarse.
    // =========================================================================
    @Column(name = "email_verificado", nullable = false)
    private Boolean emailVerificado = false;

    @Column(name = "token_verificacion", length = 255)
    private String tokenVerificacion;

    @Column(name = "token_verificacion_expira")
    private LocalDateTime tokenVerificacionExpira;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, length = 10)
    private EstadoAprobacion estadoAprobacion = EstadoAprobacion.APROBADO;
}
