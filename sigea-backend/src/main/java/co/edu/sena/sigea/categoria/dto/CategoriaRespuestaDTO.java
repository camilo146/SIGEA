package co.edu.sena.sigea.categoria.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter 
@NoArgsConstructor
@AllArgsConstructor
@Builder
//DTO de respuesta para las categorías de equipos, incluye información básica como nombre, descripción,
// estado activo, y fechas de creación/actualización. Este DTO se utiliza para mostrar la información de las categorías en las respuestas de la API.
public class CategoriaRespuestaDTO {

    private long id; 

    private String nombre;
    
    private String descripcion;

    private Boolean activo;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;



}