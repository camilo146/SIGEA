package co.edu.sena.sigea.prestamoambiente.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoAmbienteDevolucionDTO {

    @NotBlank(message = "Las observaciones de devolución son obligatorias")
    private String observacionesDevolucion;

    @NotNull(message = "El estado de devolución del ambiente es obligatorio")
    @Min(value = 1, message = "El estado mínimo es 1")
    @Max(value = 10, message = "El estado máximo es 10")
    private Integer estadoDevolucionAmbiente;
}
