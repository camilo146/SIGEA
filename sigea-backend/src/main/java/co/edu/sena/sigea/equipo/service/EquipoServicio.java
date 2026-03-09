package co.edu.sena.sigea.equipo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.categoria.entity.Categoria;
import co.edu.sena.sigea.categoria.repository.CategoriaRepository;
import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoDuplicadoException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.dto.EquipoCrearDTO;
import co.edu.sena.sigea.equipo.dto.EquipoRespuestaDTO;
import co.edu.sena.sigea.equipo.dto.FotoEquipoRespuestaDTO;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.entity.FotoEquipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.equipo.repository.FotoEquipoRepository;

@Service
public class EquipoServicio {

    private static final Logger log = LoggerFactory.getLogger(EquipoServicio.class);

    private final EquipoRepository equipoRepository;
    private final FotoEquipoRepository fotoEquipoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AmbienteRepository ambienteRepository;
    private final String rutaUploads;

    public EquipoServicio(
            EquipoRepository equipoRepository,
            FotoEquipoRepository fotoEquipoRepository,
            CategoriaRepository categoriaRepository,
            AmbienteRepository ambienteRepository,
            @Value("${sigea.uploads.path}") String rutaUploads) {

        this.equipoRepository = equipoRepository;
        this.fotoEquipoRepository = fotoEquipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.ambienteRepository = ambienteRepository;
        this.rutaUploads = rutaUploads;
    }

    @Transactional
    public EquipoRespuestaDTO crear(EquipoCrearDTO dto) {

        if (equipoRepository.existsByCodigoUnico(dto.getCodigoUnico())) {
            throw new RecursoDuplicadoException(
                    "Ya existe un equipo con el codigo: " + dto.getCodigoUnico());
        }

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con ID: " + dto.getCategoriaId()));

        if (!categoria.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se puede asignar el equipo a una categoria inactiva");
        }

        Ambiente ambiente = ambienteRepository.findById(dto.getAmbienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Ambiente no encontrado con ID: " + dto.getAmbienteId()));

        if (!ambiente.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se puede asignar el equipo a un ambiente inactivo");
        }

        Equipo equipo = Equipo.builder()
                .nombre(dto.getNombre())
                .descripcion(dto.getDescripcion())
                .codigoUnico(dto.getCodigoUnico())
                .categoria(categoria)
                .ambiente(ambiente)
                .estado(EstadoEquipo.ACTIVO)
                .cantidadTotal(dto.getCantidadTotal())
                .cantidadDisponible(dto.getCantidadTotal())
                .umbralMinimo(dto.getUmbralMinimo())
                .activo(true)
                .build();

        Equipo guardado = equipoRepository.save(equipo);

        return convertirADTO(guardado);
    }

    @Transactional(readOnly = true)
    public List<EquipoRespuestaDTO> listarActivos() {
        return equipoRepository.findByActivoTrue()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipoRespuestaDTO> listarTodos() {
        return equipoRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipoRespuestaDTO> listarPorCategoria(Long categoriaId) {
        if (!categoriaRepository.existsById(categoriaId)) {
            throw new RecursoNoEncontradoException(
                    "Categoria no encontrada con ID: " + categoriaId);
        }
        return equipoRepository.findByCategoriaIdAndActivoTrue(categoriaId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipoRespuestaDTO> listarPorAmbiente(Long ambienteId) {
        if (!ambienteRepository.existsById(ambienteId)) {
            throw new RecursoNoEncontradoException(
                    "Ambiente no encontrado con ID: " + ambienteId);
        }
        return equipoRepository.findByAmbienteIdAndActivoTrue(ambienteId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EquipoRespuestaDTO> listarPorEstado(EstadoEquipo estado) {
        return equipoRepository.findByEstadoAndActivoTrue(estado)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipoRespuestaDTO buscarPorId(Long id) {
        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
        return convertirADTO(equipo);
    }

    @Transactional
    public EquipoRespuestaDTO actualizar(Long id, EquipoCrearDTO dto) {

        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        equipoRepository.findByCodigoUnico(dto.getCodigoUnico())
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new RecursoDuplicadoException(
                                "Ya existe otro equipo con el codigo: " + dto.getCodigoUnico());
                    }
                });

        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Categoria no encontrada con ID: " + dto.getCategoriaId()));

        if (!categoria.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se puede asignar el equipo a una categoria inactiva");
        }

        Ambiente ambiente = ambienteRepository.findById(dto.getAmbienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Ambiente no encontrado con ID: " + dto.getAmbienteId()));

        if (!ambiente.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se puede asignar el equipo a un ambiente inactivo");
        }

        if (dto.getCantidadTotal() < equipo.getCantidadDisponible()) {
            throw new OperacionNoPermitidaException(
                    "La cantidad total (" + dto.getCantidadTotal()
                    + ") no puede ser menor que la cantidad disponible actual ("
                    + equipo.getCantidadDisponible() + ")");
        }

        equipo.setNombre(dto.getNombre());
        equipo.setDescripcion(dto.getDescripcion());
        equipo.setCodigoUnico(dto.getCodigoUnico());
        equipo.setCategoria(categoria);
        equipo.setAmbiente(ambiente);
        equipo.setCantidadTotal(dto.getCantidadTotal());
        equipo.setUmbralMinimo(dto.getUmbralMinimo());

        Equipo actualizado = equipoRepository.save(equipo);

        return convertirADTO(actualizado);
    }

    @Transactional
    public EquipoRespuestaDTO cambiarEstado(Long id, EstadoEquipo nuevoEstado) {

        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (!equipo.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se puede cambiar el estado de un equipo dado de baja");
        }

        equipo.setEstado(nuevoEstado);
        Equipo actualizado = equipoRepository.save(equipo);

        return convertirADTO(actualizado);
    }

    @Transactional
    public void darDeBaja(Long id) {

        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (!equipo.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "El equipo ya se encuentra dado de baja");
        }

        equipo.setActivo(false);
        equipoRepository.save(equipo);
    }

    @Transactional
    public void activar(Long id) {

        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));

        if (equipo.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "El equipo ya se encuentra activo");
        }

        equipo.setActivo(true);
        equipoRepository.save(equipo);
    }

    @Transactional
    public FotoEquipoRespuestaDTO subirFoto(Long equipoId, MultipartFile archivo)
            throws IOException {

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));

        if (!equipo.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "No se pueden subir fotos a un equipo dado de baja");
        }

