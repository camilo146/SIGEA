package co.edu.sena.sigea.usuario.dto;

//Data Transfer Object actualizar datos de un usuario  existente 
/*
Campos que se puden actualizar 
nombreCompleto, tipoDocumento, numeroDocumento, correoElectronico, 
programaFormacion, numeroFicha
*/

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

public class UsuarioActualizadoDTO {
    //Nombre complerto obligatorio
    @NotBlank(message="El nombre no debe estar vacio")
    private String nombreCompleto;

    //tipo de documento }
    @NotNull(message="El tipo de documento es obligatorio")
    private TipoDocumento tipoDocumento;

    //Numero de documento 
    @NotBlank(message="El numero de documento no debe estar vacio")
    private String numeroDocumento;

    //Correo con formato valido 
    @Email(message="El correo debe tener un formato valido")
    private String correoElectronico;

    //Numero de telefono 
    @Size(max=10, message="El numero de telefono no debe tener mas de 10 caracteres")
    private String numeroTelefono;

    //Programa  de formacion 
    @Size(max=200, message="El programa de formacion no debe tener mas de 200  caracteres")
    private String programaFormacion;

    //Numero de ficha 
    @Size(max=10, message="El numero de ficha no debe tener mas de 10 caracteres")
    private String numeroFicha;
    
}