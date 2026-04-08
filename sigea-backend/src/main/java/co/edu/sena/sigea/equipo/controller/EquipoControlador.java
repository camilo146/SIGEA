package co.edu.sena.sigea.equipo.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<EquipoRespuestaDTO> crear(
            @Valid @RequestBody EquipoCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.crear(dto, correo);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // GET /api/v1/equipos (cualquier autenticado puede listar para
    // préstamos/reservas)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarActivos() {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarActivos();
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/todos (cualquier autenticado para formularios)
    @GetMapping("/todos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarTodos() {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarTodos();
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/categoria/{categoriaId}
    @GetMapping("/categoria/{categoriaId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorCategoria(
            @PathVariable Long categoriaId) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorCategoria(categoriaId);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/ambiente/{ambienteId}
    @GetMapping("/ambiente/{ambienteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorAmbiente(
            @PathVariable Long ambienteId) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorAmbiente(ambienteId);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/estado/{estado}
    @GetMapping("/estado/{estado}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarPorEstado(
            @PathVariable EstadoEquipo estado) {

        List<EquipoRespuestaDTO> equipos = equipoServicio.listarPorEstado(estado);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/mi-inventario
    @GetMapping("/mi-inventario")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarMiInventario(
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        List<EquipoRespuestaDTO> equipos = equipoServicio.listarMiInventario(correo);
        return ResponseEntity.ok(equipos);
    }

    // GET /api/v1/equipos/mis-equipos
    @GetMapping("/mis-equipos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<List<EquipoRespuestaDTO>> listarMisEquiposComoPropietario(
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        List<EquipoRespuestaDTO> equipos = equipoServicio.listarMisEquiposComoPropietario(correo);
        return ResponseEntity.ok(equipos);
    }

    // PATCH /api/v1/equipos/{id}/recuperar
    @PatchMapping("/{id}/recuperar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<EquipoRespuestaDTO> recuperarEquipo(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.recuperarEquipo(id, correo);
        return ResponseEntity.ok(respuesta);
    }

    // GET /api/v1/equipos/{id}
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EquipoRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        EquipoRespuestaDTO equipo = equipoServicio.buscarPorId(id);
        return ResponseEntity.ok(equipo);
    }

    // PUT /api/v1/equipos/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<EquipoRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipoCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.actualizar(id, dto, correo);
        return ResponseEntity.ok(respuesta);
    }

    // PATCH /api/v1/equipos/{id}/estado/{nuevoEstado}
    @PatchMapping("/{id}/estado/{nuevoEstado}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<EquipoRespuestaDTO> cambiarEstado(
            @PathVariable Long id,
            @PathVariable EstadoEquipo nuevoEstado,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.cambiarEstado(id, nuevoEstado, correo);
        return ResponseEntity.ok(respuesta);
    }

    // PATCH /api/v1/equipos/{id}/dar-de-baja
    @PatchMapping("/{id}/dar-de-baja")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        equipoServicio.darDeBaja(id, correo);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/equipos/{id}/activar
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> activar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        equipoServicio.activar(id, correo);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/equipos/{id}/fotos
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<FotoEquipoRespuestaDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("archivo") MultipartFile archivo,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        FotoEquipoRespuestaDTO respuesta = equipoServicio.subirFoto(id, archivo, correo);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // DELETE /api/v1/equipos/{id}/fotos/{fotoId}
    @DeleteMapping("/{id}/fotos/{fotoId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> eliminarFoto(
            @PathVariable Long id,
            @PathVariable Long fotoId,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        equipoServicio.eliminarFoto(id, fotoId, correo);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/v1/equipos/{id} — eliminar equipo permanentemente (si no tiene
    // reservas/préstamos/mantenimientos/transferencias)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String correo = userDetails != null ? userDetails.getUsername() : null;
        equipoServicio.eliminar(id, correo);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/v1/equipos/{id}/sub-ubicacion/{subUbicacionId}
     * Asigna un equipo a una sub-ubicación dentro de su ambiente principal.
     * Para eliminar la sub-ubicación, usar DELETE
     * /api/v1/equipos/{id}/sub-ubicacion
     */
    @PatchMapping("/{id}/sub-ubicacion/{subUbicacionId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<EquipoRespuestaDTO> asignarSubUbicacion(
            @PathVariable Long id,
            @PathVariable Long subUbicacionId,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.asignarSubUbicacion(id, subUbicacionId, correo);
        return ResponseEntity.ok(respuesta);
    }

    /**
     * DELETE /api/v1/equipos/{id}/sub-ubicacion
     * Quita la sub-ubicación asignada al equipo (lo deja solo en el ambiente
     * principal).
     */
    @DeleteMapping("/{id}/sub-ubicacion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<EquipoRespuestaDTO> quitarSubUbicacion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails != null ? userDetails.getUsername() : null;
        EquipoRespuestaDTO respuesta = equipoServicio.asignarSubUbicacion(id, null, correo);
        return ResponseEntity.ok(respuesta);
    }
}
