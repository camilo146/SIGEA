package co.edu.sena.sigea.prestamoambiente.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteDevolucionDTO;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteRespuestaDTO;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteSolicitudDTO;
import co.edu.sena.sigea.prestamoambiente.entity.PrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.repository.PrestamoAmbienteRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrestamoAmbienteServicio {

    private final PrestamoAmbienteRepository prestamoAmbienteRepository;
    private final AmbienteRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoServicio correoServicio;

    @Transactional
    public PrestamoAmbienteRespuestaDTO solicitar(PrestamoAmbienteSolicitudDTO dto, String correoSolicitante) {

        if (!dto.getFechaFin().isAfter(dto.getFechaInicio()) && !dto.getFechaFin().equals(dto.getFechaInicio())) {
            throw new OperacionNoPermitidaException("La fecha de fin debe ser igual o posterior a la fecha de inicio");
        }
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new OperacionNoPermitidaException("La hora de fin debe ser posterior a la hora de inicio");
        }

        Ambiente ambiente = ambienteRepository.findById(dto.getAmbienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Ambiente no encontrado con ID: " + dto.getAmbienteId()));

        if (!Boolean.TRUE.equals(ambiente.getActivo())) {
            throw new OperacionNoPermitidaException("El ambiente seleccionado no está activo");
        }

        // Verificar solapamiento de horario
        boolean solapamiento = prestamoAmbienteRepository.existeSolapamiento(
                dto.getAmbienteId(),
                dto.getFechaInicio(), dto.getFechaFin(),
                dto.getHoraInicio(), dto.getHoraFin(),
                null);

        if (solapamiento) {
            throw new OperacionNoPermitidaException(
                    "El ambiente ya tiene un préstamo aprobado o activo en ese horario");
        }

        Usuario solicitante = usuarioRepository.findByCorreoElectronico(correoSolicitante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoSolicitante));

        // El propietario del ambiente es su instructor/responsable asignado
        Usuario propietario = resolverPropietarioAmbiente(ambiente);

        PrestamoAmbiente prestamo = PrestamoAmbiente.builder()
                .ambiente(ambiente)
                .solicitante(solicitante)
                .propietarioAmbiente(propietario)
                .fechaInicio(dto.getFechaInicio())
                .fechaFin(dto.getFechaFin())
                .horaInicio(dto.getHoraInicio())
                .horaFin(dto.getHoraFin())
                .proposito(dto.getProposito())
                .numeroParticipantes(dto.getNumeroParticipantes())
                .tipoActividad(dto.getTipoActividad())
                .observacionesSolicitud(dto.getObservacionesSolicitud())
                .estado(EstadoPrestamoAmbiente.SOLICITADO)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        PrestamoAmbiente guardado = prestamoAmbienteRepository.save(prestamo);

        // Notificar al propietario del ambiente
        correoServicio.enviarCorreo(
                propietario.getCorreoElectronico(),
                "Nueva solicitud de préstamo de ambiente: " + ambiente.getNombre(),
                String.format(
                        "El usuario %s ha solicitado préstamo del ambiente '%s' para el %s de %s a %s.\nPropósito: %s",
                        solicitante.getNombreCompleto(), ambiente.getNombre(),
                        dto.getFechaInicio(), dto.getHoraInicio(), dto.getHoraFin(),
                        dto.getProposito()));

        return convertirADTO(guardado);
    }

    @Transactional
    public PrestamoAmbienteRespuestaDTO aprobar(Long id, String correoAprobador) {
        PrestamoAmbiente prestamo = obtenerPrestamoOException(id);

        if (prestamo.getEstado() != EstadoPrestamoAmbiente.SOLICITADO) {
            throw new OperacionNoPermitidaException("Solo se pueden aprobar solicitudes en estado SOLICITADO");
        }

        validarEsPropietarioOAdmin(prestamo, correoAprobador);

        // Verificar que no haya solapamiento al aprobar
        boolean solapamiento = prestamoAmbienteRepository.existeSolapamiento(
                prestamo.getAmbiente().getId(),
                prestamo.getFechaInicio(), prestamo.getFechaFin(),
                prestamo.getHoraInicio(), prestamo.getHoraFin(),
                id);

        if (solapamiento) {
            throw new OperacionNoPermitidaException(
                    "No se puede aprobar: existe solapamiento con otro préstamo aprobado en ese horario");
        }

        prestamo.setEstado(EstadoPrestamoAmbiente.APROBADO);
        prestamo.setFechaAprobacion(LocalDateTime.now());
        PrestamoAmbiente actualizado = prestamoAmbienteRepository.save(prestamo);

        correoServicio.enviarCorreo(
                prestamo.getSolicitante().getCorreoElectronico(),
                "Préstamo de ambiente aprobado: " + prestamo.getAmbiente().getNombre(),
                String.format("Su solicitud de préstamo del ambiente '%s' para el %s ha sido APROBADA.",
                        prestamo.getAmbiente().getNombre(), prestamo.getFechaInicio()));

        return convertirADTO(actualizado);
    }

    @Transactional
    public PrestamoAmbienteRespuestaDTO rechazar(Long id, String correoAprobador, String motivo) {
        PrestamoAmbiente prestamo = obtenerPrestamoOException(id);

        if (prestamo.getEstado() != EstadoPrestamoAmbiente.SOLICITADO) {
            throw new OperacionNoPermitidaException("Solo se pueden rechazar solicitudes en estado SOLICITADO");
        }

        validarEsPropietarioOAdmin(prestamo, correoAprobador);

        prestamo.setEstado(EstadoPrestamoAmbiente.RECHAZADO);
        prestamo.setObservacionesSolicitud(
                (prestamo.getObservacionesSolicitud() != null ? prestamo.getObservacionesSolicitud() + "\n" : "")
                        + "Motivo de rechazo: " + motivo);
        PrestamoAmbiente actualizado = prestamoAmbienteRepository.save(prestamo);

        correoServicio.enviarCorreo(
                prestamo.getSolicitante().getCorreoElectronico(),
                "Préstamo de ambiente rechazado: " + prestamo.getAmbiente().getNombre(),
                String.format("Su solicitud de préstamo del ambiente '%s' para el %s fue RECHAZADA.\nMotivo: %s",
                        prestamo.getAmbiente().getNombre(), prestamo.getFechaInicio(), motivo));

        return convertirADTO(actualizado);
    }

    @Transactional
    public PrestamoAmbienteRespuestaDTO registrarDevolucion(Long id, PrestamoAmbienteDevolucionDTO dto,
            String correoUsuario) {
        PrestamoAmbiente prestamo = obtenerPrestamoOException(id);

        if (prestamo.getEstado() != EstadoPrestamoAmbiente.APROBADO
                && prestamo.getEstado() != EstadoPrestamoAmbiente.ACTIVO) {
            throw new OperacionNoPermitidaException(
                    "Solo se pueden registrar devoluciones de préstamos aprobados o activos");
        }

        prestamo.setEstado(EstadoPrestamoAmbiente.DEVUELTO);
        prestamo.setObservacionesDevolucion(dto.getObservacionesDevolucion());
        prestamo.setEstadoDevolucionAmbiente(dto.getEstadoDevolucionAmbiente());
        prestamo.setFechaDevolucion(LocalDateTime.now());
        PrestamoAmbiente actualizado = prestamoAmbienteRepository.save(prestamo);

        return convertirADTO(actualizado);
    }

    @Transactional
    public PrestamoAmbienteRespuestaDTO cancelar(Long id, String correoSolicitante) {
        PrestamoAmbiente prestamo = obtenerPrestamoOException(id);

        if (prestamo.getEstado() == EstadoPrestamoAmbiente.DEVUELTO
                || prestamo.getEstado() == EstadoPrestamoAmbiente.CANCELADO) {
            throw new OperacionNoPermitidaException("El préstamo no puede ser cancelado en su estado actual");
        }

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoSolicitante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoSolicitante));

        // Solo el solicitante o un admin puede cancelar
        boolean esSolicitante = prestamo.getSolicitante().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRol().name().equals("ADMINISTRADOR");
        if (!esSolicitante && !esAdmin) {
            throw new OperacionNoPermitidaException("No tiene permisos para cancelar este préstamo");
        }

        prestamo.setEstado(EstadoPrestamoAmbiente.CANCELADO);
        return convertirADTO(prestamoAmbienteRepository.save(prestamo));
    }

    @Transactional(readOnly = true)
    public List<PrestamoAmbienteRespuestaDTO> listarMisSolicitudes(String correoSolicitante) {
        Usuario solicitante = usuarioRepository.findByCorreoElectronico(correoSolicitante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoSolicitante));

        return prestamoAmbienteRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(solicitante.getId())
                .stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public PrestamoAmbienteRespuestaDTO buscarPorId(Long id) {
        return convertirADTO(obtenerPrestamoOException(id));
    }

    @Transactional(readOnly = true)
    public List<PrestamoAmbienteRespuestaDTO> listarPorSolicitante(Long solicitanteId) {
        return prestamoAmbienteRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(solicitanteId)
                .stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoAmbienteRespuestaDTO> listarPorAmbiente(Long ambienteId) {
        return prestamoAmbienteRepository.findByAmbienteIdOrderByFechaInicioDesc(ambienteId)
                .stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoAmbienteRespuestaDTO> listarPorPropietario(Long propietarioId) {
        return prestamoAmbienteRepository.findByPropietarioAmbienteIdOrderByFechaSolicitudDesc(propietarioId)
                .stream().map(this::convertirADTO).toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoAmbienteRespuestaDTO> listarPorEstado(EstadoPrestamoAmbiente estado) {
        return prestamoAmbienteRepository.findByEstadoOrderByFechaSolicitudDesc(estado)
                .stream().map(this::convertirADTO).toList();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PrestamoAmbiente obtenerPrestamoOException(Long id) {
        return prestamoAmbienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Préstamo de ambiente no encontrado con ID: " + id));
    }

    private void validarEsPropietarioOAdmin(PrestamoAmbiente prestamo, String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoUsuario));

        boolean esPropietario = prestamo.getPropietarioAmbiente().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRol().name().equals("ADMINISTRADOR");

        if (!esPropietario && !esAdmin) {
            throw new OperacionNoPermitidaException(
                    "Solo el propietario del ambiente o un administrador puede realizar esta acción");
        }
    }

    /**
     * Resuelve el propietario/responsable del ambiente.
     * Debe existir un instructor responsable activo en el ambiente.
     */
    private Usuario resolverPropietarioAmbiente(Ambiente ambiente) {
        Usuario instructorResponsable = ambiente.getInstructorResponsable();
        if (instructorResponsable == null) {
            throw new OperacionNoPermitidaException(
                    "El ambiente seleccionado no tiene un instructor responsable asignado");
        }
        if (!Boolean.TRUE.equals(instructorResponsable.getActivo())) {
            throw new OperacionNoPermitidaException(
                    "El instructor responsable del ambiente se encuentra inactivo");
        }
        return instructorResponsable;
    }

    private PrestamoAmbienteRespuestaDTO convertirADTO(PrestamoAmbiente p) {
        return PrestamoAmbienteRespuestaDTO.builder()
                .id(p.getId())
                .ambienteId(p.getAmbiente() != null ? p.getAmbiente().getId() : null)
                .ambienteNombre(p.getAmbiente() != null ? p.getAmbiente().getNombre() : null)
                .solicitanteId(p.getSolicitante() != null ? p.getSolicitante().getId() : null)
                .solicitanteNombre(p.getSolicitante() != null ? p.getSolicitante().getNombreCompleto() : null)
                .propietarioAmbienteId(p.getPropietarioAmbiente() != null ? p.getPropietarioAmbiente().getId() : null)
                .propietarioAmbienteNombre(
                        p.getPropietarioAmbiente() != null ? p.getPropietarioAmbiente().getNombreCompleto() : null)
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .horaInicio(p.getHoraInicio())
                .horaFin(p.getHoraFin())
                .proposito(p.getProposito())
                .numeroParticipantes(p.getNumeroParticipantes())
                .tipoActividad(p.getTipoActividad())
                .observacionesSolicitud(p.getObservacionesSolicitud())
                .estado(p.getEstado())
                .observacionesDevolucion(p.getObservacionesDevolucion())
                .estadoDevolucionAmbiente(p.getEstadoDevolucionAmbiente())
                .fechaSolicitud(p.getFechaSolicitud())
                .fechaAprobacion(p.getFechaAprobacion())
                .fechaDevolucion(p.getFechaDevolucion())
                .fechaCreacion(p.getFechaCreacion())
                .fechaActualizacion(p.getFechaActualizacion())
                .build();
    }
}
