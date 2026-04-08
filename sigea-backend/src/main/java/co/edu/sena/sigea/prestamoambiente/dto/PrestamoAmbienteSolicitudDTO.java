package co.edu.sena.sigea.prestamoambiente.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import co.edu.sena.sigea.prestamoambiente.enums.TipoActividad;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoAmbienteSolicitudDTO {

    @NotNull(message = "El ID del ambiente es obligatorio")
    private Long ambienteId;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser futura")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime horaFin;

    @NotBlank(message = "El propósito es obligatorio")
    private String proposito;

    @NotNull(message = "El número de participantes es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 participante")
    private Integer numeroParticipantes;

    @NotNull(message = "El tipo de actividad es obligatorio")
    private TipoActividad tipoActividad;

    private String observacionesSolicitud;
}
