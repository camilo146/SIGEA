package co.edu.sena.sigea.reserva.entity;

// =============================================================================
// ENTIDAD: Reserva
// =============================================================================
// Permite a un usuario RESERVAR equipos con anticipación (antes de necesitarlos).
//
// TABLA EN BD: reserva
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-RES-01: Reservar con máximo 5 días hábiles de anticipación
//   RF-RES-02: Cancelación automática si no se recoge en 2 horas
//   RF-RES-03: El usuario puede cancelar antes de la hora de inicio
//   RF-RES-04: Equipos reservados se muestran como "no disponibles"
//   RN-07, RN-08
//
// PRIORIDAD: Could Have (v2.0)
//   Pero creamos la entidad ahora para que la BD esté completa desde el inicio.
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.equipo.entity.Equipo;
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
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    // Cuándo empieza la reserva (el usuario debe recoger a esta hora)
    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    // Cuándo termina la reserva (si no recoge en 2h después del inicio → EXPIRADA)
    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoReserva estado = EstadoReserva.ACTIVA;
}
