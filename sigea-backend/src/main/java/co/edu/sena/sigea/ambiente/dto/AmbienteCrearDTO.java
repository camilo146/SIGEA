

package co.edu.sena.sigea.ambiente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter//genera getter para todos lo campos 
@Setter //genera setter para todos los campos 
@NoArgsConstructor//genera cponstructor vacio
@AllArgsConstructor//genera constructor con todos lo campos 

public class AmbienteCrearDTO {

    @NotBlank(message="El nombre del ambiente es obligatorio")
    @Size(max=100, message="El nombre del ambiente no debe tener mas de 100 caracteres")
    private String nombre;

    @Size(max=200, message="La ubicacion no debe tener mas de 200 caracteres")
    private String ubicacion;

    @Size(max=500, message="La descripcion no debe tener mas de 500 caracteres")
    private String descripcion;

    @NotNull(message="El ID del instrutor responsable es obligatorio")
    private Long idInstructorResponsable;
    
}
