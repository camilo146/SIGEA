package co.edu.sena.sigea.notificacion.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoEnvio;
import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.common.enums.MedioEnvio;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.enums.TipoNotificacion;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.notificacion.dto.NotificacionRespuestaDTO;
import co.edu.sena.sigea.notificacion.entity.Notificacion;
import co.edu.sena.sigea.notificacion.repository.NotificacionRepository;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.reserva.entity.Reserva;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
@Transactional
public class NotificacionServicio {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServicio.class);

    private final NotificacionRepository notificacionRepository;
    private final PrestamoRepository prestamoRepository;
    private final EquipoRepository equipoRepository;
    private final CorreoServicio correoServicio;
    private final UsuarioRepository usuarioRepository;

    public NotificacionServicio(NotificacionRepository notificacionRepository,
                                PrestamoRepository prestamoRepository,
                                EquipoRepository equipoRepository,
                                CorreoServicio correoServicio,
                                UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.prestamoRepository = prestamoRepository;
        this.equipoRepository = equipoRepository;
        this.correoServicio = correoServicio;
        this.usuarioRepository = usuarioRepository;
    }

    // =========================================================================
    // NOTIFICAR SOLICITUD DE PRÉSTAMO (a admins/instructores y confirmación al solicitante)
    // =========================================================================
    public void notificarSolicitudPrestamo(Prestamo prestamo) {
        String tituloAdmin = "Nueva solicitud de préstamo";
        String mensajeAdmin = "El usuario " + prestamo.getUsuarioSolicitante().getNombreCompleto()
                + " ha solicitado un préstamo. ID: " + prestamo.getId();
        for (Usuario admin : usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR)) {
            crearYEnviar(admin, TipoNotificacion.SOLICITUD_PRESTAMO, tituloAdmin, mensajeAdmin);
        }
        for (Usuario instr : usuarioRepository.findByRolAndActivoTrue(Rol.INSTRUCTOR)) {
            crearYEnviar(instr, TipoNotificacion.SOLICITUD_PRESTAMO, tituloAdmin, mensajeAdmin);
        }
        String tituloUser = "Solicitud de préstamo recibida";
        String mensajeUser = "Tu solicitud de préstamo (ID: " + prestamo.getId() + ") fue registrada. Te notificaremos cuando sea revisada.";
        crearYEnviar(prestamo.getUsuarioSolicitante(), TipoNotificacion.GENERAL, tituloUser, mensajeUser);
    }

    // =========================================================================
    // NOTIFICAR APROBACIÓN
    // =========================================================================
    public void notificarAprobacion(Prestamo prestamo) {
        String titulo = "Tu préstamo fue aprobado";
        String mensaje = "Tu solicitud de préstamo (ID: " + prestamo.getId()
                + ") fue aprobada. Puedes pasar a recoger los equipos.";
        crearYEnviar(prestamo.getUsuarioSolicitante(),
                TipoNotificacion.GENERAL, titulo, mensaje);
    }

    // =========================================================================
    // NOTIFICAR RECHAZO
    // =========================================================================
    public void notificarRechazo(Prestamo prestamo) {
        String titulo = "Tu préstamo fue rechazado";
        String mensaje = "Tu solicitud de préstamo (ID: " + prestamo.getId()
                + ") fue rechazada por un administrador.";
        crearYEnviar(prestamo.getUsuarioSolicitante(),
                TipoNotificacion.GENERAL, titulo, mensaje);
    }

    // =========================================================================
    // NOTIFICAR RESERVA CREADA
    // =========================================================================
    public void notificarReservaCreada(Reserva reserva) {
        Usuario usuario = reserva.getUsuario();
        if (usuario.getCorreoElectronico() == null || usuario.getCorreoElectronico().isBlank()) {
            return;
        }
        String titulo = "Reserva registrada - SIGEA";
        String mensaje = "Hola " + usuario.getNombreCompleto() + ", tu reserva ha sido registrada.\n\n"
                + "Equipo: " + reserva.getEquipo().getNombre() + " (" + reserva.getEquipo().getCodigoUnico() + ")\n"
                + "Cantidad: " + reserva.getCantidad() + "\n"
                + "Fecha/hora de recogida: " + reserva.getFechaHoraInicio() + "\n"
                + "Recuerda: tienes 2 horas desde esa hora para recoger el equipo; de lo contrario la reserva vencerá.";
        crearYEnviar(usuario, TipoNotificacion.RESERVA_CREADA, titulo, mensaje);
    }

    // =========================================================================
    // NOTIFICAR EQUIPO RECOGIDO (reserva pasó a préstamo)
    // =========================================================================
    public void notificarEquipoRecogido(Reserva reserva, java.time.LocalDateTime fechaHoraDevolucion) {
        Usuario usuario = reserva.getUsuario();
        if (usuario.getCorreoElectronico() == null || usuario.getCorreoElectronico().isBlank()) {
            return;
        }
        String titulo = "Equipo recogido - Préstamo activo";
        String mensaje = "Hola " + usuario.getNombreCompleto() + ", se ha registrado la entrega de tu equipo.\n\n"
                + "Equipo: " + reserva.getEquipo().getNombre() + " (" + reserva.getEquipo().getCodigoUnico() + ")\n"
                + "Cantidad: " + reserva.getCantidad() + "\n"
                + "Fecha límite de devolución: " + fechaHoraDevolucion + "\n\n"
                + "Recuerda devolver el equipo a tiempo. Puedes ver el estado en Mis préstamos.";
        crearYEnviar(usuario, TipoNotificacion.EQUIPO_RECOGIDO, titulo, mensaje);
    }

    // =========================================================================
    // TAREA AUTOMÁTICA: detectarMoras — cada 15 min (ACTIVO → EN_MORA + notificación/email)
    // =========================================================================
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void detectarMoras() {
        log.info("Ejecutando tarea: detectar préstamos en mora...");
        LocalDateTime ahora = LocalDateTime.now();
        List<Prestamo> prestamosActivos = prestamoRepository.findByEstado(EstadoPrestamo.ACTIVO);
        for (Prestamo prestamo : prestamosActivos) {
            if (ahora.isAfter(prestamo.getFechaHoraDevolucionEstimada())) {
                prestamo.setEstado(EstadoPrestamo.EN_MORA);
                prestamoRepository.save(prestamo);
                String titulo = "Préstamo vencido - En mora";
                String mensaje = "Tu préstamo (ID: " + prestamo.getId()
                        + ") está vencido desde "
                        + prestamo.getFechaHoraDevolucionEstimada()
                        + ". Por favor devuelve los equipos inmediatamente.";
                crearYEnviar(prestamo.getUsuarioSolicitante(),
                        TipoNotificacion.MORA, titulo, mensaje);
            }
        }
        log.info("Tarea detectarMoras completada.");
    }

    // =========================================================================
    // TAREA AUTOMÁTICA: enviarRecordatorios — todos los días a las 9:00 AM
    // =========================================================================
    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void enviarRecordatorios() {
        log.info("Ejecutando tarea: enviar recordatorios de vencimiento...");
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime en48Horas = ahora.plusHours(48);
        List<Prestamo> proximosAVencer = prestamoRepository
                .findByFechaHoraDevolucionEstimadaBeforeAndEstado(en48Horas, EstadoPrestamo.ACTIVO);
        for (Prestamo prestamo : proximosAVencer) {
            if (ahora.isBefore(prestamo.getFechaHoraDevolucionEstimada())) {
                String titulo = "Recordatorio: devuelve tus equipos pronto";
                String mensaje = "Tu préstamo (ID: " + prestamo.getId()
                        + ") vence el " + prestamo.getFechaHoraDevolucionEstimada()
                        + ". Por favor planea la devolución a tiempo.";
                crearYEnviar(prestamo.getUsuarioSolicitante(),
                        TipoNotificacion.RECORDATORIO_VENCIMIENTO, titulo, mensaje);
            }
        }
        log.info("Tarea enviarRecordatorios completada.");
    }

    // =========================================================================
    // TAREA AUTOMÁTICA: verificarStockBajo — todos los lunes a las 7:00 AM
    // =========================================================================
    @Scheduled(cron = "0 0 7 * * MON")
    @Transactional
    public void verificarStockBajo() {
        log.info("Ejecutando tarea: verificar equipos con stock bajo...");
        List<Equipo> equiposBajos = equipoRepository
                .findByCantidadDisponibleLessThanEqualAndActivoTrue(2);
        for (Equipo equipo : equiposBajos) {
            if (equipo.getCantidadDisponible() <= equipo.getUmbralMinimo()) {
                String titulo = "Alerta: stock bajo - " + equipo.getNombre();
                String mensaje = "El equipo '" + equipo.getNombre()
                        + "' (Código: " + equipo.getCodigoUnico()
                        + ") tiene solo " + equipo.getCantidadDisponible()
                        + " unidades disponibles. Se recomienda gestionar reposición.";
                if (equipo.getAmbiente() != null
                        && equipo.getAmbiente().getInstructorResponsable() != null) {
                    crearYEnviar(equipo.getAmbiente().getInstructorResponsable(),
                            TipoNotificacion.STOCK_BAJO, titulo, mensaje);
                }
            }
        }
        log.info("Tarea verificarStockBajo completada.");
    }

    // =========================================================================
    // MÉTODOS DE CONSULTA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<NotificacionRespuestaDTO> listarPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioDestinoId(usuarioId)
                .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionRespuestaDTO> listarNoLeidasPorUsuario(Long usuarioId) {
        return notificacionRepository.findByUsuarioDestinoIdAndLeidaFalse(usuarioId)
                .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepository.countByUsuarioDestinoIdAndLeidaFalse(usuarioId);
    }

    @Transactional(readOnly = true)
    public List<NotificacionRespuestaDTO> listarMisNotificaciones(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correo));
        return notificacionRepository.findByUsuarioDestinoId(usuario.getId())
                .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificacionRespuestaDTO> listarMisNoLeidas(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correo));
        return notificacionRepository.findByUsuarioDestinoIdAndLeidaFalse(usuario.getId())
                .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public long contarMisNoLeidas(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correo));
        return notificacionRepository.countByUsuarioDestinoIdAndLeidaFalse(usuario.getId());
    }

    public void marcarComoLeida(Long notificacionId, String correoUsuario) {
        Notificacion notificacion = notificacionRepository.findById(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Notificación no encontrada con ID: " + notificacionId));
        if (notificacion.getUsuarioDestino() == null || !notificacion.getUsuarioDestino().getCorreoElectronico().equals(correoUsuario)) {
            throw new OperacionNoPermitidaException("No puedes marcar esta notificación como leída.");
        }
        notificacion.setLeida(true);
        notificacionRepository.save(notificacion);
    }

    // =========================================================================
    // MÉTODO PRIVADO: crearYEnviar
    // =========================================================================
    private void crearYEnviar(Usuario destinatario, TipoNotificacion tipo,
                               String titulo, String mensaje) {
        if (destinatario == null) return;
        Notificacion notif = Notificacion.builder()
                .usuarioDestino(destinatario)
                .tipo(tipo)
                .titulo(titulo)
                .mensaje(mensaje)
                .medioEnvio(MedioEnvio.EMAIL)
                .estadoEnvio(EstadoEnvio.PENDIENTE)
                .leida(false)
                .build();
        notif = notificacionRepository.save(notif);
        boolean enviado = false;
        if (destinatario.getCorreoElectronico() != null && !destinatario.getCorreoElectronico().isBlank()) {
            enviado = correoServicio.enviarCorreo(
                    destinatario.getCorreoElectronico(), titulo, mensaje);
        }
        notif.setEstadoEnvio(enviado ? EstadoEnvio.ENVIADA : EstadoEnvio.FALLIDA);
        notif.setFechaEnvio(LocalDateTime.now());
        notificacionRepository.save(notif);
    }

    // =========================================================================
    // MÉTODO PRIVADO: mapear
    // =========================================================================
    private NotificacionRespuestaDTO mapear(Notificacion n) {
        return NotificacionRespuestaDTO.builder()
                .id(n.getId())
                .usuarioDestinoId(n.getUsuarioDestino().getId())
                .nombreUsuarioDestino(n.getUsuarioDestino().getNombreCompleto())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .tipoNotificacion(n.getTipo())
                .medioEnvio(n.getMedioEnvio())
                .estadoEnvio(n.getEstadoEnvio())
                .leida(n.getLeida())
                .fechaEnvio(n.getFechaEnvio())
                .fechaCreacion(n.getFechaCreacion())
                .build();
    }
}
