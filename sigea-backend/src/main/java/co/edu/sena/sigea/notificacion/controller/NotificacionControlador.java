package co.edu.sena.sigea.notificacion.controller;

import co.edu.sena.sigea.notificacion.dto.NotificacionRespuestaDTO;
import co.edu.sena.sigea.notificacion.service.NotificacionServicio;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notificaciones")
public class NotificacionControlador {

    private final NotificacionServicio notificacionServicio;

    public NotificacionControlador(NotificacionServicio notificacionServicio) {
        this.notificacionServicio = notificacionServicio;
    }

    // GET /notificaciones/usuario/{usuarioId} — solo ADMIN
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<NotificacionRespuestaDTO>> listarPorUsuario(
            @PathVariable Long usuarioId) {
        return ResponseEntity.ok(notificacionServicio.listarPorUsuario(usuarioId));
    }

    // GET /notificaciones/mis-notificaciones — usuario autenticado ve las suyas
    @GetMapping("/mis-notificaciones")
    public ResponseEntity<List<NotificacionRespuestaDTO>> listarMisNotificaciones(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                notificacionServicio.listarMisNotificaciones(userDetails.getUsername()));
    }

    // GET /notificaciones/mis-notificaciones/no-leidas
    @GetMapping("/mis-notificaciones/no-leidas")
    public ResponseEntity<List<NotificacionRespuestaDTO>> listarMisNoLeidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                notificacionServicio.listarMisNoLeidas(userDetails.getUsername()));
    }

    // GET /notificaciones/mis-notificaciones/contador — devuelve {"noLeidas": N}
    @GetMapping("/mis-notificaciones/contador")
    public ResponseEntity<Map<String, Long>> contarMisNoLeidas(
            @AuthenticationPrincipal UserDetails userDetails) {
        long cantidad = notificacionServicio.contarMisNoLeidas(userDetails.getUsername());
        return ResponseEntity.ok(Map.of("noLeidas", cantidad));
    }

    // PATCH /notificaciones/{id}/marcar-leida
    @PatchMapping("/{id}/marcar-leida")
    public ResponseEntity<Void> marcarComoLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        notificacionServicio.marcarComoLeida(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
