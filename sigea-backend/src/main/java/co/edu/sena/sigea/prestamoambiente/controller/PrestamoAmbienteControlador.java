package co.edu.sena.sigea.prestamoambiente.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteDevolucionDTO;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteRespuestaDTO;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteSolicitudDTO;
import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.service.PrestamoAmbienteServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/prestamos-ambientes")
@RequiredArgsConstructor
@Validated
public class PrestamoAmbienteControlador {

    private final PrestamoAmbienteServicio servicio;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> solicitar(
            @Valid @RequestBody PrestamoAmbienteSolicitudDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.solicitar(dto, userDetails.getUsername()));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoAmbienteRespuestaDTO>> listar(
            @AuthenticationPrincipal UserDetails userDetails) {
        String correoUsuario = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(servicio.listarVisiblesParaUsuario(correoUsuario));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.buscarPorId(id));
    }

    @GetMapping("/ambiente/{ambienteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoAmbienteRespuestaDTO>> listarPorAmbiente(@PathVariable Long ambienteId) {
        return ResponseEntity.ok(servicio.listarPorAmbiente(ambienteId));
    }

    @GetMapping("/mis-solicitudes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoAmbienteRespuestaDTO>> misSolicitudes(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(servicio.listarMisSolicitudes(userDetails.getUsername()));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR')")
    public ResponseEntity<List<PrestamoAmbienteRespuestaDTO>> listarPorEstado(
            @PathVariable EstadoPrestamoAmbiente estado) {
        return ResponseEntity.ok(servicio.listarPorEstado(estado));
    }

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(servicio.aprobar(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> rechazar(
            @PathVariable Long id,
            @RequestParam String motivo,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(servicio.rechazar(id, userDetails.getUsername(), motivo));
    }

    @PutMapping("/{id}/devolver")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> registrarDevolucion(
            @PathVariable Long id,
            @Valid @RequestBody PrestamoAmbienteDevolucionDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(servicio.registrarDevolucion(id, dto, userDetails.getUsername()));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoAmbienteRespuestaDTO> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(servicio.cancelar(id, userDetails.getUsername()));
    }
}
