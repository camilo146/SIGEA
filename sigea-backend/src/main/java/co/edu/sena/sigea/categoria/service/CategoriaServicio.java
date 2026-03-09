package co.edu.sena.sigea.categoria.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.categoria.dto.CategoriaCrearDTO;
import co.edu.sena.sigea.categoria.dto.CategoriaRespuestaDTO;
import co.edu.sena.sigea.categoria.entity.Categoria;
import co.edu.sena.sigea.categoria.repository.CategoriaRepository;
import co.edu.sena.sigea.common.exception.RecursoDuplicadoException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;

@Service
public class CategoriaServicio {

    private final CategoriaRepository categoriaRepository;

    public CategoriaServicio(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    //  CREAR 

    @Transactional
    public CategoriaRespuestaDTO crear(CategoriaCrearDTO dto) {

        if (categoriaRepository.existsByNombre(dto.getNombre())) {
            throw new RecursoDuplicadoException(
                    "Ya existe una categoría con el nombre: " + dto.getNombre());
        }

        Categoria categoria = Categoria.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .activo(true)
                .build();

        Categoria guardada = categoriaRepository.save(categoria);

        return convertirADTO(guardada);
    }

    //  LISTAR TODAS (solo activas) 

    @Transactional(readOnly = true)
    public List<CategoriaRespuestaDTO> listarActivas() {
        return categoriaRepository.findByActivoTrue()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // LISTAR TODAS (incluyendo inactivas, para admins) 

    @Transactional(readOnly = true)
    public List<CategoriaRespuestaDTO> listarTodas() {
        return categoriaRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // BUSCAR POR ID 

    @Transactional(readOnly = true)
    public CategoriaRespuestaDTO buscarPorId(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));

        return convertirADTO(categoria);
    }

    //  ACTUALIZAR 

    @Transactional
    public CategoriaRespuestaDTO actualizar(Long id, CategoriaCrearDTO dto) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));

        categoriaRepository.findByNombre(dto.getNombre())
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new RecursoDuplicadoException(
                                "Ya existe otra categoría con el nombre: " + dto.getNombre());
                    }
                });

        categoria.setNombre(dto.getNombre());
        categoria.setDescripcion(dto.getDescripcion());

        Categoria actualizada = categoriaRepository.save(categoria);

        return convertirADTO(actualizada);
    }

    //  ELIMINAR (lógico) 

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría", id));

        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    //  MÉTODO PRIVADO: Convertir Entidad → DTO 

    private CategoriaRespuestaDTO convertirADTO(Categoria categoria) {
        return CategoriaRespuestaDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .descripcion(categoria.getDescripcion())
                .activo(categoria.getActivo())
                .fechaCreacion(categoria.getFechaCreacion())
                .fechaActualizacion(categoria.getFechaActualizacion())
                .build();
    }
}
