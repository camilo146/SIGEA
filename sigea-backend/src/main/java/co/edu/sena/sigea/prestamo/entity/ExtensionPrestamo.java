package co.edu.sena.sigea.prestamo.entity;

// =============================================================================
// ENTIDAD: ExtensionPrestamo
// =============================================================================
// Representa una solicitud de extensión (prórroga) de un préstamo activo.
//
// TABLA EN BD: extension_prestamo
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-PRE-07: "Permitir la extensión de un préstamo activo, con un máximo
//               de 2 extensiones por préstamo, sujeta a aprobación del admin."
//   RN-02: "Cada préstamo puede ser extendido un máximo de 2 veces."
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoExtension;
import co.edu.sena.sigea.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "extension_prestamo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtensionPrestamo extends EntidadBase {

    // =========================================================================
    // RELACIÓN: prestamo
    // =========================================================================
    // El préstamo que se quiere extender.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    // =========================================================================
    // CAMPO: fechaSolicitud
    // =========================================================================
    // Cuándo el usuario pidió la extensión.
    // =========================================================================
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    // =========================================================================
    // CAMPO: nuevaFechaDevolucion
    // =========================================================================
    // La nueva fecha de devolución que el usuario propone.
    // Si el admin aprueba, el préstamo se actualiza con esta fecha.
    // =========================================================================
    @Column(name = "nueva_fecha_devolucion", nullable = false)
    private LocalDateTime nuevaFechaDevolucion;

    // =========================================================================
    // RELACIÓN: administradorAprueba
    // =========================================================================
    // El admin que aprobó o rechazó la extensión.
    // NULL mientras esté en estado SOLICITADA (aún sin respuesta).
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_aprueba_id")
    private Usuario administradorAprueba;

    // =========================================================================
    // CAMPO: estado
    // =========================================================================
    // SOLICITADA → APROBADA o RECHAZADA
    // =========================================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoExtension estado = EstadoExtension.SOLICITADA;

    // =========================================================================
    // CAMPO: motivo
    // =========================================================================
    // Razón por la cual el usuario solicita la extensión.
    // Ejemplo: "Necesito 2 días más para terminar la práctica de cableado"
    // =========================================================================
    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    // =========================================================================
    // CAMPO: fechaRespuesta
    // =========================================================================
    // Cuándo el admin respondió (aprobó o rechazó). NULL si aún no responde.
    // =========================================================================
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
}
