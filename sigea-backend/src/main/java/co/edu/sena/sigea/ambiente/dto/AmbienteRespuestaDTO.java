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

// Dta de respueta para el detalle de un ambiente, incluye informacion de su
// ambiente padre y sub-ubicaciones hijas (si las tiene)
public class AmbienteRespuestaDTO {

    private long id;

    private String nombre;

    private String ubicacion;

    private String descripcion;

    private String direccion;

    private Long instructorResponsableId;

    private String instructorResponsableNombre;

    private Long propietarioId;

    private String propietarioNombre;

    /** IDs de usuarios adicionales con acceso de gestión al ambiente. */
    private List<Long> encargadoIds;

    /** Resumen de usuarios encargados adicionales. */
    private List<EncargadoResumenDTO> encargados;

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
