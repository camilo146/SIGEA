package co.edu.sena.sigea.prestamo.entity;

// =============================================================================
// ENTIDAD: Prestamo (CABECERA)
// =============================================================================
// Representa la CABECERA de un préstamo: quién, cuándo, estado general.
// Los equipos específicos incluidos en el préstamo van en DetallePrestamo.
//
// TABLA EN BD: prestamo
//
// PATRÓN DE DISEÑO: Cabecera-Detalle (Master-Detail)
//   Es el mismo patrón que usa una FACTURA:
//   - FACTURA (cabecera): cliente, fecha, total → Prestamo
//   - LÍNEA DE FACTURA (detalle): producto, cantidad, precio → DetallePrestamo
//
//   ¿Por qué este patrón?
//   Porque un préstamo puede incluir VARIOS equipos (RF-PRE-03: "equipo(s)").
//   Sin esta separación, tendríamos que crear un registro de préstamo por cada
//   equipo, repitiendo los datos del usuario, fecha, admin, etc.
//   Eso viola el principio DRY y la 2da Forma Normal (2FN) de BD.
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-PRE-01 a RF-PRE-10, RN-01 a RN-06
//
// NOTA SOBRE LAS 3 RELACIONES A USUARIO:
//   Este préstamo tiene TRES FK a la tabla usuario:
//   1. usuario_solicitante_id → Quién pide el equipo
//   2. administrador_aprueba_id → Quién autoriza el préstamo
//   3. administrador_recibe_id → Quién recibe la devolución
//
//   Pueden ser la misma persona o personas diferentes:
//   - El admin que aprueba puede ser diferente al que recibe la devolución
//     (turnos diferentes, vacaciones, etc.)
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.reserva.entity.Reserva;
import co.edu.sena.sigea.usuario.entity.Usuario;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prestamo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prestamo extends EntidadBase {

    // =========================================================================
    // RELACIÓN: usuarioSolicitante
    // =========================================================================
    // El usuario que solicita el préstamo (puede ser admin o estándar).
    //
    // RF-PRE-01: "El sistema debe permitir al usuario estándar solicitar un
    //             préstamo seleccionando los equipos deseados."
    //
    // nullable = false → Todo préstamo DEBE tener un solicitante.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_solicitante_id", nullable = false)
    private Usuario usuarioSolicitante;

    // =========================================================================
    // RELACIÓN: administradorAprueba
    // =========================================================================
    // El administrador que aprobó (o rechazó) la solicitud.
    //
    // RF-PRE-02: "Solo el administrador puede aprobar o rechazar solicitudes."
    // RN-06: Confirma esta regla.
    //
    // nullable = true → porque al momento de CREAR la solicitud, aún no hay
    //   un admin que la haya aprobado. Se llena cuando el admin responde.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_aprueba_id")
    private Usuario administradorAprueba;

    // =========================================================================
    // RELACIÓN: administradorRecibe
    // =========================================================================
    // El administrador que recibe los equipos cuando el usuario los devuelve.
    //
    // RF-PRE-04: "Administrador que recibe" es un dato obligatorio en devolución.
    //
    // nullable = true → se llena solo al momento de la devolución.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_recibe_id")
    private Usuario administradorRecibe;

    /** Reserva desde la que se creó este préstamo (equipo recogido). Null si el préstamo no viene de reserva. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    // =========================================================================
    // CAMPOS DE FECHA Y HORA
    // =========================================================================
    // Cada campo registra un MOMENTO específico del ciclo de vida del préstamo.
    //
    // ¿Por qué tantas fechas?
    //   Para TRAZABILIDAD completa: saber exactamente cuándo pasó cada cosa.
    //   Ej: "¿Cuánto tardó el admin en aprobar?" = fechaHoraAprobacion - fechaHoraSolicitud
    //   Ej: "¿Devolvió a tiempo?" = fechaHoraDevolucionReal vs fechaHoraDevolucionEstimada
    // =========================================================================

    // Cuándo el usuario creó la solicitud
    @Column(name = "fecha_hora_solicitud", nullable = false)
    private LocalDateTime fechaHoraSolicitud;

    // Cuándo el admin aprobó o rechazó (NULL si aún no responde)
    @Column(name = "fecha_hora_aprobacion")
    private LocalDateTime fechaHoraAprobacion;

    // Cuándo se entregó físicamente el equipo al usuario (NULL si aún no se entrega)
    @Column(name = "fecha_hora_salida")
    private LocalDateTime fechaHoraSalida;

    // Fecha límite para devolver (se fija al crear la solicitud)
    // Si el usuario no devuelve antes de esta fecha → EN_MORA
    @Column(name = "fecha_hora_devolucion_estimada", nullable = false)
    private LocalDateTime fechaHoraDevolucionEstimada;

    // Cuándo se devolvió realmente (NULL si aún no se devuelve)
    @Column(name = "fecha_hora_devolucion_real")
    private LocalDateTime fechaHoraDevolucionReal;

    // =========================================================================
    // CAMPO: estado
    // =========================================================================
    // Estado actual del préstamo en su ciclo de vida.
    // Ver enum EstadoPrestamo para la explicación de cada estado.
    // =========================================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPrestamo estado = EstadoPrestamo.SOLICITADO;

    // =========================================================================
    // CAMPO: observacionesGenerales
    // =========================================================================
    // Notas generales sobre el préstamo.
    // Ejemplo: "Para la práctica de cableado estructurado del viernes"
    // =========================================================================
    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    // =========================================================================
    // CAMPO: extensionesRealizadas
    // =========================================================================
    // Contador de cuántas veces se ha extendido este préstamo.
    //
    // RN-02: "Cada préstamo puede ser extendido un máximo de 2 veces."
    //
    // Validación en el servicio:
    //   if (prestamo.getExtensionesRealizadas() >= 2) {
    //       throw new MaximoExtensionesException("Ya se alcanzó el máximo de extensiones");
    //   }
    // =========================================================================
    @Column(name = "extensiones_realizadas", nullable = false)
    private Integer extensionesRealizadas = 0;

    // =========================================================================
    // RELACIÓN: detalles (OneToMany)
    // =========================================================================
    // Lista de equipos incluidos en este préstamo.
    //
    // @OneToMany: "Un préstamo tiene muchos detalles (líneas de equipos)"
    //
    // mappedBy = "prestamo":
    //   Le dice a JPA: "La FK está en la tabla detalle_prestamo, en la columna
    //   que mapea el campo 'prestamo' de la clase DetallePrestamo."
    //   Esto evita que JPA cree una tabla intermedia (join table).
    //
    // cascade = CascadeType.ALL:
    //   "Si guardo/elimino el préstamo, también guarda/elimina sus detalles."
    //   Operaciones en cascada: si haces prestamoRepository.save(prestamo),
    //   automáticamente guarda los detalles también.
    //
    // orphanRemoval = true:
    //   "Si quito un detalle de la lista, elimínalo de la BD."
    //   Ej: prestamo.getDetalles().remove(detalle) → DELETE FROM detalle_prestamo
    //
    // @Builder.Default:
    //   Le dice a Lombok que cuando se usa el Builder, inicialice este campo
    //   con una lista vacía en vez de NULL.
    // =========================================================================
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DetallePrestamo> detalles = new ArrayList<>();

    // =========================================================================
    // RELACIÓN: extensiones (OneToMany)
    // =========================================================================
    // Lista de extensiones solicitadas para este préstamo (máx. 2).
    // =========================================================================
    @OneToMany(mappedBy = "prestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExtensionPrestamo> extensiones = new ArrayList<>();
}
