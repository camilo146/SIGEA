package co.edu.sena.sigea.seguridad.dto;

import jakarta.validation.constraints.Email;     // Valida formato de correo
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;  // Constructor con todos los campos
import lombok.Getter;              // Genera getters
import lombok.NoArgsConstructor;   // Constructor vacío (necesario para deserialización JSON)
import lombok.Setter;             // Genera setters

@Getter //Genera Gettets para todos los campos
@Setter //Genera Setters para todos los campos
@NoArgsConstructor //Genera un constructor vacío
@AllArgsConstructor //Genera un constructor con todos los campos

public class LoginDTO {


    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El formato del correo no es valido ")
    private String correoElectronico;

    @NotBlank(message = "La contraseña es obligatoria")
    private String contrasena;

}
