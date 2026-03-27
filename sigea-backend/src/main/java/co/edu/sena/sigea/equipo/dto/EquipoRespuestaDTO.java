package co.edu.sena.sigea.equipo.dto;

import java.time.LocalDateTime;
import java.util.List;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.TipoUsoEquipo;
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
public class EquipoRespuestaDTO {

    private Long id;

    private String nombre;

    private String descripcion;

    private String codigoUnico;

    private Long categoriaId;

    private String categoriaNombre;

    private Long ambienteId;

    private String ambienteNombre;

    /** ID de la sub-ubicación asignada (null si no aplica). */
    private Long subUbicacionId;

    /** Nombre de la sub-ubicación asignada (null si no aplica). */
    private String subUbicacionNombre;

    private Long propietarioId;

    private String propietarioNombre;

    private Long inventarioActualInstructorId;

    private String inventarioActualInstructorNombre;

    private EstadoEquipo estado;

    private Integer cantidadTotal;

    private Integer cantidadDisponible;

    private TipoUsoEquipo tipoUso;

    private Integer umbralMinimo;

    private Boolean activo;

    private List<FotoEquipoRespuestaDTO> fotos;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

}
