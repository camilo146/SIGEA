package co.edu.sena.sigea.reserva.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO para marcar una reserva como "equipo recogido" y registrar la hora de devolución.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservaEquipoRecogidoDTO {

    @NotNull(message = "La fecha/hora de devolución es obligatoria")
    @Future(message = "La fecha de devolución debe ser futura")
    private LocalDateTime fechaHoraDevolucion;
}
