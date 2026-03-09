package co.edu.sena.sigea.categoria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//esto es un POJO solo tiene campos con su getters y setters
//no contiene logica solo se encargan de transportar datos 
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CategoriaCrearDTO{
    // si el front manda un nombre vacio o nulo, se rechaza la peticion o un erroe 400
    @NotBlank(message="El nombre de la categoria es obligatorio")
    @Size(min = 2, max = 100, message ="El nombre de la categoria debe tener entre 2 y 100 caracteres")
    private String nombre;
    // si el front manda una descripcion con mas de 500 caracteres, se rechaza la peticion o un error 400
    @Size (max =500, message="la descripcion de la categoria no puede tener mas de 500 caracteres")
    private String descripcion;
}