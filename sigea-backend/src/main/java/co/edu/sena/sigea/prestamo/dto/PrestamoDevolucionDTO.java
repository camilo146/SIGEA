package co.edu.sena.sigea.prestamo.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoDevolucionDTO {

    @Valid
    @NotEmpty(message = "Debes registrar la devolución de al menos un equipo")
    private List<PrestamoDevolucionDetalleDTO> detalles;
}