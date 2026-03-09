package co.edu.sena.sigea.ambiente.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.ambiente.dto.AmbienteCrearDTO;
import co.edu.sena.sigea.ambiente.dto.AmbienteRespuestaDTO;
import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoDuplicadoException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
// Servicio para gestionar los ambientes de formación
public class AmbienteService {
    // Inyectamos los repositorios necesarios para acceder a la base de datos
    private final AmbienteRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    
    // Constructor para inyectar los repositorios
    public AmbienteService(AmbienteRepository ambienteRepository,
                            UsuarioRepository usuarioRepository) {
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional// si algo falla revierte todo 

    //Metodo para crear un ambiente de formacion
    public AmbienteRespuestaDTO crear(AmbienteCrearDTO dto) {
        //Validar que no exista otro ambiente con el mismo nombre 
        if (ambienteRepository.existsByNombre(dto.getNombre())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un ambiente con el nombre: " + dto.getNombre());
        }
        
        //validar que el isntructor responsable exista ysea un instructor activo
        Usuario instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
        

                        //construie la entidad usando el patron builder 
        Ambiente ambiente = Ambiente.builder()
                .nombre(dto.getNombre())//setea el nombre del ambiente 
                .ubicacion(dto.getUbicacion())//setea la ubicacion del ambiente
                .descripcion(dto.getDescripcion())//setea la descripcion del ambiente
                .instructorResponsable(instructor)//setea el instructor responsable del ambiente
                .activo(true)//todo ambiete nuevo nace activo por defecto
                .build();//construye el objeto y lo retorna
        

        //Guardar en la base de datos 
        Ambiente guardado = ambienteRepository.save(ambiente);
         

        //Convertir a DTO y retornar la respuesta
        return convertirADTO(guardado);
    }
    
    //Listar ambientes activos 
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarActivos() {
        return ambienteRepository.findByActivoTrue()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }
    
    //Metodo para listar todos lo ambientes (incluye inactivos, solo admin)
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarTodos() {
        return ambienteRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)

    //Metodo para listar ambientes por instructor
    public List<AmbienteRespuestaDTO> listarPorInstructor(Long instructorId) {
        return ambienteRepository.findByInstructorResponsableId(instructorId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    //
    @Transactional(readOnly = true)
    public AmbienteRespuestaDTO buscarPorId(Long id) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        return convertirADTO(ambiente);
    }

    //Metodo para actulizar un hambiente de formacion 
    @Transactional
    public AmbienteRespuestaDTO actualizar(Long id, AmbienteCrearDTO dto) {
        

        //recibe eñ id del ambiente a actualizar y eñ DTO con los nuevos datos 
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        //validar que el ambiente no exista 
        ambienteRepository.findByNombre(dto.getNombre())
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new RecursoDuplicadoException(
                                "Ya existe otro ambiente con el nombre: " + dto.getNombre());
                    }
                });
        //valiar que el instructor responsable exista y sea un instructor activo
        Usuario instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));

        //actualizar los campos del ambiente con los datos del DTO 
        ambiente.setNombre(dto.getNombre());
        ambiente.setUbicacion(dto.getUbicacion());
        ambiente.setDescripcion(dto.getDescripcion());
        ambiente.setInstructorResponsable(instructor);
        

        //guardar los cambios en la DB
        Ambiente actualizado = ambienteRepository.save(ambiente);
        

        //convertir a DTO y retornar la respuesta
        return convertirADTO(actualizado);
    }


    //Metodo para desactivar un ambiente 
    @Transactional
    //
    public void desactivar(Long id) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));

        if (!ambiente.getActivo()) {
            throw new OperacionNoPermitidaException("El ambiente ya se encuentra desactivado");
        }

        ambiente.setActivo(false);
        ambienteRepository.save(ambiente);
    }
    

    //Metodo para activar un ambiente 
    @Transactional
    public void activar(Long id) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));

        if (ambiente.getActivo()) {
            throw new OperacionNoPermitidaException("El ambiente ya se encuentra activo");
        }

        ambiente.setActivo(true);
        ambienteRepository.save(ambiente);
    }
    

    //Metodo privado para convertir una entidad Ambiente a un DTO de respuesta
    private AmbienteRespuestaDTO convertirADTO(Ambiente ambiente) {
        return AmbienteRespuestaDTO.builder()
                .id(ambiente.getId())
                .nombre(ambiente.getNombre())
                .ubicacion(ambiente.getUbicacion())
                .descripcion(ambiente.getDescripcion())
                .instructorResponsableId(
                        ambiente.getInstructorResponsable() != null
                                ? ambiente.getInstructorResponsable().getId()
                                : null)
                .instructorResponsableNombre(
                        ambiente.getInstructorResponsable() != null
                                ? ambiente.getInstructorResponsable().getNombreCompleto()
                                : null)
                .activo(ambiente.getActivo())
                .fechaCreacion(ambiente.getFechaCreacion())
                .fechaActualizacion(ambiente.getFechaActualizacion())
                .build();
    }
}