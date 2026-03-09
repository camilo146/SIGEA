package co.edu.sena.sigea.mantenimiento.entity;

// =============================================================================
// ENTIDAD: Mantenimiento
// =============================================================================
// Registra las actividades de mantenimiento realizadas a los equipos.
//
// TABLA EN BD: mantenimiento
//
// REQUERIMIENTOS:
//   RF-MAN-01: Registrar mantenimientos (preventivos/correctivos) con
//              equipo, tipo, descripción, fecha inicio, fecha fin,
//              responsable, observaciones.
//   RN-11: Registrar mantenimiento con mínimo: tipo, descripción, fecha,
//           responsable.
//
// TIPOS DE MANTENIMIENTO (enum TipoMantenimiento):
//   - PREVENTIVO: Se hace ANTES de que el equipo falle (planificado).
//   - CORRECTIVO: Se hace DESPUÉS de detectar un fallo (reactivo).
//
// ¿POR QUÉ fechaFin ES NULLABLE?
//   Porque cuando registramos un mantenimiento que está EN CURSO,
//   aún no sabemos cuándo va a terminar. Se actualiza cuando se completa.
// =============================================================================

import java.time.LocalDate;

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.equipo.entity.Equipo;
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

@Entity
@Table(name = "mantenimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mantenimiento extends EntidadBase {

    // Equipo al que se le realiza el mantenimiento
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // Tipo: PREVENTIVO o CORRECTIVO
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMantenimiento tipo;

    // Descripción de lo que se hizo o se va a hacer
    @Column(name = "descripcion", columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    // Fecha en que inició el mantenimiento
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    // Fecha en que terminó (nullable = el mantenimiento puede estar en curso)
    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    // Persona o empresa responsable del mantenimiento
    @Column(name = "responsable", nullable = false, length = 200)
    private String responsable;

    // Observaciones adicionales (hallazgos, repuestos usados, etc.)
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;
}
