package co.edu.sena.sigea.prestamoambiente.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.TipoActividad;
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

@Entity
@Table(name = "prestamo_ambiente")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoAmbiente extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambiente_id", nullable = false)
    private Ambiente ambiente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_ambiente_id", nullable = false)
    private Usuario propietarioAmbiente;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "proposito", columnDefinition = "TEXT", nullable = false)
    private String proposito;

    @Column(name = "numero_participantes", nullable = false)
    private Integer numeroParticipantes;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_actividad", nullable = false, length = 20)
    private TipoActividad tipoActividad;

    @Column(name = "observaciones_solicitud", columnDefinition = "TEXT")
    private String observacionesSolicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    @Builder.Default
    private EstadoPrestamoAmbiente estado = EstadoPrestamoAmbiente.SOLICITADO;

    @Column(name = "observaciones_devolucion", columnDefinition = "TEXT")
    private String observacionesDevolucion;

    /** Escala 1-10 del estado en que se devolvió el ambiente */
    @Column(name = "estado_devolucion_ambiente")
    private Integer estadoDevolucionAmbiente;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "fecha_devolucion")
    private LocalDateTime fechaDevolucion;
}
