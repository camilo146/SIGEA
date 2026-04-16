package co.edu.sena.sigea.transferencia.service;

// =============================================================================
// SERVICIO: TransferenciaServicio
// =============================================================================
// Lógica de negocio para transferencias de equipos entre ambientes.
// RF-AMB-04: Transferir equipos entre ambientes con origen, destino, equipo,
//            fecha, admin que autoriza, motivo.
// RN-10: Las transferencias deben ser autorizadas por el admin del origen.
// =============================================================================

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.categoria.entity.Categoria;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.transferencia.dto.TransferenciaCrearDTO;
import co.edu.sena.sigea.transferencia.dto.TransferenciaRespuestaDTO;
import co.edu.sena.sigea.transferencia.entity.Transferencia;
import co.edu.sena.sigea.transferencia.repository.TransferenciaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
@Transactional
public class TransferenciaServicio {

        private final TransferenciaRepository transferenciaRepository;
        private final EquipoRepository equipoRepository;
        private final AmbienteRepository ambienteRepository;
        private final UsuarioRepository usuarioRepository;

        public TransferenciaServicio(TransferenciaRepository transferenciaRepository,
                        EquipoRepository equipoRepository,
                        AmbienteRepository ambienteRepository,
                        UsuarioRepository usuarioRepository) {
                this.transferenciaRepository = transferenciaRepository;
                this.equipoRepository = equipoRepository;
                this.ambienteRepository = ambienteRepository;
                this.usuarioRepository = usuarioRepository;
        }

