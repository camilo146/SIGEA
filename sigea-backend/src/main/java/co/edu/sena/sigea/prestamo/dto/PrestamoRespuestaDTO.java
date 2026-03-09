package co.edu.sena.sigea.prestamo.dto;

// =============================================================================
// DTO: PrestamoRespuestaDTO (SALIDA)
// =============================================================================
// Datos de la CABECERA del préstamo que el servidor envía al cliente.
// Representa el préstamo completo: quién lo pidió, con qué equipos, en qué estado.
//
// PATRÓN CABECERA-DETALLE:
//   Este DTO ES la cabecera. Los equipos van dentro de List<DetallePrestamoRespuestaDTO>.
//   Es como una factura: los datos del cliente van en la cabecera,
//   y los productos van en las líneas (detalles).
//
// ¿Por qué nombres en lugar de solo IDs?
//   - nombreUsuarioSolicitante → el frontend muestra "Juan Perez", no "ID: 42"
//   - nombreAdministradorAprueba → puede ser null si aún no fue aprobado
//   Mismo principio del DetallePrestamoRespuestaDTO: 1 sola petición, datos completos.
//
// ¿Por qué LocalDateTime en los campos de fecha?
//   Porque en BD se guardan como DATETIME.
//   Jackson los convierte automáticamente a String en el JSON de respuesta.
//   Resultado en JSON: "fechaHoraSolicitud": "2026-03-01T10:30:00"
// =============================================================================

import java.time.LocalDateTime;
import java.util.List;

import co.edu.sena.sigea.common.enums.EstadoPrestamo;
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
public class PrestamoRespuestaDTO {

    // =========================================================================
    // CAMPO: id
    // =========================================================================
    // ID del préstamo en la BD. Lo usa el frontend para las operaciones:
    //   PATCH /prestamos/{id}/aprobar
    //   PATCH /prestamos/{id}/registrar-salida
    //   PATCH /prestamos/{id}/registrar-devolucion
    // =========================================================================
    private Long id;

    // =========================================================================
    // DATOS DEL USUARIO SOLICITANTE
    // =========================================================================
    // Incluimos ID + nombre completo + correo.
    // El frontend puede mostrar "Solicitado por: Juan Pérez (juan@sena.edu.co)"
    // sin necesidad de hacer GET /usuarios/{id}.
    // =========================================================================

    // ID del usuario que hizo la solicitud
    private Long usuarioSolicitanteId;

    // Nombre del usuario que hizo la solicitud (nombre + apellido)
    private String nombreUsuarioSolicitante;

    // Correo del solicitante
    private String correoUsuarioSolicitante;

    // =========================================================================
    // DATOS DE LOS ADMINISTRADORES (pueden ser null)
    // =========================================================================
    // - administradorAprueba: null si el préstamo está SOLICITADO (nadie respondió aún)
    // - administradorRecibe:  null si el préstamo no ha sido devuelto aún
    //
    // Solo mostramos el nombre (suficiente para pantalla de consulta).
    // =========================================================================

    // Nombre del admin que aprobó/rechazó (null si aún está SOLICITADO)
    private String nombreAdministradorAprueba;

    // Nombre del admin que recibió la devolución (null si no fue devuelto aún)
    private String nombreAdministradorRecibe;

    // =========================================================================
    // CAMPOS DE FECHA Y HORA (ciclo de vida del préstamo)
    // =========================================================================
    // Cada fecha marca un evento en el ciclo de vida. Algunas pueden ser null
    // si ese evento aún no ocurrió.
    //
    // Ciclo completo:
    //   fechaHoraSolicitud → fechaHoraAprobacion → fechaHoraSalida →
    //   fechaHoraDevolucionEstimada (límite) → fechaHoraDevolucionReal
    // =========================================================================

    // Cuándo el usuario creó la solicitud. Nunca null.
    private LocalDateTime fechaHoraSolicitud;

    // Cuándo el admin aprobó o rechazó. Null si aún está SOLICITADO.
    private LocalDateTime fechaHoraAprobacion;

    // Cuándo los equipos salieron físicamente. Null si aún no se entregaron.
    private LocalDateTime fechaHoraSalida;

    // Fecha límite para devolver. Nunca null (se fija al crear la solicitud).
    private LocalDateTime fechaHoraDevolucionEstimada;

    // Cuándo se devolvieron realmente. Null si aún no fueron devueltos.
    private LocalDateTime fechaHoraDevolucionReal;

    // =========================================================================
    // CAMPO: estado
    // =========================================================================
    // Estado actual del préstamo.
    // Enum EstadoPrestamo: SOLICITADO, APROBADO, ACTIVO, DEVUELTO, RECHAZADO, EN_MORA
    // =========================================================================
    private EstadoPrestamo estado;

    // =========================================================================
    // CAMPO: observacionesGenerales
    // =========================================================================
    // Notas opcionales del usuario al solicitar. Puede ser null.
    // =========================================================================
    private String observacionesGenerales;

    // =========================================================================
    // CAMPO: extensionesRealizadas
    // =========================================================================
    // Cuántas veces se extendió el plazo. Máximo 2 (RN-02).
    // Pasa de 0 → 1 → 2 cada vez que se aprueba una extensión.
    // =========================================================================
    private Integer extensionesRealizadas;

    // =========================================================================
    // CAMPO: detalles
    // =========================================================================
    // Lista de equipos incluidos en este préstamo.
    // Cada elemento es un DetallePrestamoRespuestaDTO con todos los datos del equipo.
    //
    // Equivale a las "líneas" de una factura.
    // El frontend puede iterar esta lista para mostrar la tabla de equipos.
    // =========================================================================
    private List<DetallePrestamoRespuestaDTO> detalles;
}
