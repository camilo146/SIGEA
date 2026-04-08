package co.edu.sena.sigea.equipo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.categoria.entity.Categoria;
import co.edu.sena.sigea.categoria.repository.CategoriaRepository;
import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.Rol;
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
import co.edu.sena.sigea.marca.entity.Marca;
import co.edu.sena.sigea.marca.repository.MarcaRepository;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.transferencia.repository.TransferenciaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
public class EquipoServicio {

        private static final Logger log = LoggerFactory.getLogger(EquipoServicio.class);

        private final EquipoRepository equipoRepository;
        private final FotoEquipoRepository fotoEquipoRepository;
        private final CategoriaRepository categoriaRepository;
        private final AmbienteRepository ambienteRepository;
        private final ReservaRepository reservaRepository;
        private final PrestamoRepository prestamoRepository;
        private final MantenimientoRepository mantenimientoRepository;
        private final TransferenciaRepository transferenciaRepository;
        private final UsuarioRepository usuarioRepository;
        private final MarcaRepository marcaRepository;
        private final String rutaUploads;

        public EquipoServicio(
                        EquipoRepository equipoRepository,
                        FotoEquipoRepository fotoEquipoRepository,
                        CategoriaRepository categoriaRepository,
                        AmbienteRepository ambienteRepository,
                        ReservaRepository reservaRepository,
                        PrestamoRepository prestamoRepository,
                        MantenimientoRepository mantenimientoRepository,
                        TransferenciaRepository transferenciaRepository,
                        UsuarioRepository usuarioRepository,
                        MarcaRepository marcaRepository,
                        @Value("${sigea.uploads.path}") String rutaUploads) {

                this.equipoRepository = equipoRepository;
                this.fotoEquipoRepository = fotoEquipoRepository;
                this.categoriaRepository = categoriaRepository;
                this.ambienteRepository = ambienteRepository;
                this.reservaRepository = reservaRepository;
                this.prestamoRepository = prestamoRepository;
                this.mantenimientoRepository = mantenimientoRepository;
                this.transferenciaRepository = transferenciaRepository;
                this.usuarioRepository = usuarioRepository;
                this.marcaRepository = marcaRepository;
                this.rutaUploads = rutaUploads;
        }

