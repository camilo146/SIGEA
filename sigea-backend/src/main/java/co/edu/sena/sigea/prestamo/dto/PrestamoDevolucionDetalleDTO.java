package co.edu.sena.sigea.prestamo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoDevolucionDetalleDTO {

    @NotNull(message = "El detalle del préstamo es obligatorio")
    private Long detalleId;

    @NotBlank(message = "Las observaciones de devolución son obligatorias")
    private String observacionesDevolucion;

    @NotNull(message = "La calificación del estado es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 10, message = "La calificación máxima es 10")
    private Integer estadoDevolucion;
}