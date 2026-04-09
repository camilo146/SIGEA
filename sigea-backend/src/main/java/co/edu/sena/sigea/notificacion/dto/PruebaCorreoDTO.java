package co.edu.sena.sigea.notificacion.dto;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PruebaCorreoDTO {

    @Email(message = "El destinatario debe ser un correo válido")
    private String destinatario;
}