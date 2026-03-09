package co.edu.sena.sigea.usuario.dto;

//Data Transfer Object para cambiar el rol de usuario


import co.edu.sena.sigea.common.enums.Rol;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter 
@Setter 
@NoArgsConstructor
@AllArgsConstructor

public class UsuarioCambiarRolDTO {


    //nuevo rol a asignar 
    @NotNull(message="El rol es obligatorio")
    private Rol nuevoRol;
    
}
