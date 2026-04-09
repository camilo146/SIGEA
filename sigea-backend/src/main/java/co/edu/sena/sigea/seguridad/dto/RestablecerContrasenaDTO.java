package co.edu.sena.sigea.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerContrasenaDTO {

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato valido")
    private String correo;

    @NotBlank(message = "El codigo es obligatorio")
    @Size(min = 6, max = 6, message = "El codigo debe tener 6 digitos")
    private String codigo;

    @NotBlank(message = "La nueva contrasena es obligatoria")
    @Size(min = 8, message = "La nueva contrasena debe tener al menos 8 caracteres")
    private String nuevaContrasena;
}