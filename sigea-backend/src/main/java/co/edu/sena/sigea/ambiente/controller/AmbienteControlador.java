package co.edu.sena.sigea.ambiente.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.ambiente.dto.AmbienteCrearDTO;
import co.edu.sena.sigea.ambiente.dto.AmbienteRespuestaDTO;
import co.edu.sena.sigea.ambiente.service.AmbienteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/ambientes")
public class AmbienteControlador {

    private final AmbienteService ambienteService;

    public AmbienteControlador(AmbienteService ambienteService) {
        this.ambienteService = ambienteService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AmbienteRespuestaDTO> crear(
            @Valid @RequestBody AmbienteCrearDTO dto) {

        AmbienteRespuestaDTO respuesta = ambienteService.crear(dto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarActivos() {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarActivos();
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarTodos() {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarTodos();
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarPorInstructor(
            @PathVariable Long instructorId) {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarPorInstructor(instructorId);
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AmbienteRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        AmbienteRespuestaDTO ambiente = ambienteService.buscarPorId(id);
        return ResponseEntity.ok(ambiente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<AmbienteRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AmbienteCrearDTO dto) {

        AmbienteRespuestaDTO respuesta = ambienteService.actualizar(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> activar(@PathVariable Long id) {

        ambienteService.activar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {

        ambienteService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}