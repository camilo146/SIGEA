package co.edu.sena.sigea.equipo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.equipo.dto.EquipoCrearDTO;
import co.edu.sena.sigea.equipo.dto.EquipoRespuestaDTO;
import co.edu.sena.sigea.equipo.dto.FotoEquipoRespuestaDTO;
import co.edu.sena.sigea.equipo.service.EquipoServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/equipos")
public class EquipoControlador {

    private final EquipoServicio equipoServicio;

    public EquipoControlador(EquipoServicio equipoServicio) {
        this.equipoServicio = equipoServicio;
    }

    // POST /api/v1/equipos
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EquipoRespuestaDTO> crear(
            @Valid @RequestBody EquipoCrearDTO dto) {

        EquipoRespuestaDTO respuesta = equipoServicio.crear(dto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // GET /api/v1/equipos
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarActivos() {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarActivos();
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/todos
    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarTodos() {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarTodos();
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/categoria/{categoriaId}
    @GetMapping("/categoria/{categoriaId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorCategoria(
            @PathVariable Long categoriaId) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorCategoria(categoriaId);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/ambiente/{ambienteId}
    @GetMapping("/ambiente/{ambienteId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorAmbiente(
            @PathVariable Long ambienteId) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorAmbiente(ambienteId);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/estado/{estado}
    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorEstado(
            @PathVariable EstadoEquipo estado) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorEstado(estado);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/{id}
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EquipoRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        EquipoRespuestaDTO equipo = equipoServicio.buscarPorId(id);
        return ResponseEntity.ok(equipo);
    }

    // PUT /api/v1/equipos/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EquipoRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipoCrearDTO dto) {

        EquipoRespuestaDTO respuesta = equipoServicio.actualizar(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    // PATCH /api/v1/equipos/{id}/estado/{nuevoEstado}
    @PatchMapping("/{id}/estado/{nuevoEstado}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<EquipoRespuestaDTO> cambiarEstado(
            @PathVariable Long id,
            @PathVariable EstadoEquipo nuevoEstado) {

        EquipoRespuestaDTO respuesta = equipoServicio.cambiarEstado(id, nuevoEstado);
        return ResponseEntity.ok(respuesta);
    }

    // PATCH /api/v1/equipos/{id}/dar-de-baja
    @PatchMapping("/{id}/dar-de-baja")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {

        equipoServicio.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/equipos/{id}/activar
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> activar(@PathVariable Long id) {

        equipoServicio.activar(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/equipos/{id}/fotos
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<FotoEquipoRespuestaDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo) throws IOException {

        FotoEquipoRespuestaDTO respuesta = equipoServicio.subirFoto(id, archivo);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // DELETE /api/v1/equipos/{id}/fotos/{fotoId}
    @DeleteMapping("/{id}/fotos/{fotoId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminarFoto(
            @PathVariable Long id,
            @PathVariable Long fotoId) throws IOException {

        equipoServicio.eliminarFoto(id, fotoId);
        return ResponseEntity.noContent().build();
    }
}
