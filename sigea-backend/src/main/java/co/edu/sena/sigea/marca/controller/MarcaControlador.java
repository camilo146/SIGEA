package co.edu.sena.sigea.marca.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.marca.dto.MarcaCrearDTO;
import co.edu.sena.sigea.marca.dto.MarcaRespuestaDTO;
import co.edu.sena.sigea.marca.service.MarcaServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/marcas")
public class MarcaControlador {

    private final MarcaServicio marcaServicio;

    public MarcaControlador(MarcaServicio marcaServicio) {
        this.marcaServicio = marcaServicio;
    }

    /** POST /api/v1/marcas — Crear marca (roles operativos) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<MarcaRespuestaDTO> crear(@Valid @RequestBody MarcaCrearDTO dto) {
        return new ResponseEntity<>(marcaServicio.crear(dto), HttpStatus.CREATED);
    }

    /** GET /api/v1/marcas — Listar marcas activas (todos los autenticados) */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MarcaRespuestaDTO>> listarActivas() {
        return ResponseEntity.ok(marcaServicio.listarActivas());
    }

    /** GET /api/v1/marcas/todas — Listar todas (roles operativos) */
    @GetMapping("/todas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<List<MarcaRespuestaDTO>> listarTodas() {
        return ResponseEntity.ok(marcaServicio.listarTodas());
    }

    /** GET /api/v1/marcas/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MarcaRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(marcaServicio.buscarPorId(id));
    }

    /** PUT /api/v1/marcas/{id} — Editar (roles operativos) */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<MarcaRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MarcaCrearDTO dto) {
        return ResponseEntity.ok(marcaServicio.actualizar(id, dto));
    }

    /** PUT /api/v1/marcas/{id}/activar */
    @PutMapping("/{id}/activar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<MarcaRespuestaDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(marcaServicio.activar(id));
    }

    /** PUT /api/v1/marcas/{id}/desactivar */
    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')")
    public ResponseEntity<MarcaRespuestaDTO> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(marcaServicio.desactivar(id));
    }
}
