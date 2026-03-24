package co.edu.sena.sigea.transferencia.controller;

// =============================================================================
// CONTROLADOR: TransferenciaControlador
// =============================================================================
// Endpoints REST para transferencias de equipos entre ambientes.
// BASE URL: /api/v1/transferencias
// RF-AMB-04, RN-10.
// =============================================================================

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.transferencia.dto.TransferenciaCrearDTO;
import co.edu.sena.sigea.transferencia.dto.TransferenciaRespuestaDTO;
import co.edu.sena.sigea.transferencia.service.TransferenciaServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/transferencias")
public class TransferenciaControlador {

    private final TransferenciaServicio transferenciaServicio;

    public TransferenciaControlador(TransferenciaServicio transferenciaServicio) {
        this.transferenciaServicio = transferenciaServicio;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<TransferenciaRespuestaDTO> crear(
            @Valid @RequestBody TransferenciaCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        TransferenciaRespuestaDTO respuesta = transferenciaServicio.crear(dto, correo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransferenciaRespuestaDTO>> listarTodos() {
        return ResponseEntity.ok(transferenciaServicio.listarTodos());
    }

    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransferenciaRespuestaDTO>> listarPorEquipo(@PathVariable Long equipoId) {
        return ResponseEntity.ok(transferenciaServicio.listarPorEquipo(equipoId));
    }

    @GetMapping("/inventario-origen/{instructorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransferenciaRespuestaDTO>> listarPorInstructorOrigen(@PathVariable Long instructorId) {
        return ResponseEntity.ok(transferenciaServicio.listarPorInstructorOrigen(instructorId));
    }

    @GetMapping("/inventario-destino/{instructorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransferenciaRespuestaDTO>> listarPorInstructorDestino(@PathVariable Long instructorId) {
        return ResponseEntity.ok(transferenciaServicio.listarPorInstructorDestino(instructorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TransferenciaRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(transferenciaServicio.buscarPorId(id));
    }
}
