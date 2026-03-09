package co.edu.sena.sigea.usuario.dto;

//Data Transfer Object para que un admin cree un nuevo usuario


import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter //Genera Getters para todos los campos      
@Setter //Genera Setters para todos los campos
@NoArgsConstructor //Genera un constructor sin argumentos
@AllArgsConstructor //Genera un constructor con todos los argumentos

public class UsuarioCrearDTO{
    //Nombre completo obligatorio
@NotBlank(message="El nombre no puede estar vacío")
@Size (min=5, max=150, message="El nombre debe tener entre 5 y 150 caracteres")
private String nombreCompleto;

//Tipo de documento 
@NotNull(message = "El tipo de documento es obligatorio")
private TipoDocumento tipoDocumento;

//Numero de documento obligatorio
@NotBlank(message="El numero de  documento no puede estar vacío")
@Size(min=5, max=10, message="El numero de documento debe tener entre 5 y 10 caracteres")
private String numeroDocumento;

//Correo electronico obligatorio y con formato valido
@Email(message="El correo debe tener un formato valido")
private String correoElectronico;

//Programa de formacion 
@Size(min=50, max=100, message="El programa de formacion debe tener entre 50 o mas caracteres")
private String programaFormacion;

//numero de ficha 
@Size(max=10, message="El numero de ficha no de ser mayor a 10 caracteres")
private String numeroFicha;

//Telefono opcional
@Size(max=20, message="El teléfono no puede tener más de 20 caracteres")
private String telefono;

//Contraseña obligatoria
@NotBlank(message="La contraseña es obligatoria")
@Size(min=8, max=100, message="La contraseña debe tener entre 8 y 100 caracteres")
private String contrasena;

//Rol, este rolo lo elije el admin
@NotNull(message="El rol es obligatorio")
private Rol rol;
    
}
