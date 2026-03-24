package co.edu.sena.sigea.reserva.dto;

// =============================================================================
// DTO: ReservaRespuestaDTO (SALIDA)
// =============================================================================
// Datos de la reserva que el servidor envía al cliente.
// Incluye datos enriquecidos (nombre usuario, nombre equipo) para evitar
// llamadas adicionales desde el frontend.
//
// PRINCIPIOS:
//   SRP: Solo representa la estructura de respuesta de una reserva.
//   Encapsulamiento: Builder para construcción inmutable y legible.
// =============================================================================

import java.time.LocalDateTime;

import co.edu.sena.sigea.common.enums.EstadoReserva;
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
public class ReservaRespuestaDTO {

    private Long id;

    private Long usuarioId;
    private String nombreUsuario;
    private String correoUsuario;

    private Long equipoId;
    private String nombreEquipo;
    private String codigoEquipo;
    private TipoUsoEquipo tipoUso;

    private Integer cantidad;

    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    private EstadoReserva estado;

    private LocalDateTime fechaCreacion;
}
