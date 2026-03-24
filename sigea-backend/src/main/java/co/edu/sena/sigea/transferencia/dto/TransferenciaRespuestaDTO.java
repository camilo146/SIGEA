package co.edu.sena.sigea.transferencia.dto;

// =============================================================================
// DTO: TransferenciaRespuestaDTO (SALIDA)
// =============================================================================
// Datos de una transferencia enviados al cliente, con nombres enriquecidos
// para evitar llamadas adicionales desde el frontend.
// =============================================================================

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
public class TransferenciaRespuestaDTO {

    private Long id;

    private Long equipoId;
    private String nombreEquipo;
    private String codigoEquipo;

    private Long inventarioOrigenInstructorId;
    private String nombreInventarioOrigenInstructor;

    private Long inventarioDestinoInstructorId;
    private String nombreInventarioDestinoInstructor;

    private Long propietarioEquipoId;
    private String nombrePropietarioEquipo;

    private Long ubicacionDestinoId;
    private String nombreUbicacionDestino;

    private Integer cantidad;

    private Long administradorAutorizaId;
    private String nombreAdministrador;

    private String motivo;
    private LocalDateTime fechaTransferencia;
    private LocalDateTime fechaCreacion;
}
