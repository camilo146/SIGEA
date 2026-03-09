package co.edu.sena.sigea.common.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
// DTO para representar la estructura de una respuesta de error en la API.
@Getter 
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorRespuesta {
    
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String ruta;
    private List<String> detalles ; // Detalles adicionales de errores, como validaciones fallidas
}