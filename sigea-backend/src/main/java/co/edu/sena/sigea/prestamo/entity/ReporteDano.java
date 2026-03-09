package co.edu.sena.sigea.prestamo.entity;

// =============================================================================
// ENTIDAD: ReporteDano
// =============================================================================
// Registra el daño reportado cuando un equipo se devuelve en mal estado.
//
// TABLA EN BD: reporte_dano
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-PRE-08: "Registrar un reporte de daño cuando un equipo se devuelve
//               en mal estado: descripción, fotografía (opcional), fecha."
//   RN-05: "Si un equipo se devuelve dañado, se debe generar un reporte de
//           daño antes de reincorporarlo al inventario."
//
// RELACIÓN CLAVE:
//   ReporteDano 1 ←→ 1 DetallePrestamo (OneToOne)
//   Cada reporte está asociado a UN detalle específico de un préstamo.
//   No al préstamo completo, sino al equipo ESPECÍFICO que se dañó.
// =============================================================================

import java.time.LocalDateTime;

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reporte_dano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteDano extends EntidadBase {

    // =========================================================================
    // RELACIÓN: detallePrestamo
    // =========================================================================
    // El detalle del préstamo donde se detectó el daño.
    //
    // @OneToOne: "Un reporte de daño corresponde a exactamente un detalle"
    //
    // @JoinColumn: La FK está AQUÍ (en reporte_dano), apuntando a detalle_prestamo.
    //   unique = true → garantiza la relación 1:1 a nivel de BD.
    //   No pueden existir dos reportes de daño para el mismo detalle de préstamo.
    // =========================================================================
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detalle_prestamo_id", nullable = false, unique = true)
    private DetallePrestamo detallePrestamo;

    // =========================================================================
    // CAMPO: descripcion
    // =========================================================================
    // Descripción detallada del daño encontrado.
    // Ejemplo: "El cable presenta un corte a 30cm del conector RJ45.
    //           El conector izquierdo está partido."
    // =========================================================================
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    // =========================================================================
    // CAMPO: fotoRuta
    // =========================================================================
    // Ruta de la fotografía del daño (OPCIONAL según RF-PRE-08).
    // Se guarda en el sistema de archivos, aquí solo va la ruta.
    // =========================================================================
    @Column(name = "foto_ruta", length = 500)
    private String fotoRuta;

    // =========================================================================
    // CAMPO: fechaReporte
    // =========================================================================
    // Cuándo se registró el reporte de daño.
    // =========================================================================
    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    // =========================================================================
    // RELACIÓN: reportadoPor
    // =========================================================================
    // El administrador que generó el reporte de daño.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reportado_por_id", nullable = false)
    private Usuario reportadoPor;
}
