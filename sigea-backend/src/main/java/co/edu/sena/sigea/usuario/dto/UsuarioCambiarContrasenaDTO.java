package co.edu.sena.sigea.usuario.dto;


//Data Transfer Object para cambiar la contraseña del usuario 


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

public class UsuarioCambiarContrasenaDTO {

    //Contraseña actual obligatoria
    @NotBlank(message="La contraseña actual es obligatoria")
    private String contrasenaActual;

    //Nueva contraseña obligatoria con minimo de 8 caracteres
    @NotBlank(message="La nueva contraseña es obligatoria")
    @Size(min=8, message="La nueva contraseña debe tener al menos 8 caracteres")
    private String nuevaContrasena;
    
}
