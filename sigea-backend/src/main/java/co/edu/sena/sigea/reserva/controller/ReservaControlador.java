package co.edu.sena.sigea.reserva.controller;

// =============================================================================
// CONTROLADOR: ReservaControlador
// =============================================================================
// Expone los endpoints REST del módulo de reservas anticipadas.
// Recibe peticiones HTTP → delega al ReservaServicio → retorna la respuesta.
// No contiene lógica de negocio (SRP).
//
// BASE URL: /api/v1/reservas
//
// SEGURIDAD:
//   POST /reservas, GET /reservas/mis-reservas, PATCH /reservas/{id}/cancelar → autenticado
//   GET /reservas, GET /reservas/estado/{estado}, GET /reservas/usuario/{id}, GET /reservas/{id} → ADMIN
// =============================================================================

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.reserva.dto.ReservaCrearDTO;
import co.edu.sena.sigea.reserva.dto.ReservaEquipoRecogidoDTO;
import co.edu.sena.sigea.reserva.dto.ReservaRespuestaDTO;
import co.edu.sena.sigea.reserva.service.ReservaServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/reservas")
public class ReservaControlador {

    private final ReservaServicio reservaServicio;

    public ReservaControlador(ReservaServicio reservaServicio) {
        this.reservaServicio = reservaServicio;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaRespuestaDTO> crear(
            @Valid @RequestBody ReservaCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails.getUsername();
        ReservaRespuestaDTO respuesta = reservaServicio.crear(dto, correo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaRespuestaDTO>> listarTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin = userDetails != null && userDetails.getAuthorities() != null
                && userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a) || "ROLE_INSTRUCTOR".equals(a));
        if (isAdmin) {
            return ResponseEntity.ok(reservaServicio.listarTodos());
        }
        String correo = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(reservaServicio.listarMisReservas(correo));
    }

    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaRespuestaDTO>> listarMisReservas(
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails.getUsername();
        return ResponseEntity.ok(reservaServicio.listarMisReservas(correo));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaRespuestaDTO>> listarPorEstado(
            @PathVariable EstadoReserva estado) {

        return ResponseEntity.ok(reservaServicio.listarPorEstado(estado));
    }

    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservaRespuestaDTO>> listarPorUsuario(
            @PathVariable Long usuarioId) {

        return ResponseEntity.ok(reservaServicio.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservaRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(reservaServicio.buscarPorId(id));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails.getUsername();
        reservaServicio.cancelar(id, correo);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/equipo-recogido")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<ReservaRespuestaDTO> marcarEquipoRecogido(
            @PathVariable Long id,
            @Valid @RequestBody ReservaEquipoRecogidoDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails.getUsername();
        ReservaRespuestaDTO respuesta = reservaServicio.marcarEquipoRecogido(id, dto, correo);
        return ResponseEntity.ok(respuesta);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        reservaServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