        long cantidadFotos = fotoEquipoRepository.countByEquipoId(equipoId);
        if (cantidadFotos >= 3) {
            throw new OperacionNoPermitidaException(
                    "El equipo ya tiene el maximo de 3 fotos permitidas");
        }

        if (archivo.isEmpty()) {
            throw new OperacionNoPermitidaException("El archivo de imagen esta vacio");
        }

        String nombreOriginal = archivo.getOriginalFilename();

        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new OperacionNoPermitidaException("El archivo no tiene nombre");
        }

        String extension = nombreOriginal
                .substring(nombreOriginal.lastIndexOf('.') + 1)
                .toLowerCase();

        if (!List.of("jpg", "jpeg", "png").contains(extension)) {
            throw new OperacionNoPermitidaException(
                    "Formato no permitido. Use: JPG, JPEG o PNG");
        }

        long tamanoMaximo = 5L * 1024L * 1024L;
        if (archivo.getSize() > tamanoMaximo) {
            throw new OperacionNoPermitidaException(
                    "El archivo excede el tamano maximo de 5 MB");
        }

        String nombreEnServidor = UUID.randomUUID().toString() + "_" + nombreOriginal;

        Path directorio = Paths.get(rutaUploads);
        Files.createDirectories(directorio);

        Path rutaArchivo = directorio.resolve(nombreEnServidor);
        Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

        String rutaParaBD = "/uploads/" + nombreEnServidor;

        FotoEquipo foto = FotoEquipo.builder()
                .equipo(equipo)
                .nombreArchivo(nombreOriginal)
                .rutaArchivo(rutaParaBD)
                .tamanoBytes(archivo.getSize())
                .fechaSubida(LocalDateTime.now())
                .build();

        FotoEquipo fotoGuardada = fotoEquipoRepository.save(foto);

        return convertirFotoADTO(fotoGuardada);
    }

    @Transactional
    public void eliminarFoto(Long equipoId, Long fotoId) {

        if (!equipoRepository.existsById(equipoId)) {
            throw new RecursoNoEncontradoException("Equipo", equipoId);
        }

        FotoEquipo foto = fotoEquipoRepository.findById(fotoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Foto", fotoId));

        if (!foto.getEquipo().getId().equals(equipoId)) {
            throw new OperacionNoPermitidaException(
                    "La foto no pertenece al equipo indicado");
        }

        fotoEquipoRepository.delete(foto);

        try {
            Path archivoAEliminar = Paths.get("." + foto.getRutaArchivo());
            Files.deleteIfExists(archivoAEliminar);
        } catch (IOException e) {
            log.warn("No se pudo eliminar el archivo físico del disco: {}", foto.getRutaArchivo(), e);
        }
    }

    private EquipoRespuestaDTO convertirADTO(Equipo equipo) {

        List<FotoEquipoRespuestaDTO> fotosDTO = fotoEquipoRepository
                .findByEquipoId(equipo.getId())
                .stream()
                .map(this::convertirFotoADTO)
                .toList();

        return EquipoRespuestaDTO.builder()
                .id(equipo.getId())
                .nombre(equipo.getNombre())
                .descripcion(equipo.getDescripcion())
                .codigoUnico(equipo.getCodigoUnico())
                .categoriaId(equipo.getCategoria() != null
                        ? equipo.getCategoria().getId() : null)
                .categoriaNombre(equipo.getCategoria() != null
                        ? equipo.getCategoria().getNombre() : null)
                .ambienteId(equipo.getAmbiente() != null
                        ? equipo.getAmbiente().getId() : null)
                .ambienteNombre(equipo.getAmbiente() != null
                        ? equipo.getAmbiente().getNombre() : null)
                .estado(equipo.getEstado())
                .cantidadTotal(equipo.getCantidadTotal())
                .cantidadDisponible(equipo.getCantidadDisponible())
                .umbralMinimo(equipo.getUmbralMinimo())
                .activo(equipo.getActivo())
                .fotos(fotosDTO)
                .fechaCreacion(equipo.getFechaCreacion())
                .fechaActualizacion(equipo.getFechaActualizacion())
                .build();
    }

    private FotoEquipoRespuestaDTO convertirFotoADTO(FotoEquipo foto) {
        return FotoEquipoRespuestaDTO.builder()
                .id(foto.getId())
                .nombreArchivo(foto.getNombreArchivo())
                .rutaArchivo(foto.getRutaArchivo())
                .tamanoBytes(foto.getTamanoBytes())
                .fechaSubida(foto.getFechaSubida())
                .build();
    }
}
