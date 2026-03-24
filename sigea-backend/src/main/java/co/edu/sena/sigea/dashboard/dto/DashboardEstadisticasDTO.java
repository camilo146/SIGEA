package co.edu.sena.sigea.dashboard.dto;

// =============================================================================
// DTO: DashboardEstadisticasDTO (SALIDA)
// =============================================================================
// Resumen de estadísticas para el panel de control (dashboard).
// Usado por el frontend para mostrar tarjetas y gráficos.
// =============================================================================

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
public class DashboardEstadisticasDTO {

    private long totalEquipos;
    private long equiposActivos;
    private long totalCategorias;
    private long totalAmbientes;
    private long totalUsuarios;

    private long prestamosSolicitados;
    private long prestamosActivos;
    private long prestamosEnMora;
    private long prestamosDevueltos;

    private long reservasActivas;

    private long mantenimientosEnCurso;
    private long totalTransferencias;

    private long equiposStockBajo;
}
