package co.edu.sena.sigea.ambiente.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncargadoResumenDTO {
    private Long id;
    private String nombreCompleto;
    private String correoElectronico;
    private String rol;
}
