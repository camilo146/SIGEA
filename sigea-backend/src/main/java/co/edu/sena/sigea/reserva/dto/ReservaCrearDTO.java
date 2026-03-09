package co.edu.sena.sigea.reserva.dto;

// =============================================================================
// DTO: ReservaCrearDTO (ENTRADA)
// =============================================================================
// Datos que el cliente envía al servidor para crear una reserva anticipada.
//
// RF-RES-01: Reservar equipos con máximo 5 días hábiles de anticipación.
// RF-RES-02: Si no recoge en 2 horas desde fechaHoraInicio, la reserva se cancela.
//
// El usuario autenticado se obtiene del TOKEN JWT (no del body).
// El servicio calcula fechaHoraFin = fechaHoraInicio + 2 horas.
//
// PRINCIPIOS:
//   SRP: Este DTO solo transporta datos de entrada para crear una reserva.
//   Encapsulamiento: Validaciones declarativas con Bean Validation.
// =============================================================================

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
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
public class ReservaCrearDTO {

    @NotNull(message = "El ID del equipo es obligatorio")
    private Long equipoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    // Hora en que el usuario debe recoger el equipo (inicio de la ventana de reserva).
    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    @Future(message = "La fecha y hora de inicio debe ser en el futuro")
    private LocalDateTime fechaHoraInicio;
}
