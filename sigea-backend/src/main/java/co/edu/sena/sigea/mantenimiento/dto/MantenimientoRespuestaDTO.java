package co.edu.sena.sigea.mantenimiento.dto;

// =============================================================================
// DTO: MantenimientoRespuestaDTO (SALIDA)
// =============================================================================
// Datos de un mantenimiento enviados al cliente.
// =============================================================================

import java.time.LocalDate;
import java.time.LocalDateTime;

import co.edu.sena.sigea.common.enums.TipoMantenimiento;
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
public class MantenimientoRespuestaDTO {

    private Long id;

    private Long equipoId;
    private String nombreEquipo;
    private String codigoEquipo;

    private TipoMantenimiento tipo;
    private String descripcion;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private String responsable;
    private String observaciones;

    private LocalDateTime fechaCreacion;
}
