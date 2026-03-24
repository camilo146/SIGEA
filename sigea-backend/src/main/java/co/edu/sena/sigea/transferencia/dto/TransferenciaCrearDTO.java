package co.edu.sena.sigea.transferencia.dto;

// =============================================================================
// DTO: TransferenciaCrearDTO (ENTRADA)
// =============================================================================
// Datos para registrar una transferencia de equipo entre ambientes.
// RF-AMB-04: Transferir equipos entre ambientes con origen, destino, equipo,
//            fecha, admin que autoriza, motivo.
// El administrador que autoriza se obtiene del token JWT (usuario autenticado).
// =============================================================================

import java.time.LocalDateTime;

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
public class TransferenciaCrearDTO {

    @NotNull(message = "El ID del equipo es obligatorio")
    private Long equipoId;

    @NotNull(message = "El ID del instructor destino del inventario es obligatorio")
    private Long instructorDestinoId;

    private Long ubicacionDestinoId;

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad = 1;

    private String motivo;

    @NotNull(message = "La fecha de transferencia es obligatoria")
    private LocalDateTime fechaTransferencia;
}
