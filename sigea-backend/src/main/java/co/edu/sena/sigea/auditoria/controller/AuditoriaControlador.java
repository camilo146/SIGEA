package co.edu.sena.sigea.auditoria.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.auditoria.dto.LogAuditoriaRespuestaDTO;
import co.edu.sena.sigea.auditoria.service.AuditoriaServicio;

@RestController
// Esta clase es un controlador REST: atiende peticiones HTTP
@RequestMapping("/auditoria")
// Todas las rutas de esta clase empiezan con /auditoria
@PreAuthorize("hasRole('ADMINISTRADOR')")
// Toda la clase exige rol ADMINISTRADOR — los logs son datos críticos
public class AuditoriaControlador {

    private final AuditoriaServicio auditoriaServicio;

    public AuditoriaControlador(AuditoriaServicio auditoriaServicio) {
        this.auditoriaServicio = auditoriaServicio;
    }

    // GET /api/v1/auditoria
    // Devuelve TODOS los logs del sistema
    @GetMapping
    public ResponseEntity<List<LogAuditoriaRespuestaDTO>> listarTodos() {
        return ResponseEntity.ok(auditoriaServicio.listarTodos());
    }

    // GET /api/v1/auditoria/usuario/{usuarioId}
    // Devuelve todos los logs de un usuario específico
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<LogAuditoriaRespuestaDTO>> listarPorUsuario(
            @PathVariable Long usuarioId) {
        // @PathVariable extrae el {usuarioId} de la URL
        return ResponseEntity.ok(auditoriaServicio.listarPorUsuario(usuarioId));
    }

    // GET /api/v1/auditoria/entidad/{entidad}/{entidadId}
    // Ej: GET /api/v1/auditoria/entidad/Equipo/5 → logs del Equipo con ID 5
    @GetMapping("/entidad/{entidad}/{entidadId}")
    public ResponseEntity<List<LogAuditoriaRespuestaDTO>> listarPorEntidad(
            @PathVariable String entidad,
            @PathVariable Long entidadId) {
        return ResponseEntity.ok(auditoriaServicio.listarPorEntidad(entidad, entidadId));
    }  
    
  
    // 
    // GET /api/v1/auditoria/rango?desde=2026-01-01T00:00:00&hasta=2026-01-31T23:59:59
    // Filtra logs entre dos fechas (se pasan como query params en formato ISO)
    @GetMapping("/rango")
    public ResponseEntity<List<LogAuditoriaRespuestaDTO>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        // @RequestParam lee el parámetro de la URL: ?desde=...&hasta=...
        // @DateTimeFormat convierte el String ISO a LocalDateTime automáticamente
        return ResponseEntity.ok(auditoriaServicio.listarPorRangoFechas(desde, hasta));
    }
}