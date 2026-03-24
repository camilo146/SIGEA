package co.edu.sena.sigea.ambiente.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<AmbienteRespuestaDTO> crear(
            @Valid @ModelAttribute AmbienteCrearDTO dto,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        AmbienteRespuestaDTO respuesta = ambienteService.crearConFoto(
                dto,
                archivo,
                userDetails != null ? userDetails.getUsername() : null);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarActivos() {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarActivos();
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/todos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarTodos() {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarTodos();
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/instructor/{instructorId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarPorInstructor(
            @PathVariable Long instructorId) {

        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarPorInstructor(instructorId);
        return ResponseEntity.ok(ambientes);
    }

    /**
     * Ambientes que el usuario actual (instructor) administra. Solo aplica para
     * INSTRUCTOR.
     */
    @GetMapping("/mi-ambiente")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<List<AmbienteRespuestaDTO>> listarMiAmbiente(
            @AuthenticationPrincipal UserDetails userDetails) {
        List<AmbienteRespuestaDTO> ambientes = ambienteService.listarPorCorreoInstructor(
                userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(ambientes);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AmbienteRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        AmbienteRespuestaDTO ambiente = ambienteService.buscarPorId(id);
        return ResponseEntity.ok(ambiente);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<AmbienteRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AmbienteCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        AmbienteRespuestaDTO respuesta = ambienteService.actualizar(id, dto,
                userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.ok(respuesta);
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> activar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ambienteService.activar(id, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> desactivar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        ambienteService.desactivar(id, userDetails != null ? userDetails.getUsername() : null);
        return ResponseEntity.noContent().build();
    }
}