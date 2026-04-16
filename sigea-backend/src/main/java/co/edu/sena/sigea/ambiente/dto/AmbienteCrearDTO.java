
package co.edu.sena.sigea.ambiente.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter // genera getter para todos lo campos
@Setter // genera setter para todos los campos
@NoArgsConstructor // genera cponstructor vacio
@AllArgsConstructor // genera constructor con todos lo campos

// Dto para la creacion de un nuevo ambiente, incluye validaciones de campos
// obligatorios y limites de caracteres
public class AmbienteCrearDTO {

    @NotBlank(message = "El nombre del ambiente es obligatorio")
    @Size(max = 100, message = "El nombre del ambiente no debe tener mas de 100 caracteres")
    private String nombre;

    @Size(max = 200, message = "La ubicacion no debe tener mas de 200 caracteres")
    private String ubicacion;

    @Size(max = 500, message = "La descripcion no debe tener mas de 500 caracteres")
    private String descripcion;

    @Size(max = 250, message = "La direccion no debe tener mas de 250 caracteres")
    private String direccion;

    /**
     * Identificador del ambiente padre. Si se proporciona, esta ubicación
     * se crea como sub-ubicación del ambiente padre indicado.
     */
    private Long padreId;

    /**
     * Obligatorio para ADMIN; si es INSTRUCTOR puede ser null y se asigna él mismo.
     */
    private Long idInstructorResponsable;

    /**
     * Usuarios adicionales que podrán administrar la ubicación y atender reservas.
     */
    private List<Long> encargadoIds;

}
