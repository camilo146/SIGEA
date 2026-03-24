package co.edu.sena.sigea.dashboard.controller;

// =============================================================================
// CONTROLADOR: DashboardControlador
// =============================================================================
// Expone estadísticas agregadas para el panel de control.
// BASE URL: /api/v1/dashboard
// =============================================================================

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.dashboard.dto.DashboardEstadisticasDTO;
import co.edu.sena.sigea.dashboard.dto.EquiposPorCategoriaDTO;
import co.edu.sena.sigea.dashboard.dto.PrestamosPorMesDTO;
import co.edu.sena.sigea.dashboard.service.DashboardServicio;

@RestController
@RequestMapping("/dashboard")
public class DashboardControlador {

    private final DashboardServicio dashboardServicio;

    public DashboardControlador(DashboardServicio dashboardServicio) {
        this.dashboardServicio = dashboardServicio;
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DashboardEstadisticasDTO> obtenerEstadisticas() {
        return ResponseEntity.ok(dashboardServicio.obtenerEstadisticas());
    }

    @GetMapping("/grafico-prestamos-por-mes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamosPorMesDTO>> prestamosPorMes() {
        return ResponseEntity.ok(dashboardServicio.prestamosPorMes());
    }

    @GetMapping("/grafico-equipos-por-categoria")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquiposPorCategoriaDTO>> equiposPorCategoria() {
        return ResponseEntity.ok(dashboardServicio.equiposPorCategoria());
    }
}
