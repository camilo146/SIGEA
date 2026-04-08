package co.edu.sena.sigea.observacion.controller;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.observacion.dto.ObservacionEquipoCrearDTO;
import co.edu.sena.sigea.observacion.dto.ObservacionEquipoRespuestaDTO;
import co.edu.sena.sigea.observacion.service.ObservacionEquipoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/observaciones-equipo")
@RequiredArgsConstructor
@Validated
public class ObservacionEquipoControlador {

    private final ObservacionEquipoServicio servicio;

    /** Registra una observación al devolver un equipo */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR') or hasRole('FUNCIONARIO')")
    public ResponseEntity<ObservacionEquipoRespuestaDTO> registrar(
            @Valid @RequestBody ObservacionEquipoCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servicio.registrar(dto, userDetails.getUsername()));
    }

    /** Lista todas las observaciones de un equipo específico */
    @GetMapping("/equipo/{equipoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ObservacionEquipoRespuestaDTO>> listarPorEquipo(
            @PathVariable Long equipoId) {
        return ResponseEntity.ok(servicio.listarPorEquipo(equipoId));
    }

    /** Lista todas las observaciones de un préstamo específico */
    @GetMapping("/prestamo/{prestamoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ObservacionEquipoRespuestaDTO>> listarPorPrestamo(
            @PathVariable Long prestamoId) {
        return ResponseEntity.ok(servicio.listarPorPrestamo(prestamoId));
    }
}
