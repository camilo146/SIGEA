package co.edu.sena.sigea.mantenimiento.dto;

// =============================================================================
// DTO: MantenimientoCrearDTO (ENTRADA)
// =============================================================================
// Datos para registrar un mantenimiento (preventivo o correctivo).
// RF-MAN-01, RN-11: tipo, descripción, fecha, responsable obligatorios.
// =============================================================================

import java.time.LocalDate;

import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MantenimientoCrearDTO {

    @NotNull(message = "El ID del equipo es obligatorio")
    private Long equipoId;

    @NotNull(message = "El tipo de mantenimiento es obligatorio")
    private TipoMantenimiento tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000)
    private String descripcion;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    private LocalDate fechaFin;

    @NotBlank(message = "El responsable es obligatorio")
    @Size(max = 200)
    private String responsable;

    @Size(max = 2000)
    private String observaciones;
}
