package co.edu.sena.sigea.prestamo.dto;

// =============================================================================
// DTO: DetallePrestamoRespuestaDTO (SALIDA)
// =============================================================================
// Datos de UNA LÍNEA del préstamo que el servidor envía al cliente.
// Representa un equipo específico dentro de un préstamo.
//
// DIFERENCIA con DetallePrestamoDTO (entrada):
//   Entrada → solo equipoId + cantidad (lo que el cliente envía)
//   Salida  → datos enriquecidos: nombre, código, condición, si fue devuelto, etc.
//
// ¿Por qué necesitamos nombreEquipo y codigoEquipo si ya tenemos equipoId?
//   Porque el cliente NO debería tener que hacer otra petición GET /equipos/{id}
//   para mostrar el nombre en pantalla. El servidor devuelve todo junto → 1 petición.
//   Esto es el patrón "response enrichment" (enriquecimiento de respuesta).
//
// ¿Por qué NO tiene @NotNull ni otras validaciones?
//   Porque es un DTO de SALIDA. Lo construye el SERVIDOR, no el cliente.
//   Las validaciones (@NotNull, @NotBlank) solo tienen sentido en DTOs de ENTRADA.
//
// ¿Por qué @Builder?
//   En el Servicio, construimos el DTO así:
//     DetallePrestamoRespuestaDTO.builder()
//         .id(detalle.getId())
//         .nombreEquipo(detalle.getEquipo().getNombre())
//         .build();
//   Es más legible que un constructor con 10 parámetros.
// =============================================================================

import co.edu.sena.sigea.common.enums.EstadoCondicion;
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
public class DetallePrestamoRespuestaDTO {

    // =========================================================================
    // CAMPO: id
    // =========================================================================
    // ID del registro en la tabla detalle_prestamo.
    // Lo necesita el frontend para luego hacer PATCH
    // /prestamos/{id}/devolver-item/{detalleId}
    // =========================================================================
    private Long id;

    // =========================================================================
    // CAMPO: equipoId
    // =========================================================================
    // ID del equipo prestado. Sirve para enlazar con el módulo de equipos si se
    // necesita.
    // =========================================================================
    private Long equipoId;

    // =========================================================================
    // CAMPOS: nombreEquipo, codigoEquipo
    // =========================================================================
    // Datos del equipo incluidos directamente para no requerir otra petición HTTP.
    // - nombreEquipo: "Laptop Dell Latitude"
    // - codigoEquipo: "LAP-DELL-001"
    // =========================================================================
    private String nombreEquipo;
    private String codigoEquipo;

    // =========================================================================
    // CAMPO: cantidad
    // =========================================================================
    // Cuántas unidades de ese equipo se incluyeron en esta línea del préstamo.
    // Ej: 3 multímetros Fluke.
    // =========================================================================
    private Integer cantidad;

    private TipoUsoEquipo tipoUso;

    // =========================================================================
    // CAMPO: estadoEquipoEntrega
    // =========================================================================
    // Condición del equipo cuando fue ENTREGADO al usuario.
    // Valores posibles (enum EstadoCondicion): BUENO, REGULAR, DAÑADO, etc.
    // Nullable hasta que el admin registre la salida (RF-PRE-09).
    // =========================================================================
    private EstadoCondicion estadoEquipoEntrega;

    // =========================================================================
    // CAMPO: observacionesEntrega
    // =========================================================================
    // Notas del admin al momento de entregar. Ej: "Pantalla tiene una rayita
    // pequeña"
    // Permite documentar el estado ANTES de que el usuario lo lleve.
    // RN-03: "Documentar estado del equipo al prestar"
    // =========================================================================
    private String observacionesEntrega;

    // =========================================================================
    // CAMPO: estadoEquipoDevolucion
    // =========================================================================
    // Condición del equipo cuando fue DEVUELTO por el usuario.
    // Nullable hasta que el usuario devuelva el equipo (RF-PRE-04).
    // =========================================================================
    private EstadoCondicion estadoEquipoDevolucion;

    // =========================================================================
    // CAMPO: observacionesDevolucion
    // =========================================================================
    // Notas del admin al momento de recibir la devolución.
    // Ej: "Pantalla rota - se generará reporte de daño"
    // RN-04: "Documentar estado del equipo al devolver"
    // =========================================================================
    private String observacionesDevolucion;

    // =========================================================================
    // CAMPO: devuelto
    // =========================================================================
    // false → el equipo todavía está en manos del usuario.
    // true → el equipo fue devuelto y reincorporado al stock.
    //
    // En el Servicio, cuando TODOS los detalles tienen devuelto=true,
    // el préstamo cambia de estado ACTIVO → DEVUELTO.
    // Lógica: prestamo.getDetalles().stream().allMatch(DetallePrestamo::isDevuelto)
    // =========================================================================
    private Boolean devuelto;
}
