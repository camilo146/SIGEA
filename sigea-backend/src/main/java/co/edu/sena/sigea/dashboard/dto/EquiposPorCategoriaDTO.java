package co.edu.sena.sigea.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquiposPorCategoriaDTO {
    private String categoriaNombre;
    private long cantidad;
}
