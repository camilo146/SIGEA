package co.edu.sena.sigea.marca.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoDuplicadoException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.marca.dto.MarcaCrearDTO;
import co.edu.sena.sigea.marca.dto.MarcaRespuestaDTO;
import co.edu.sena.sigea.marca.entity.Marca;
import co.edu.sena.sigea.marca.repository.MarcaRepository;

@Service
@Transactional
public class MarcaServicio {

    private final MarcaRepository marcaRepository;

    public MarcaServicio(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    public MarcaRespuestaDTO crear(MarcaCrearDTO dto) {
        if (marcaRepository.existsByNombre(dto.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe una marca con el nombre: " + dto.getNombre());
        }
        Marca marca = Marca.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .activo(true)
                .build();
        return convertirADTO(marcaRepository.save(marca));
    }

    @Transactional(readOnly = true)
    public List<MarcaRespuestaDTO> listarActivas() {
        return marcaRepository.findByActivoTrue().stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public List<MarcaRespuestaDTO> listarTodas() {
        return marcaRepository.findAll().stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public MarcaRespuestaDTO buscarPorId(Long id) {
        return convertirADTO(marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", id)));
    }

    public MarcaRespuestaDTO actualizar(Long id, MarcaCrearDTO dto) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", id));

        marcaRepository.findByNombre(dto.getNombre()).ifPresent(existente -> {
            if (!existente.getId().equals(id)) {
                throw new RecursoDuplicadoException("Ya existe otra marca con el nombre: " + dto.getNombre());
            }
        });

        marca.setNombre(dto.getNombre());
        marca.setDescripcion(dto.getDescripcion());
        return convertirADTO(marcaRepository.save(marca));
    }

    public MarcaRespuestaDTO activar(Long id) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", id));
        if (Boolean.TRUE.equals(marca.getActivo())) {
            throw new OperacionNoPermitidaException("La marca ya se encuentra activa");
        }
        marca.setActivo(true);
        return convertirADTO(marcaRepository.save(marca));
    }

    public MarcaRespuestaDTO desactivar(Long id) {
        Marca marca = marcaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Marca", id));
        if (Boolean.FALSE.equals(marca.getActivo())) {
            throw new OperacionNoPermitidaException("La marca ya se encuentra inactiva");
        }
        marca.setActivo(false);
        return convertirADTO(marcaRepository.save(marca));
    }

    private MarcaRespuestaDTO convertirADTO(Marca m) {
        return MarcaRespuestaDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .descripcion(m.getDescripcion())
                .activo(m.getActivo())
                .fechaCreacion(m.getFechaCreacion())
                .fechaActualizacion(m.getFechaActualizacion())
                .build();
    }
}
