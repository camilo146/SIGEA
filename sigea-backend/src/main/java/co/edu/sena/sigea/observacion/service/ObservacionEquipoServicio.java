package co.edu.sena.sigea.observacion.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.observacion.dto.ObservacionEquipoCrearDTO;
import co.edu.sena.sigea.observacion.dto.ObservacionEquipoRespuestaDTO;
import co.edu.sena.sigea.observacion.entity.ObservacionEquipo;
import co.edu.sena.sigea.observacion.repository.ObservacionEquipoRepository;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ObservacionEquipoServicio {

    private final ObservacionEquipoRepository observacionRepository;
    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ObservacionEquipoRespuestaDTO registrar(ObservacionEquipoCrearDTO dto, String correoUsuario) {

        Prestamo prestamo = prestamoRepository.findById(dto.getPrestamoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Préstamo no encontrado con ID: " + dto.getPrestamoId()));

        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", dto.getEquipoId()));

        Usuario registrador = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoUsuario));

        // El registrador es el propietario del equipo (quien recibe la devolución)
        Usuario propietario = equipo.getPropietario();
        if (propietario == null) {
            throw new OperacionNoPermitidaException("El equipo no tiene un propietario asignado");
        }

        // El prestatario es el solicitante del préstamo
        Usuario prestatario = prestamo.getUsuarioSolicitante();

        ObservacionEquipo observacion = ObservacionEquipo.builder()
                .prestamo(prestamo)
                .equipo(equipo)
                .usuarioDuenio(propietario)
                .usuarioPrestatario(prestatario)
                .observaciones(dto.getObservaciones())
                .estadoDevolucion(dto.getEstadoDevolucion())
                .fechaRegistro(LocalDateTime.now())
                .build();

        // Actualizar la escala de estado del equipo con la última evaluación
        equipo.setEstadoEquipoEscala(dto.getEstadoDevolucion());
        equipoRepository.save(equipo);

        ObservacionEquipo guardada = observacionRepository.save(observacion);
        return convertirADTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<ObservacionEquipoRespuestaDTO> listarPorEquipo(Long equipoId) {
        equipoRepository.findById(equipoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Equipo", equipoId));
        return observacionRepository.findByEquipoIdOrderByFechaRegistroDesc(equipoId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ObservacionEquipoRespuestaDTO> listarPorPrestamo(Long prestamoId) {
        return observacionRepository.findByPrestamoId(prestamoId)
                .stream()
                .map(this::convertirADTO)
                .toList();
    }

    private ObservacionEquipoRespuestaDTO convertirADTO(ObservacionEquipo obs) {
        return ObservacionEquipoRespuestaDTO.builder()
                .id(obs.getId())
                .prestamoId(obs.getPrestamo() != null ? obs.getPrestamo().getId() : null)
                .equipoId(obs.getEquipo() != null ? obs.getEquipo().getId() : null)
                .equipoNombre(obs.getEquipo() != null ? obs.getEquipo().getNombre() : null)
                .equipoPlaca(obs.getEquipo() != null ? obs.getEquipo().getPlaca() : null)
                .usuarioDuenioId(obs.getUsuarioDuenio() != null ? obs.getUsuarioDuenio().getId() : null)
                .usuarioDuenioNombre(obs.getUsuarioDuenio() != null ? obs.getUsuarioDuenio().getNombreCompleto() : null)
                .usuarioPrestatarioId(obs.getUsuarioPrestatario() != null ? obs.getUsuarioPrestatario().getId() : null)
                .usuarioPrestatarioNombre(
                        obs.getUsuarioPrestatario() != null ? obs.getUsuarioPrestatario().getNombreCompleto() : null)
                .observaciones(obs.getObservaciones())
                .estadoDevolucion(obs.getEstadoDevolucion())
                .fechaRegistro(obs.getFechaRegistro())
                .fechaCreacion(obs.getFechaCreacion())
                .build();
    }
}
