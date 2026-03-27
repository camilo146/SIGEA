package co.edu.sena.sigea.ambiente.dto;

import java.time.LocalDateTime;
import java.util.List;

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
public class AmbienteRespuestaDTO {

    private long id;

    private String nombre;

    private String ubicacion;

    private String descripcion;

    private String direccion;

    private Long instructorResponsableId;

    private String instructorResponsableNombre;

    /** ID del ambiente padre (null si es una ubicación raíz). */
    private Long padreId;

    /** Nombre del ambiente padre (null si es una ubicación raíz). */
    private String padreNombre;

    /**
     * Sub-ubicaciones hijas de esta ubicación. 
     * Se incluye solo en consultas de detalle/listado jerarquizado.
     */
    private List<SubUbicacionResumenDTO> subUbicaciones;

    private Boolean activo;

    private String rutaFoto;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;
}
