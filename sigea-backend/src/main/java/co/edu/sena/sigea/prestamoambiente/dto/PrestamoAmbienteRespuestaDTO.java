package co.edu.sena.sigea.prestamoambiente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.TipoActividad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoAmbienteRespuestaDTO {

    private Long id;
    private Long ambienteId;
    private String ambienteNombre;
    private Long solicitanteId;
    private String solicitanteNombre;
    private Long propietarioAmbienteId;
    private String propietarioAmbienteNombre;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String proposito;
    private Integer numeroParticipantes;
    private TipoActividad tipoActividad;
    private String observacionesSolicitud;
    private EstadoPrestamoAmbiente estado;
    private String observacionesDevolucion;
    private Integer estadoDevolucionAmbiente;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaDevolucion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
}