        @Transactional(isolation = Isolation.READ_COMMITTED)
        public EquipoRespuestaDTO crear(EquipoCrearDTO dto, String correoUsuario) {

                Usuario usuarioActual = obtenerUsuarioActual(correoUsuario);
                Usuario propietario = resolverPropietarioParaCreacion(dto, usuarioActual);

                String codigoUnico = dto.getCodigoUnico();
                if (codigoUnico == null || codigoUnico.isBlank()) {
                        codigoUnico = generarCodigoUnico();
                }
                if (equipoRepository.existsByCodigoUnico(codigoUnico)) {
                        throw new RecursoDuplicadoException(
                                        "Ya existe un equipo con el codigo: " + codigoUnico);
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

                // Sub-ubicación opcional: debe pertenecer al ambiente principal indicado
                Ambiente subUbicacion = null;
                if (dto.getSubUbicacionId() != null) {
                        subUbicacion = ambienteRepository.findById(dto.getSubUbicacionId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Sub-ubicacion no encontrada con ID: "
                                                                        + dto.getSubUbicacionId()));
                        if (subUbicacion.getPadre() == null
                                        || !subUbicacion.getPadre().getId().equals(ambiente.getId())) {
                                throw new OperacionNoPermitidaException(
                                                "La sub-ubicacion indicada no pertenece al ambiente seleccionado.");
                        }
                }

                // Resolver marca
                Marca marca = null;
                if (dto.getMarcaId() != null) {
                        marca = marcaRepository.findById(dto.getMarcaId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Marca no encontrada con ID: " + dto.getMarcaId()));
                        if (!Boolean.TRUE.equals(marca.getActivo())) {
                                throw new OperacionNoPermitidaException("No se puede usar una marca inactiva");
                        }
                }

                // Verificar placa única
                if (dto.getPlaca() != null && !dto.getPlaca().isBlank()) {
                        equipoRepository.findByPlaca(dto.getPlaca()).ifPresent(ex -> {
                                throw new RecursoDuplicadoException(
                                                "Ya existe un equipo con la placa: " + dto.getPlaca());
                        });
                }

                Equipo equipo = Equipo.builder()
                                .nombre(dto.getNombre())
                                .descripcion(dto.getDescripcion())
                                .placa(dto.getPlaca())
                                .serial(dto.getSerial())
                                .modelo(dto.getModelo())
                                .marca(marca)
                                .estadoEquipoEscala(dto.getEstadoEquipoEscala())
                                .codigoUnico(codigoUnico)
                                .categoria(categoria)
                                .ambiente(ambiente)
                                .subUbicacion(subUbicacion)
                                .propietario(propietario)
                                .inventarioActualInstructor(propietario)
                                .estado(EstadoEquipo.ACTIVO)
                                .cantidadTotal(dto.getCantidadTotal())
                                .cantidadDisponible(dto.getCantidadTotal())
                                .tipoUso(dto.getTipoUso())
                                .umbralMinimo(dto.getUmbralMinimo())
                                .activo(true)
                                .build();

                Equipo guardado = equipoRepository.save(equipo);

                return convertirADTO(guardado);
        }

        /** Genera un código único automático (ej: SIGEA-EQ-20260310143022). */
        private String generarCodigoUnico() {
                String base = "SIGEA-EQ-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                String codigo = base;
                int sufijo = 0;
                while (equipoRepository.existsByCodigoUnico(codigo)) {
                        codigo = base + "-" + (++sufijo);
                }
                return codigo;
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

        /**
         * Equipos que actualmente están en el inventario del instructor autenticado.
         */
        @Transactional(readOnly = true)
        public List<EquipoRespuestaDTO> listarMiInventario(String correoUsuario) {
                Usuario usuario = obtenerUsuarioActual(correoUsuario);
                return equipoRepository.findByInventarioActualInstructorIdAndActivoTrue(usuario.getId())
                                .stream()
                                .map(this::convertirADTO)
                                .toList();
        }

        /**
         * Todos los equipos de los que el instructor autenticado es propietario (estén
         * donde estén).
         */
        @Transactional(readOnly = true)
        public List<EquipoRespuestaDTO> listarMisEquiposComoPropietario(String correoUsuario) {
                Usuario usuario = obtenerUsuarioActual(correoUsuario);
                return equipoRepository.findByPropietarioIdAndActivoTrue(usuario.getId())
                                .stream()
                                .map(this::convertirADTO)
                                .toList();
        }

        /**
         * El propietario recupera un equipo transferido, devolviéndolo a su inventario.
         */
        @Transactional
        public EquipoRespuestaDTO recuperarEquipo(Long equipoId, String correoUsuario) {
                Equipo equipo = equipoRepository.findById(equipoId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));

                Usuario usuario = obtenerUsuarioActual(correoUsuario);

                boolean esPropietario = equipo.getPropietario() != null
                                && equipo.getPropietario().getId().equals(usuario.getId());
                boolean esAdmin = usuario.getRol() == Rol.ADMINISTRADOR;

                if (!esPropietario && !esAdmin) {
                        throw new OperacionNoPermitidaException(
                                        "Solo el propietario del equipo o un administrador puede recuperarlo.");
                }

                if (equipo.getInventarioActualInstructor() != null
                                && equipo.getInventarioActualInstructor().getId()
                                                .equals(equipo.getPropietario().getId())) {
                        throw new OperacionNoPermitidaException(
                                        "El equipo ya se encuentra en el inventario del propietario.");
                }

                equipo.setInventarioActualInstructor(equipo.getPropietario());
                Equipo actualizado = equipoRepository.save(equipo);
                return convertirADTO(actualizado);
        }

        @Transactional(readOnly = true)
        public EquipoRespuestaDTO buscarPorId(Long id) {
                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                return convertirADTO(equipo);
        }

        @Transactional
        public EquipoRespuestaDTO actualizar(Long id, EquipoCrearDTO dto, String correoUsuario) {

                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                validarPermisoGestion(equipo, correoUsuario);

                equipoRepository.findByCodigoUnico(dto.getCodigoUnico())
                                .ifPresent(existente -> {
                                        if (!existente.getId().equals(id)) {
                                                throw new RecursoDuplicadoException(
                                                                "Ya existe otro equipo con el codigo: "
                                                                                + dto.getCodigoUnico());
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

                // Resolver marca para actualizar
                Marca marcaActualizar = null;
                if (dto.getMarcaId() != null) {
                        marcaActualizar = marcaRepository.findById(dto.getMarcaId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Marca no encontrada con ID: " + dto.getMarcaId()));
                        if (!Boolean.TRUE.equals(marcaActualizar.getActivo())) {
                                throw new OperacionNoPermitidaException("No se puede usar una marca inactiva");
                        }
                }

                // Verificar placa única (ignorar el equipo actual)
                if (dto.getPlaca() != null && !dto.getPlaca().isBlank()) {
                        equipoRepository.findByPlaca(dto.getPlaca()).ifPresent(ex -> {
                                if (!ex.getId().equals(id)) {
                                        throw new RecursoDuplicadoException(
                                                        "Ya existe otro equipo con la placa: " + dto.getPlaca());
                                }
                        });
                }

                equipo.setNombre(dto.getNombre());
                equipo.setDescripcion(dto.getDescripcion());
                equipo.setPlaca(dto.getPlaca());
                equipo.setSerial(dto.getSerial());
                equipo.setModelo(dto.getModelo());
                equipo.setMarca(marcaActualizar);
                equipo.setEstadoEquipoEscala(dto.getEstadoEquipoEscala());
                equipo.setCodigoUnico(dto.getCodigoUnico());
                equipo.setCategoria(categoria);
                equipo.setAmbiente(ambiente);
                equipo.setCantidadTotal(dto.getCantidadTotal());
                equipo.setTipoUso(dto.getTipoUso());
                equipo.setUmbralMinimo(dto.getUmbralMinimo());

                Equipo actualizado = equipoRepository.save(equipo);

                return convertirADTO(actualizado);
        }

        @Transactional(isolation = Isolation.READ_COMMITTED)
        public EquipoRespuestaDTO asignarSubUbicacion(Long equipoId, Long subUbicacionId, String correoUsuario) {
                Equipo equipo = equipoRepository.findById(equipoId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));
                validarPermisoGestion(equipo, correoUsuario);

                if (subUbicacionId == null) {
                        equipo.setSubUbicacion(null);
                } else {
                        Ambiente subUbicacion = ambienteRepository.findById(subUbicacionId)
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Sub-ubicación no encontrada con ID: " + subUbicacionId));
                        if (subUbicacion.getPadre() == null ||
                                        !subUbicacion.getPadre().getId().equals(equipo.getAmbiente().getId())) {
                                throw new OperacionNoPermitidaException(
                                                "La sub-ubicación no pertenece al ambiente principal del equipo.");
                        }
                        equipo.setSubUbicacion(subUbicacion);
                }
                return convertirADTO(equipoRepository.save(equipo));
        }

        public EquipoRespuestaDTO cambiarEstado(Long id, EstadoEquipo nuevoEstado, String correoUsuario) {

                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                validarPermisoGestion(equipo, correoUsuario);

                if (!equipo.getActivo()) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede cambiar el estado de un equipo dado de baja");
                }

                equipo.setEstado(nuevoEstado);
                Equipo actualizado = equipoRepository.save(equipo);

                return convertirADTO(actualizado);
        }

        @Transactional
        public void darDeBaja(Long id, String correoUsuario) {

                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                validarPermisoGestion(equipo, correoUsuario);

                if (!equipo.getActivo()) {
                        throw new OperacionNoPermitidaException(
                                        "El equipo ya se encuentra dado de baja");
                }

                equipo.setActivo(false);
                equipoRepository.save(equipo);
        }

        @Transactional
        public void activar(Long id, String correoUsuario) {

                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                validarPermisoGestion(equipo, correoUsuario);

                if (equipo.getActivo()) {
                        throw new OperacionNoPermitidaException(
                                        "El equipo ya se encuentra activo");
                }

                equipo.setActivo(true);
                equipoRepository.save(equipo);
        }

        /**
         * Elimina permanentemente un equipo. No se puede si tiene reservas, préstamos,
         * mantenimientos o transferencias asociados. Elimina antes sus fotos (y
         * archivos).
         */
        @Transactional
        public void eliminar(Long id, String correoUsuario) {
                Equipo equipo = equipoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", id));
                validarPermisoGestion(equipo, correoUsuario);
                if (reservaRepository.countByEquipoId(id) > 0) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede eliminar el equipo: tiene reservas asociadas.");
                }
                if (!prestamoRepository.findByDetallesEquipoId(id).isEmpty()) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede eliminar el equipo: tiene préstamos asociados.");
                }
                if (!mantenimientoRepository.findByEquipoId(id).isEmpty()) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede eliminar el equipo: tiene mantenimientos asociados.");
                }
                if (!transferenciaRepository.findByEquipoId(id).isEmpty()) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede eliminar el equipo: tiene transferencias asociadas.");
                }
                List<FotoEquipo> fotos = fotoEquipoRepository.findByEquipoId(id);
                for (FotoEquipo foto : fotos) {
                        fotoEquipoRepository.delete(foto);
                        try {
                                Path archivoAEliminar = Paths.get(rutaUploads).resolve(
                                                foto.getRutaArchivo().replace("/uploads/", ""));
                                Files.deleteIfExists(archivoAEliminar);
                        } catch (IOException e) {
                                log.warn("No se pudo eliminar el archivo: {}", foto.getRutaArchivo(), e);
                        }
                }
                equipoRepository.delete(equipo);
        }

        @Transactional
        public FotoEquipoRespuestaDTO subirFoto(Long equipoId, MultipartFile archivo, String correoUsuario)
                        throws IOException {

                Equipo equipo = equipoRepository.findById(equipoId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));
                validarPermisoGestion(equipo, correoUsuario);

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
        public void eliminarFoto(Long equipoId, Long fotoId, String correoUsuario) {

                Equipo equipo = equipoRepository.findById(equipoId)
                                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));
                validarPermisoGestion(equipo, correoUsuario);

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
                                .placa(equipo.getPlaca())
                                .serial(equipo.getSerial())
                                .modelo(equipo.getModelo())
                                .marcaId(equipo.getMarca() != null ? equipo.getMarca().getId() : null)
                                .marcaNombre(equipo.getMarca() != null ? equipo.getMarca().getNombre() : null)
                                .estadoEquipoEscala(equipo.getEstadoEquipoEscala())
                                .codigoUnico(equipo.getCodigoUnico())
                                .categoriaId(equipo.getCategoria() != null
                                                ? equipo.getCategoria().getId()
                                                : null)
                                .categoriaNombre(equipo.getCategoria() != null
                                                ? equipo.getCategoria().getNombre()
                                                : null)
                                .ambienteId(equipo.getAmbiente() != null
                                                ? equipo.getAmbiente().getId()
                                                : null)
                                .ambienteNombre(equipo.getAmbiente() != null
                                                ? equipo.getAmbiente().getNombre()
                                                : null)
                                .subUbicacionId(equipo.getSubUbicacion() != null
                                                ? equipo.getSubUbicacion().getId()
                                                : null)
                                .subUbicacionNombre(equipo.getSubUbicacion() != null
                                                ? equipo.getSubUbicacion().getNombre()
                                                : null)
                                .propietarioId(equipo.getPropietario() != null
                                                ? equipo.getPropietario().getId()
                                                : null)
                                .propietarioNombre(equipo.getPropietario() != null
                                                ? equipo.getPropietario().getNombreCompleto()
                                                : null)
                                .inventarioActualInstructorId(equipo.getInventarioActualInstructor() != null
                                                ? equipo.getInventarioActualInstructor().getId()
                                                : null)
                                .inventarioActualInstructorNombre(equipo.getInventarioActualInstructor() != null
                                                ? equipo.getInventarioActualInstructor().getNombreCompleto()
                                                : null)
                                .estado(equipo.getEstado())
                                .cantidadTotal(equipo.getCantidadTotal())
                                .cantidadDisponible(equipo.getCantidadDisponible())
                                .tipoUso(equipo.getTipoUso())
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

        private Usuario resolverPropietarioParaCreacion(EquipoCrearDTO dto, Usuario usuarioActual) {
                if (usuarioActual.getRol() == Rol.INSTRUCTOR) {
                        return usuarioActual;
                }

                if (usuarioActual.getRol() == Rol.ADMINISTRADOR) {
                        return usuarioActual;
                }

                // ALIMENTADOR_EQUIPOS: debe indicar el instructor propietario explícitamente
                if (usuarioActual.getRol() == Rol.ALIMENTADOR_EQUIPOS) {
                        if (dto.getPropietarioId() == null) {
                                throw new OperacionNoPermitidaException(
                                                "Debes indicar el propietario del equipo (propietarioId) al crearlo.");
                        }
                        Usuario propietario = usuarioRepository.findById(dto.getPropietarioId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Instructor propietario no encontrado con ID: "
                                                                        + dto.getPropietarioId()));
                        if (propietario.getRol() != Rol.INSTRUCTOR) {
                                throw new OperacionNoPermitidaException(
                                                "El propietario del equipo debe tener rol INSTRUCTOR.");
                        }
                        return propietario;
                }

                throw new OperacionNoPermitidaException(
                                "Solo administradores, instructores o alimentadores de equipos pueden crear equipos.");
        }

        private Usuario obtenerUsuarioActual(String correoUsuario) {
                if (correoUsuario == null || correoUsuario.isBlank()) {
                        throw new OperacionNoPermitidaException("No se pudo identificar el usuario autenticado.");
                }
                return usuarioRepository.findByCorreoElectronico(correoUsuario)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Usuario no encontrado: " + correoUsuario));
        }

        private void validarPermisoGestion(Equipo equipo, String correoUsuario) {
                Usuario usuarioActual = obtenerUsuarioActual(correoUsuario);
                if (usuarioActual.getRol() == Rol.ADMINISTRADOR) {
                        return;
                }
                if (usuarioActual.getRol() == Rol.INSTRUCTOR
                                && equipo.getPropietario() != null
                                && equipo.getPropietario().getId().equals(usuarioActual.getId())) {
                        return;
                }
                throw new OperacionNoPermitidaException(
                                "Solo el instructor dueño del equipo o un administrador puede gestionarlo.");
        }
}
