package co.edu.sena.sigea.common.entity;

// =============================================================================
// CLASE: EntidadBase (CLASE ABSTRACTA)
// =============================================================================
// Esta es la CLASE PADRE de todas las entidades del sistema.
//
// ¿Qué es una clase abstracta?
// Es una clase que NO se puede instanciar directamente (no puedes hacer
// "new EntidadBase()"). Solo sirve para que OTRAS clases hereden de ella.
// Es como un "molde base" que dice: "toda entidad en SIGEA tiene un id,
// una fecha de creación y una fecha de actualización".
//
// ¿Por qué la necesitamos?
// Porque TODAS las entidades (Usuario, Equipo, Prestamo, etc.) necesitan:
//   - Un campo "id" (identificador único)
//   - Un campo "fechaCreacion" (cuándo se creó el registro)
//   - Un campo "fechaActualizacion" (cuándo se modificó por última vez)
//
// Sin esta clase, tendríamos que COPIAR y PEGAR estos 3 campos en las 15
// entidades del sistema. Eso viola el principio DRY (Don't Repeat Yourself).
//
// PRINCIPIOS APLICADOS:
// ─────────────────────
// POO - Herencia:
//   Todas las entidades heredan id, fechaCreacion y fechaActualizacion
//   automáticamente. Escribimos este código UNA sola vez.
//
// POO - Abstracción:
//   La clase es "abstract" porque no tiene sentido crear una "entidad base"
//   por sí sola. Solo tiene sentido como padre de entidades concretas.
//
// POO - Encapsulamiento:
//   Los campos son "protected" (#), lo que significa que solo las clases
//   hijas (y el propio paquete) pueden acceder directamente a ellos.
//   El mundo exterior usa los getters y setters que genera Lombok.
//
// SOLID - Single Responsibility:
//   Esta clase tiene UNA sola responsabilidad: definir los campos comunes
//   de auditoría para todas las entidades.
//
// SOLID - Liskov Substitution:
//   Cualquier método que reciba un "EntidadBase" funcionará correctamente
//   con cualquier entidad hija (Usuario, Equipo, Prestamo, etc.).
//
// ANOTACIONES JPA EXPLICADAS:
// ───────────────────────────
// @MappedSuperclass:
//   Le dice a JPA: "Esta clase NO es una tabla, pero sus campos deben
//   incluirse en las tablas de las clases que hereden de ella."
//   Sin esta anotación, JPA ignoraría los campos id, fechaCreacion, etc.
//
// @PrePersist:
//   "Antes de guardar por PRIMERA VEZ en la base de datos, ejecuta este método."
//   Lo usamos para fijar la fechaCreacion automáticamente.
//
// @PreUpdate:
//   "Antes de ACTUALIZAR un registro existente, ejecuta este método."
//   Lo usamos para actualizar la fechaActualizacion automáticamente.
//
// ANOTACIONES LOMBOK EXPLICADAS:
// ──────────────────────────────
// @Getter: Genera automáticamente getters para todos los campos.
//   Ejemplo: genera public Long getId() { return this.id; }
//
// @Setter: Genera automáticamente setters para todos los campos.
//   Ejemplo: genera public void setId(Long id) { this.id = id; }
//
// Sin Lombok, tendríamos que escribir manualmente 6 métodos (get/set para
// cada campo). En las entidades con 15+ campos, Lombok ahorra CIENTOS de líneas.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass  // JPA: "No crees tabla para esta clase, pero hereda sus campos"
@Getter            // Lombok: genera getters automáticos para id, fechaCreacion, fechaActualizacion
@Setter            // Lombok: genera setters automáticos para id, fechaCreacion, fechaActualizacion
public abstract class EntidadBase {

    // =========================================================================
    // CAMPO: id
    // =========================================================================
    // Es el identificador único de cada registro en la base de datos.
    //
    // @Id: Le dice a JPA "este campo es la PRIMARY KEY de la tabla".
    //
    // @GeneratedValue(strategy = GenerationType.IDENTITY):
    //   Le dice a JPA "no me pidas el id, déjalo que la base de datos lo genere
    //   automáticamente con AUTO_INCREMENT". Así cada registro recibe un número
    //   secuencial único: 1, 2, 3, 4...
    //
    // ¿Por qué Long y no Integer?
    //   Long soporta hasta 9,223,372,036,854,775,807 registros.
    //   Integer solo hasta 2,147,483,647.
    //   Aunque SIGEA nunca tendrá tantos registros, usar Long es una BUENA PRÁCTICA
    //   estándar en JPA porque previene problemas si el sistema crece mucho.
    // =========================================================================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    // =========================================================================
    // CAMPO: fechaCreacion
    // =========================================================================
    // Registra CUÁNDO se creó este registro por primera vez.
    // Se fija automáticamente en el método onCreate() y NUNCA se modifica después.
    //
    // @Column(nullable = false, updatable = false):
    //   - nullable = false → La columna NO puede ser NULL en la BD (es obligatoria)
    //   - updatable = false → JPA nunca incluirá este campo en un UPDATE.
    //     Una vez creada la fecha, es INMUTABLE. Nadie puede cambiarla.
    //
    // ¿Por qué LocalDateTime y no Date?
    //   java.util.Date es una clase vieja (Java 1.0) con muchos problemas de diseño.
    //   LocalDateTime es parte de java.time (Java 8+), es inmutable, thread-safe,
    //   y representa correctamente fecha + hora sin zona horaria.
    // =========================================================================
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    protected LocalDateTime fechaCreacion;

    // =========================================================================
    // CAMPO: fechaActualizacion
    // =========================================================================
    // Registra CUÁNDO fue la última vez que se modificó este registro.
    // Se actualiza automáticamente en el método onUpdate().
    //
    // @Column(nullable = false):
    //   - nullable = false → Siempre debe tener un valor.
    //   (No ponemos updatable = false porque este campo SÍ se actualiza)
    // =========================================================================
    @Column(name = "fecha_actualizacion", nullable = false)
    protected LocalDateTime fechaActualizacion;

    // =========================================================================
    // MÉTODO: onCreate()
    // =========================================================================
    // Este método se ejecuta automáticamente JUSTO ANTES de que JPA inserte
    // el registro en la base de datos por primera vez (INSERT).
    //
    // @PrePersist:
    //   Es un "callback de ciclo de vida" de JPA. Le dices a JPA:
    //   "Antes de hacer el INSERT, ejecuta este método."
    //
    // ¿Qué hace?
    //   Fija la fechaCreacion y la fechaActualizacion al momento actual.
    //   Así no tienes que recordar setear la fecha manualmente cada vez
    //   que creas un registro. Es AUTOMÁTICO.
    //
    // LocalDateTime.now():
    //   Obtiene la fecha y hora actual del servidor.
    // =========================================================================
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }

    // =========================================================================
    // MÉTODO: onUpdate()
    // =========================================================================
    // Este método se ejecuta automáticamente JUSTO ANTES de que JPA actualice
    // el registro en la base de datos (UPDATE).
    //
    // @PreUpdate:
    //   Otro "callback de ciclo de vida" de JPA. Le dices:
    //   "Antes de hacer el UPDATE, ejecuta este método."
    //
    // ¿Qué hace?
    //   Actualiza la fechaActualizacion al momento actual.
    //   Así siempre sabes cuándo fue la última vez que se modificó un registro.
    // =========================================================================
    @PreUpdate
    protected void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