        public TransferenciaRespuestaDTO crear(TransferenciaCrearDTO dto, String correoAdministrador) {

                Usuario usuarioActual = usuarioRepository.findByIdentificador(correoAdministrador)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Usuario no encontrado: " + correoAdministrador));

                Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Equipo no encontrado con ID: " + dto.getEquipoId()));

                if (!esAdministrador(usuarioActual)
                                && (equipo.getPropietario() == null
                                                || !equipo.getPropietario().getId().equals(usuarioActual.getId()))) {
                        throw new OperacionNoPermitidaException(
                                        "Solo el instructor dueño del equipo o un administrador puede transferirlo.");
                }

                Usuario inventarioOrigen = equipo.getInventarioActualInstructor();
                if (inventarioOrigen == null) {
                        throw new OperacionNoPermitidaException("El equipo no tiene inventario origen definido.");
                }

                Usuario inventarioDestino = usuarioRepository.findById(dto.getInstructorDestinoId())
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Instructor destino no encontrado con ID: "
                                                                + dto.getInstructorDestinoId()));

                if (inventarioDestino.getRol() != Rol.INSTRUCTOR) {
                        throw new OperacionNoPermitidaException(
                                        "El inventario destino debe pertenecer a un INSTRUCTOR.");
                }

                if (inventarioOrigen.getId().equals(inventarioDestino.getId())) {
                        throw new OperacionNoPermitidaException(
                                        "El inventario de origen y destino no pueden ser el mismo.");
                }

                Ambiente ubicacionDestino = null;
                if (dto.getUbicacionDestinoId() != null) {
                        ubicacionDestino = ambienteRepository.findById(dto.getUbicacionDestinoId())
                                        .orElseThrow(() -> new RecursoNoEncontradoException(
                                                        "Ubicacion destino no encontrada con ID: "
                                                                        + dto.getUbicacionDestinoId()));
                }

                if (!Boolean.TRUE.equals(equipo.getActivo())) {
                        throw new OperacionNoPermitidaException(
                                        "No se puede transferir el equipo '" + equipo.getNombre()
                                                        + "' porque no está activo.");
                }

                Integer cantidadDto = dto.getCantidad();
                int cantidad = cantidadDto != null ? cantidadDto.intValue() : 1;
                if (cantidad > equipo.getCantidadDisponible()) {
                        throw new OperacionNoPermitidaException(
                                        "Cantidad solicitada (" + cantidad + ") supera la disponible ("
                                                        + equipo.getCantidadDisponible() + ") para el equipo '"
                                                        + equipo.getNombre() + "'.");
                }

                Equipo equipoTransferido;
                if (cantidad < equipo.getCantidadTotal()) {
                        equipoTransferido = crearEquipoTransferidoParcial(equipo, cantidad, inventarioDestino,
                                        ubicacionDestino);

                        equipo.setCantidadTotal(equipo.getCantidadTotal() - cantidad);
                        equipo.setCantidadDisponible(equipo.getCantidadDisponible() - cantidad);
                        equipoRepository.save(equipo);
                } else {
                        equipo.setInventarioActualInstructor(inventarioDestino);
                        if (ubicacionDestino != null) {
                                equipo.setAmbiente(ubicacionDestino);
                        }
                        equipoTransferido = equipoRepository.save(equipo);
                }

                Transferencia transferencia = Transferencia.builder()
                                .equipo(equipoTransferido)
                                .inventarioOrigenInstructor(inventarioOrigen)
                                .inventarioDestinoInstructor(inventarioDestino)
                                .propietarioEquipo(equipo.getPropietario())
                                .ubicacionDestino(ubicacionDestino)
                                .cantidad(cantidad)
                                .administradorAutoriza(usuarioActual)
                                .motivo(dto.getMotivo())
                                .fechaTransferencia(dto.getFechaTransferencia())
                                .build();

                Transferencia guardada = transferenciaRepository.save(transferencia);

                return mapear(guardada);
        }

        @Transactional(readOnly = true)
        public List<TransferenciaRespuestaDTO> listarTodos() {
                return transferenciaRepository.findAll().stream()
                                .map(this::mapear)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TransferenciaRespuestaDTO> listarPorEquipo(Long equipoId) {
                return transferenciaRepository.findByEquipoId(equipoId).stream()
                                .map(this::mapear)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TransferenciaRespuestaDTO> listarPorInstructorOrigen(Long instructorId) {
                return transferenciaRepository.findByInventarioOrigenInstructorId(instructorId).stream()
                                .map(this::mapear)
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<TransferenciaRespuestaDTO> listarPorInstructorDestino(Long instructorId) {
                return transferenciaRepository.findByInventarioDestinoInstructorId(instructorId).stream()
                                .map(this::mapear)
                                .toList();
        }

        @Transactional(readOnly = true)
        public TransferenciaRespuestaDTO buscarPorId(Long id) {
                Transferencia t = transferenciaRepository.findById(id)
                                .orElseThrow(() -> new RecursoNoEncontradoException(
                                                "Transferencia no encontrada con ID: " + id));
                return mapear(t);
        }

        private TransferenciaRespuestaDTO mapear(Transferencia t) {
                return TransferenciaRespuestaDTO.builder()
                                .id(t.getId())
                                .equipoId(t.getEquipo().getId())
                                .nombreEquipo(t.getEquipo().getNombre())
                                .codigoEquipo(t.getEquipo().getCodigoUnico())
                                .inventarioOrigenInstructorId(t.getInventarioOrigenInstructor().getId())
                                .nombreInventarioOrigenInstructor(t.getInventarioOrigenInstructor().getNombreCompleto())
                                .inventarioDestinoInstructorId(t.getInventarioDestinoInstructor().getId())
                                .nombreInventarioDestinoInstructor(
                                                t.getInventarioDestinoInstructor().getNombreCompleto())
                                .propietarioEquipoId(t.getPropietarioEquipo() != null ? t.getPropietarioEquipo().getId()
                                                : null)
                                .nombrePropietarioEquipo(t.getPropietarioEquipo() != null
                                                ? t.getPropietarioEquipo().getNombreCompleto()
                                                : null)
                                .ubicacionDestinoId(t.getUbicacionDestino() != null ? t.getUbicacionDestino().getId()
                                                : null)
                                .nombreUbicacionDestino(
                                                t.getUbicacionDestino() != null ? t.getUbicacionDestino().getNombre()
                                                                : null)
                                .cantidad(t.getCantidad())
                                .administradorAutorizaId(t.getAdministradorAutoriza().getId())
                                .nombreAdministrador(t.getAdministradorAutoriza().getNombreCompleto())
                                .motivo(t.getMotivo())
                                .fechaTransferencia(t.getFechaTransferencia())
                                .fechaCreacion(t.getFechaCreacion())
                                .build();
        }

        private boolean esAdministrador(Usuario usuario) {
                return usuario.getRol() == Rol.ADMINISTRADOR;
        }

        private Equipo crearEquipoTransferidoParcial(Equipo origen,
                        int cantidad,
                        Usuario inventarioDestino,
                        Ambiente ubicacionDestino) {
                Categoria categoria = origen.getCategoria();
                EstadoEquipo estado = origen.getEstado();
                String codigoNuevo = generarCodigoTransferido(origen.getCodigoUnico());

                Equipo nuevo = Equipo.builder()
                                .nombre(origen.getNombre())
                                .descripcion(origen.getDescripcion())
                                .codigoUnico(codigoNuevo)
                                .categoria(categoria)
                                .estado(estado)
                                .cantidadTotal(cantidad)
                                .cantidadDisponible(cantidad)
                                .ambiente(ubicacionDestino != null ? ubicacionDestino : origen.getAmbiente())
                                .umbralMinimo(origen.getUmbralMinimo())
                                .activo(origen.getActivo())
                                .propietario(origen.getPropietario())
                                .inventarioActualInstructor(inventarioDestino)
                                .build();

                return equipoRepository.save(nuevo);
        }

        private String generarCodigoTransferido(String codigoBase) {
                String candidato = codigoBase + "-TR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                while (equipoRepository.existsByCodigoUnico(candidato)) {
                        candidato = codigoBase + "-TR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                }
                return candidato;
        }
}
