package co.edu.sena.sigea.usuario.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.notificacion.repository.NotificacionRepository;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

/**
 * Tarea programada que elimina automáticamente los registros de usuarios
 * que llevan más de 24 horas en estado PENDIENTE sin que el administrador
 * haya tomado ninguna acción (aprobar o rechazar).
 */
@Service
public class LimpiezaUsuariosPendientesServicio {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaUsuariosPendientesServicio.class);

    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final ReservaRepository reservaRepository;
    private final AmbienteRepository ambienteRepository;

    public LimpiezaUsuariosPendientesServicio(UsuarioRepository usuarioRepository,
            NotificacionRepository notificacionRepository,
            ReservaRepository reservaRepository,
            AmbienteRepository ambienteRepository) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.reservaRepository = reservaRepository;
        this.ambienteRepository = ambienteRepository;
    }

    /**
     * Ejecuta cada hora. Elimina usuarios PENDIENTE con más de 24 h de antigüedad.
     * Cron: "0 0 * * * *" → cada hora en punto.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void eliminarPendientesExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusHours(24);

        List<Usuario> expirados = usuarioRepository
                .findByEstadoAprobacionAndFechaCreacionBefore(EstadoAprobacion.PENDIENTE, limite);

        if (expirados.isEmpty()) {
            return;
        }

        log.info("[SIGEA] Limpieza automática: eliminando {} usuario(s) PENDIENTE con más de 24 h sin acción.",
                expirados.size());

        for (Usuario u : expirados) {
            try {
                notificacionRepository.findByUsuarioDestinoId(u.getId())
                        .forEach(notificacionRepository::delete);
                reservaRepository.findByUsuarioId(u.getId())
                        .forEach(reservaRepository::delete);
                ambienteRepository.findByInstructorResponsableId(u.getId()).forEach(a -> {
                    a.setInstructorResponsable(null);
                    ambienteRepository.save(a);
                });
                usuarioRepository.delete(u);
                log.info("[SIGEA] Usuario PENDIENTE expirado eliminado: id={}, correo={}",
                        u.getId(), u.getCorreoElectronico());
            } catch (Exception e) {
                log.error("[SIGEA] Error al eliminar usuario pendiente id={}: {}", u.getId(), e.getMessage());
            }
        }
    }
}
