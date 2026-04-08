package co.edu.sena.sigea.observacion.entity;

import java.time.LocalDateTime;

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "observacion_equipo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionEquipo extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_duenio_id", nullable = false)
    private Usuario usuarioDuenio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_prestatario_id", nullable = false)
    private Usuario usuarioPrestatario;

    @Column(name = "observaciones", columnDefinition = "TEXT", nullable = false)
    private String observaciones;

    /**
     * Escala de 1 a 10 con la que el propietario evalúa el estado del equipo
     * devuelto
     */
    @Column(name = "estado_devolucion", nullable = false)
    private Integer estadoDevolucion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
