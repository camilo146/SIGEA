package co.edu.sena.sigea.observacion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObservacionEquipoCrearDTO {

    @NotNull(message = "El ID del préstamo es obligatorio")
    private Long prestamoId;

    @NotNull(message = "El ID del equipo es obligatorio")
    private Long equipoId;

    @NotBlank(message = "Las observaciones son obligatorias")
    private String observaciones;

    @NotNull(message = "El estado de devolución es obligatorio")
    @Min(value = 1, message = "El estado de devolución mínimo es 1")
    @Max(value = 10, message = "El estado de devolución máximo es 10")
    private Integer estadoDevolucion;
}
