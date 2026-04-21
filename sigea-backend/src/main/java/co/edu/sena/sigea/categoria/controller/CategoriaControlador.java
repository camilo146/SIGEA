package co.edu.sena.sigea.categoria.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.categoria.dto.CategoriaCrearDTO;
import co.edu.sena.sigea.categoria.dto.CategoriaRespuestaDTO;
import co.edu.sena.sigea.categoria.service.CategoriaServicio;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/categorias")

// Controlador REST para gestionar las categorías de equipos, incluye endpoints para crear, 
// listar (activos y todos), buscar por ID, actualizar y eliminar categorías.
public class CategoriaControlador {

    private final CategoriaServicio categoriaServicio;

    public CategoriaControlador(CategoriaServicio categoriaServicio) {
        this.categoriaServicio = categoriaServicio;
    }

    //  POST /api/v1/categorias 

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaRespuestaDTO> crear(
            @Valid @RequestBody CategoriaCrearDTO dto) {

        CategoriaRespuestaDTO respuesta = categoriaServicio.crear(dto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    //  GET /api/v1/categorias 

    @GetMapping
    public ResponseEntity<List<CategoriaRespuestaDTO>> listarActivas() {

        List<CategoriaRespuestaDTO> categorias = categoriaServicio.listarActivas();
        return ResponseEntity.ok(categorias);
    }

    //  GET /api/v1/categorias/todas 

    @GetMapping("/todas")
    public ResponseEntity<List<CategoriaRespuestaDTO>> listarTodas() {

        List<CategoriaRespuestaDTO> categorias = categoriaServicio.listarTodas();
        return ResponseEntity.ok(categorias);
    }

    //  GET /api/v1/categorias/{id} 

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        CategoriaRespuestaDTO categoria = categoriaServicio.buscarPorId(id);
        return ResponseEntity.ok(categoria);
    }

    //  PUT /api/v1/categorias/{id} 

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<CategoriaRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaCrearDTO dto) {

        CategoriaRespuestaDTO respuesta = categoriaServicio.actualizar(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    //  DELETE /api/v1/categorias/{id} 

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        categoriaServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}