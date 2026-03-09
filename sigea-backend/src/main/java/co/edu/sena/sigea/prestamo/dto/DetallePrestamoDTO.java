package co.edu.sena.sigea.prestamo.dto;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;




@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DetallePrestamoDTO {


    @NotNull(message="El id del equipo es obligatorio")
    private Long equipoId;

    @NotNull(message="La cantidad es obligatoria")
    @Min(value=1, message="La cantidad debe ser al menos 1")
    private Integer cantidad;
    
    
}
