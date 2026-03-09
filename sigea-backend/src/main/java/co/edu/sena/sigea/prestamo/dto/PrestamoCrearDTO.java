package co.edu.sena.sigea.prestamo.dto;

// =============================================================================
// DTO: PrestamoCrearDTO (ENTRADA)
// =============================================================================
// Datos que el CLIENTE (navegador/app) envía al servidor para crear un préstamo.
//
// IMPORTANTE — ¿Por qué NO está usuarioSolicitanteId aquí?
//   Porque el usuario autenticado se obtiene del TOKEN JWT en el servidor.
//   Si lo pusiéramos en el body, cualquier usuario podría enviar el ID de
//   otra persona y hacer préstamos en su nombre → vulnerabilidad de seguridad.
//   El Controller usa: @AuthenticationPrincipal UserDetails ud → ud.getUsername()
//   para obtener el correo del usuario real. El Servicio busca al usuario por correo.
//
// ¿Por qué LocalDateTime y no String?
//   @Future solo funciona con tipos de fecha de Java (LocalDateTime, LocalDate, etc.)
//   Si usas String, la anotación @Future es IGNORADA por el framework.
//   LocalDateTime = "2026-03-15T14:30:00" (sin zona horaria)
// =============================================================================

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrestamoCrearDTO {

    // =========================================================================
    // CAMPO: fechaHoraDevolucionEstimada
    // =========================================================================
    // Fecha y hora límite para devolver los equipos.
    //
    // @Future → valida que la fecha sea UN MOMENTO FUTURO (no hoy, no ayer).
    //   Funciona porque es LocalDateTime (tipo de fecha real de Java).
    //   Si el cliente envía "2020-01-01T00:00:00", falla la validación → HTTP 400.
    //
    // @NotNull → campo obligatorio.
    // =========================================================================
    @NotNull(message = "La fecha de devolución estimada es obligatoria")
    @Future(message = "La fecha de devolución estimada debe ser en el futuro")
    private LocalDateTime fechaHoraDevolucionEstimada;

    // =========================================================================
    // CAMPO: observacionesGenerales
    // =========================================================================
    // Notas opcionales del usuario. Ejemplo: "Para práctica del viernes"
    // @Size → máximo 500 caracteres para evitar textos exagerados.
    // =========================================================================
    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observacionesGenerales;

    // =========================================================================
    // CAMPO: detalles
    // =========================================================================
    // Lista de equipos que se quieren pedir prestados.
    //
    // @NotEmpty → la lista no puede estar vacía. Un préstamo SIN equipos no tiene sentido.
    // @Valid → le dice a Spring que también valide CADA elemento de la lista.
    //   Sin @Valid, las anotaciones dentro de DetallePrestamoDTO son ignoradas.
    //
    // Cada DetallePrestamoDTO contiene: equipoId + cantidad.
    // =========================================================================
    @NotEmpty(message = "Debe incluir al menos un equipo en el préstamo")
    @Valid
    private List<DetallePrestamoDTO> detalles;
}
