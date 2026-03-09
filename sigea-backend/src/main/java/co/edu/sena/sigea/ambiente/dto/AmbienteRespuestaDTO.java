package co.edu.sena.sigea.ambiente.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter //genera los getters para todos lo campos 
@Setter //genera los setters para todos los campos
@NoArgsConstructor//genera un constructor vacio
@AllArgsConstructor//genera un constructor con todos los campos
@Builder//genera un builder para facilitar la creacion de objetos
public class AmbienteRespuestaDTO {

    private long id;

    private String nombre;

    private String ubicacion;

    private String descripcion;

    private Long instructorResponsableId;

    private String instructorResponsableNombre;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion; 
}


