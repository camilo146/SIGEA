package co.edu.sena.sigea.mantenimiento.dto;

// =============================================================================
// DTO: MantenimientoCerrarDTO (ENTRADA)
// =============================================================================
// Datos para cerrar un mantenimiento (registrar fecha fin y opcionalmente observaciones).
// =============================================================================

import java.time.LocalDate;

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
public class MantenimientoCerrarDTO {

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    @Size(max = 2000)
    private String observaciones;
}
