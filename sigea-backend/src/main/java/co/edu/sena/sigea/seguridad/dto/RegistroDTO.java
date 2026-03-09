package co.edu.sena.sigea.seguridad.dto;

import co.edu.sena.sigea.common.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
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

//DTO para registrar un nuevo usuario en el sistema, con validaciones de campos 

public class RegistroDTO {

    // 
    @NotBlank(message="El nombre no puede estar vacío")
    @Size (min=2, max=150, message="El nombre debe tener entre 2 y 150 caracteres")
    private String nombre;//El nombre completo del usuario que se registra

    @NotNull(message="El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;// El tipo de ducumento del usuario 

    @NotBlank(message="El numero de documento no puede estar vacio")
    @Size(min=5, max=20, message="El numero de documento debe tener entre 5 y 20 caracteres")
    private String numeroDocumento;// El numero de documento del usuario

    @Email(message="El correo debe tener un formato valido")
    private String correoElectronico;// El correo del usuario

    @Size(max = 200, message = "El programa no puede tener más de 200 caracteres")
    private String programaFormacion;// El programa de formación del usuario

    @Size(max=20, message="El telefono no puede tener mas de 20 caracteres ")
    private String telefono;// El telefono del usuario

    @Size(max=10, message="El numero de ficha no debe ser mayor a 10 caracteres")
    private String numeroFicha;// El numero de ficha del usuario si es aprendiz 

    @NotBlank(message="La contraseña es obligatoria")
    @Size(min=8,  max=100, message="La contraseña debe tener 8 o mas caracteres")
    private String contrasena;// La contraseña del usuario  


   
}