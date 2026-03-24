package co.edu.sena.sigea.mantenimiento.controller;

// =============================================================================
// CONTROLADOR: MantenimientoControlador
// =============================================================================
// Endpoints REST para mantenimientos de equipos (preventivo/correctivo).
// BASE URL: /api/v1/mantenimientos
// RF-MAN-01, RN-11.
// =============================================================================

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCerrarDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCrearDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoRespuestaDTO;
import co.edu.sena.sigea.mantenimiento.service.MantenimientoServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/mantenimientos")
public class MantenimientoControlador {

    private final MantenimientoServicio mantenimientoServicio;

    public MantenimientoControlador(MantenimientoServicio mantenimientoServicio) {
        this.mantenimientoServicio = mantenimientoServicio;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<MantenimientoRespuestaDTO> crear(@Valid @RequestBody MantenimientoCrearDTO dto) {
        MantenimientoRespuestaDTO respuesta = mantenimientoServicio.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<MantenimientoRespuestaDTO> cerrar(
            @PathVariable Long id,
            @Valid @RequestBody MantenimientoCerrarDTO dto) {

        return ResponseEntity.ok(mantenimientoServicio.cerrar(id, dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoRespuestaDTO>> listar(
            @RequestParam(required = false) Long equipoId,
            @RequestParam(required = false) TipoMantenimiento tipo,
            @RequestParam(required = false) Boolean enCurso) {

        if (Boolean.TRUE.equals(enCurso)) {
            return ResponseEntity.ok(mantenimientoServicio.listarEnCurso());
        }
        if (tipo != null) {
            return ResponseEntity.ok(mantenimientoServicio.listarPorTipo(tipo));
        }
        if (equipoId != null) {
            return ResponseEntity.ok(mantenimientoServicio.listarPorEquipo(equipoId));
        }
        return ResponseEntity.ok(mantenimientoServicio.listarTodos());
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoRespuestaDTO>> listarPorEquipo(@PathVariable Long equipoId) {
        return ResponseEntity.ok(mantenimientoServicio.listarPorEquipo(equipoId));
    }

    @GetMapping("/tipo/{tipo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoRespuestaDTO>> listarPorTipo(@PathVariable TipoMantenimiento tipo) {
        return ResponseEntity.ok(mantenimientoServicio.listarPorTipo(tipo));
    }

    @GetMapping("/en-curso")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MantenimientoRespuestaDTO>> listarEnCurso() {
        return ResponseEntity.ok(mantenimientoServicio.listarEnCurso());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MantenimientoRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mantenimientoServicio.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<MantenimientoRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MantenimientoCrearDTO dto) {
        return ResponseEntity.ok(mantenimientoServicio.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        mantenimientoServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
