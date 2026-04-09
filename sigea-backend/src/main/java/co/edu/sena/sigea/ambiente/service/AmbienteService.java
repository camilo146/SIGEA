package co.edu.sena.sigea.ambiente.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.sena.sigea.ambiente.dto.AmbienteCrearDTO;
import co.edu.sena.sigea.ambiente.dto.AmbienteRespuestaDTO;
import co.edu.sena.sigea.ambiente.dto.SubUbicacionResumenDTO;
import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.enums.Rol;
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

    @Value("${sigea.uploads.path:./uploads}")
    private String rutaUploads;

    // Constructor para inyectar los repositorios
    public AmbienteService(AmbienteRepository ambienteRepository,
            UsuarioRepository usuarioRepository) {
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)

    /**
     * Crea un ambiente (JSON, sin foto). Si correoUsuario corresponde a un
     * INSTRUCTOR, se asigna como responsable. Para ADMIN o ALIMENTADOR_EQUIPOS
     * se requiere idInstructorResponsable. Acepta padreId para sub-ubicaciones.
     */
    public AmbienteRespuestaDTO crear(AmbienteCrearDTO dto, String correoUsuario) {
        if (ambienteRepository.existsByNombre(dto.getNombre())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un ambiente con el nombre: " + dto.getNombre());
        }
        Usuario propietario = null;
        Usuario instructor;
        if (correoUsuario != null && !correoUsuario.isBlank()) {
            Usuario actual = usuarioRepository.findByCorreoElectronico(correoUsuario).orElse(null);
            propietario = actual;
            if (actual != null && actual.getRol() == Rol.INSTRUCTOR) {
                instructor = actual;
            } else {
                if (dto.getIdInstructorResponsable() == null) {
                    throw new OperacionNoPermitidaException("El ID del instructor responsable es obligatorio.");
                }
                instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
            }
        } else {
            if (dto.getIdInstructorResponsable() == null) {
                throw new OperacionNoPermitidaException("El ID del instructor responsable es obligatorio.");
            }
            instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
        }
        if (propietario == null) {
            propietario = instructor;
        }

        // Soporte de sub-ubicaciones: resolver el padre si se indica
        Ambiente padre = null;
        if (dto.getPadreId() != null) {
            padre = ambienteRepository.findById(dto.getPadreId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Ambiente padre no encontrado con ID: " + dto.getPadreId()));
            if (!padre.getActivo()) {
                throw new OperacionNoPermitidaException(
                        "El ambiente padre está inactivo. No se puede crear una sub-ubicación en él.");
            }
        }

        Ambiente ambiente = Ambiente.builder()
                .nombre(dto.getNombre())
                .ubicacion(dto.getUbicacion())
                .descripcion(dto.getDescripcion())
                .direccion(dto.getDireccion())
                .instructorResponsable(instructor)
                .propietario(propietario)
                .padre(padre)
                .activo(true)
                .build();
        Ambiente guardado = ambienteRepository.save(ambiente);
        return convertirADTO(guardado);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AmbienteRespuestaDTO crearConFoto(AmbienteCrearDTO dto, MultipartFile archivo, String correoInstructor)
            throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new OperacionNoPermitidaException("La foto de la ubicación es obligatoria.");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new OperacionNoPermitidaException("El archivo de foto no tiene nombre.");
        }
        if (!nombreOriginal.contains(".")) {
            throw new OperacionNoPermitidaException("El archivo de foto no tiene una extensión válida.");
        }

        String extension = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
        if (!List.of("jpg", "jpeg", "png").contains(extension)) {
            throw new OperacionNoPermitidaException("Formato no permitido. Use: JPG, JPEG o PNG.");
        }

        long tamanoMaximo = 5L * 1024L * 1024L;
        if (archivo.getSize() > tamanoMaximo) {
            throw new OperacionNoPermitidaException("La foto excede el tamaño máximo de 5 MB.");
        }

        if (ambienteRepository.existsByNombre(dto.getNombre())) {
            throw new RecursoDuplicadoException("Ya existe un ambiente con el nombre: " + dto.getNombre());
        }

        Usuario propietario = null;
        Usuario instructor;
        if (correoInstructor != null && !correoInstructor.isBlank()) {
            Usuario actual = usuarioRepository.findByCorreoElectronico(correoInstructor).orElse(null);
            propietario = actual;
            if (actual != null && actual.getRol() == Rol.INSTRUCTOR) {
                instructor = actual;
            } else {
                if (dto.getIdInstructorResponsable() == null) {
                    throw new OperacionNoPermitidaException("El ID del instructor responsable es obligatorio.");
                }
                instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
            }
        } else {
            if (dto.getIdInstructorResponsable() == null) {
                throw new OperacionNoPermitidaException("El ID del instructor responsable es obligatorio.");
            }
            instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
        }
        if (propietario == null) {
            propietario = instructor;
        }

        Path directorio = Paths.get(rutaUploads).resolve("ambientes");
        Files.createDirectories(directorio);

        String nombreEnServidor = UUID.randomUUID().toString() + "_" + nombreOriginal;
        Path rutaArchivo = directorio.resolve(nombreEnServidor);
        Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);
        String rutaParaBD = "/uploads/ambientes/" + nombreEnServidor;

        try {
            // Soporte de sub-ubicaciones en crearConFoto
            Ambiente padre = null;
            if (dto.getPadreId() != null) {
                padre = ambienteRepository.findById(dto.getPadreId())
                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                "Ambiente padre no encontrado con ID: " + dto.getPadreId()));
                if (!padre.getActivo()) {
                    throw new OperacionNoPermitidaException(
                            "El ambiente padre está inactivo. No se puede crear una sub-ubicación en él.");
                }
            }
            Ambiente ambiente = Ambiente.builder()
                    .nombre(dto.getNombre())
                    .ubicacion(dto.getUbicacion())
                    .descripcion(dto.getDescripcion())
                    .direccion(dto.getDireccion())
                    .instructorResponsable(instructor)
                    .propietario(propietario)
                    .padre(padre)
                    .activo(true)
                    .rutaFoto(rutaParaBD)
                    .build();

            Ambiente guardado = ambienteRepository.save(ambiente);
            return convertirADTO(guardado);
        } catch (RuntimeException ex) {
            Files.deleteIfExists(rutaArchivo);
            throw ex;
        }
    }

    // Listar ambientes activos
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarActivos() {
        return ambienteRepository.findByActivoTrue()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    // Metodo para listar todos lo ambientes (incluye inactivos, solo admin)
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarTodos() {
        return ambienteRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarPorInstructor(Long instructorId) {
        return ambienteRepository.findByInstructorResponsableId(instructorId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    /**
     * Lista ambientes que el usuario actual (instructor) administra. Por correo.
     */
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarPorCorreoInstructor(String correo) {
        if (correo == null || correo.isBlank())
            return Collections.emptyList();
        return usuarioRepository.findByCorreoElectronico(correo)
                .map(u -> ambienteRepository.findByPropietarioId(u.getId())
                        .stream().map(this::convertirADTO).toList())
                .orElse(Collections.emptyList());
    }

    //
    @Transactional(readOnly = true)
    public AmbienteRespuestaDTO buscarPorId(Long id) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        return convertirADTO(ambiente);
    }

    @Transactional
    public AmbienteRespuestaDTO actualizar(Long id, AmbienteCrearDTO dto, String correoUsuario) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        verificarPropiedadInstructor(ambiente, correoUsuario, "Solo puedes editar los ambientes que tú creaste.");
        ambienteRepository.findByNombre(dto.getNombre())
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new RecursoDuplicadoException(
                                "Ya existe otro ambiente con el nombre: " + dto.getNombre());
                    }
                });
        Usuario instructor = usuarioRepository.findById(dto.getIdInstructorResponsable())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Instructor no encontrado con ID: " + dto.getIdInstructorResponsable()));
        ambiente.setNombre(dto.getNombre());
        ambiente.setUbicacion(dto.getUbicacion());
        ambiente.setDescripcion(dto.getDescripcion());
        ambiente.setDireccion(dto.getDireccion());
        ambiente.setInstructorResponsable(instructor);
        Ambiente actualizado = ambienteRepository.save(ambiente);
        return convertirADTO(actualizado);
    }

    @Transactional
    public void desactivar(Long id, String correoUsuario) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        verificarPropiedadInstructor(ambiente, correoUsuario,
            "Solo puedes activar/desactivar los ambientes que tú creaste.");
        if (!ambiente.getActivo()) {
            throw new OperacionNoPermitidaException("El ambiente ya se encuentra desactivado");
        }
        ambiente.setActivo(false);
        ambienteRepository.save(ambiente);
    }

    @Transactional
    public void activar(Long id, String correoUsuario) {
        Ambiente ambiente = ambienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ambiente", id));
        verificarPropiedadInstructor(ambiente, correoUsuario,
            "Solo puedes activar/desactivar los ambientes que tú creaste.");
        if (ambiente.getActivo()) {
            throw new OperacionNoPermitidaException("El ambiente ya se encuentra activo");
        }
        ambiente.setActivo(true);
        ambienteRepository.save(ambiente);
    }

    private void verificarPropiedadInstructor(Ambiente ambiente, String correoUsuario, String mensajeError) {
        if (correoUsuario == null || correoUsuario.isBlank())
            return;
        Usuario actual = usuarioRepository.findByCorreoElectronico(correoUsuario).orElse(null);
        if (actual != null && actual.getRol() == Rol.INSTRUCTOR) {
            if (ambiente.getPropietario() == null
                    || !ambiente.getPropietario().getId().equals(actual.getId())) {
                throw new OperacionNoPermitidaException(mensajeError);
            }
        }
    }

    // Metodo privado para convertir una entidad Ambiente a un DTO de respuesta
    private AmbienteRespuestaDTO convertirADTO(Ambiente ambiente) {
        List<SubUbicacionResumenDTO> subUbicacionesDTO = ambiente.getSubUbicaciones() == null
                ? List.of()
                : ambiente.getSubUbicaciones().stream()
                        .map(sub -> SubUbicacionResumenDTO.builder()
                                .id(sub.getId())
                                .nombre(sub.getNombre())
                                .ubicacion(sub.getUbicacion())
                                .descripcion(sub.getDescripcion())
                                .activo(sub.getActivo())
                                .build())
                        .toList();

        return AmbienteRespuestaDTO.builder()
                .id(ambiente.getId())
                .nombre(ambiente.getNombre())
                .ubicacion(ambiente.getUbicacion())
                .descripcion(ambiente.getDescripcion())
                .direccion(ambiente.getDireccion())
                .instructorResponsableId(
                        ambiente.getInstructorResponsable() != null
                                ? ambiente.getInstructorResponsable().getId()
                                : null)
                .instructorResponsableNombre(
                        ambiente.getInstructorResponsable() != null
                                ? ambiente.getInstructorResponsable().getNombreCompleto()
                                : null)
                .propietarioId(
                    ambiente.getPropietario() != null
                        ? ambiente.getPropietario().getId()
                        : null)
                .propietarioNombre(
                    ambiente.getPropietario() != null
                        ? ambiente.getPropietario().getNombreCompleto()
                        : null)
                .padreId(ambiente.getPadre() != null ? ambiente.getPadre().getId() : null)
                .padreNombre(ambiente.getPadre() != null ? ambiente.getPadre().getNombre() : null)
                .subUbicaciones(subUbicacionesDTO)
                .activo(ambiente.getActivo())
                .rutaFoto(ambiente.getRutaFoto())
                .fechaCreacion(ambiente.getFechaCreacion())
                .fechaActualizacion(ambiente.getFechaActualizacion())
                .build();
    }

    // =========================================================================
    // SUB-UBICACIONES
    // =========================================================================

    /** Crear una sub-ubicación hija de un ambiente padre. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AmbienteRespuestaDTO crearSubUbicacion(Long padreId, AmbienteCrearDTO dto, String correoUsuario) {
        dto = copiarConPadre(dto, padreId);
        return crear(dto, correoUsuario);
    }

    /** Listar sub-ubicaciones de un ambiente padre dado. */
    @Transactional(readOnly = true)
    public List<AmbienteRespuestaDTO> listarSubUbicaciones(Long padreId) {
        if (!ambienteRepository.existsById(padreId)) {
            throw new RecursoNoEncontradoException("Ambiente padre no encontrado con ID: " + padreId);
        }
        return ambienteRepository.findByPadreId(padreId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    /**
     * Asignar un equipo ya existente a una sub-ubicación. La lógica está en
     * EquipoServicio.
     */
    private AmbienteCrearDTO copiarConPadre(AmbienteCrearDTO dto, Long padreId) {
        AmbienteCrearDTO copia = new AmbienteCrearDTO();
        copia.setNombre(dto.getNombre());
        copia.setUbicacion(dto.getUbicacion());
        copia.setDescripcion(dto.getDescripcion());
        copia.setDireccion(dto.getDireccion());
        copia.setIdInstructorResponsable(dto.getIdInstructorResponsable());
        copia.setPadreId(padreId);
        return copia;
    }
}