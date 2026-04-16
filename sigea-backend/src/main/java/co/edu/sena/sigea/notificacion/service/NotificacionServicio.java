package co.edu.sena.sigea.notificacion.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
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
    // NOTIFICAR SOLICITUD DE PRÉSTAMO (a admins/instructores y confirmación al
    // solicitante)
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
        String mensajeUser = "Tu solicitud de préstamo (ID: " + prestamo.getId()
                + ") fue registrada. Te notificaremos cuando sea revisada.";
        crearYEnviar(prestamo.getUsuarioSolicitante(), TipoNotificacion.GENERAL, tituloUser, mensajeUser);
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.SOLICITUD_PRESTAMO,
                "Solicitud de préstamo sobre equipos a tu cargo",
                "Se registró la solicitud de préstamo ID " + prestamo.getId()
                        + " para equipos bajo tu responsabilidad.");
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
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.GENERAL,
                "Préstamo aprobado sobre equipos a tu cargo",
                "La solicitud de préstamo ID " + prestamo.getId() + " fue aprobada.");
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
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.GENERAL,
                "Préstamo rechazado sobre equipos a tu cargo",
                "La solicitud de préstamo ID " + prestamo.getId() + " fue rechazada.");
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

        Usuario duenio = resolverDuenioEquipo(reserva.getEquipo());
        if (duenio != null && !duenio.getId().equals(usuario.getId())) {
            crearYEnviar(
                    duenio,
                    TipoNotificacion.RESERVA_CREADA,
                    "Nueva reserva sobre equipo a tu cargo",
                    "El usuario " + usuario.getNombreCompleto() + " reservó el equipo "
                            + reserva.getEquipo().getNombre() + " para " + reserva.getFechaHoraInicio() + ".");
        }
    }

    public void notificarReservaCancelada(Reserva reserva) {
        crearYEnviar(
                reserva.getUsuario(),
                TipoNotificacion.RESERVA_CANCELADA,
                "Reserva cancelada",
                "Tu reserva del equipo " + reserva.getEquipo().getNombre() + " fue cancelada correctamente.");

        Usuario duenio = resolverDuenioEquipo(reserva.getEquipo());
        if (duenio != null && !duenio.getId().equals(reserva.getUsuario().getId())) {
            crearYEnviar(
                    duenio,
                    TipoNotificacion.RESERVA_CANCELADA,
                    "Reserva cancelada sobre equipo a tu cargo",
                    "La reserva del equipo " + reserva.getEquipo().getNombre() + " realizada por "
                            + reserva.getUsuario().getNombreCompleto() + " fue cancelada.");
        }
    }

    public void notificarReservaExpirada(Reserva reserva) {
        crearYEnviar(
                reserva.getUsuario(),
                TipoNotificacion.RESERVA_EXPIRADA,
                "Reserva expirada",
                "Tu reserva del equipo " + reserva.getEquipo().getNombre()
                        + " expiró porque no fue recogida dentro de la ventana permitida.");

        Usuario duenio = resolverDuenioEquipo(reserva.getEquipo());
        if (duenio != null && !duenio.getId().equals(reserva.getUsuario().getId())) {
            crearYEnviar(
                    duenio,
                    TipoNotificacion.RESERVA_EXPIRADA,
                    "Reserva expirada sobre equipo a tu cargo",
                    "La reserva del equipo " + reserva.getEquipo().getNombre() + " realizada por "
                            + reserva.getUsuario().getNombreCompleto() + " expiró por no recogida.");
        }
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

        Usuario duenio = resolverDuenioEquipo(reserva.getEquipo());
        if (duenio != null && !duenio.getId().equals(usuario.getId())) {
            crearYEnviar(
                    duenio,
                    TipoNotificacion.EQUIPO_RECOGIDO,
                    "Equipo entregado desde reserva",
                    "Se registró la entrega del equipo " + reserva.getEquipo().getNombre() + " al usuario "
                            + usuario.getNombreCompleto() + ". Fecha límite de devolución: " + fechaHoraDevolucion
                            + ".");
        }
    }

    public void notificarSalidaPrestamo(Prestamo prestamo) {
        crearYEnviar(
                prestamo.getUsuarioSolicitante(),
                TipoNotificacion.PRESTAMO_SALIDA,
                "Préstamo entregado",
                "Tu préstamo ID " + prestamo.getId() + " ya fue entregado y quedó activo.");
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.PRESTAMO_SALIDA,
                "Equipos entregados en préstamo",
                "El préstamo ID " + prestamo.getId() + " ya fue entregado al solicitante.");
    }

    public void notificarPrestamoDevuelto(Prestamo prestamo) {
        crearYEnviar(
                prestamo.getUsuarioSolicitante(),
                TipoNotificacion.PRESTAMO_DEVUELTO,
                "Préstamo devuelto",
                "Tu préstamo ID " + prestamo.getId() + " fue recibido y cerrado correctamente.");
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.PRESTAMO_DEVUELTO,
                "Equipos devueltos",
                "El préstamo ID " + prestamo.getId() + " fue devuelto y registrado correctamente.");
    }

    public void notificarPrestamoCancelado(Prestamo prestamo) {
        crearYEnviar(
                prestamo.getUsuarioSolicitante(),
                TipoNotificacion.PRESTAMO_CANCELADO,
                "Solicitud de préstamo cancelada",
                "La solicitud de préstamo ID " + prestamo.getId() + " fue cancelada correctamente.");
        notificarUsuarios(
                resolverDueniosPrestamo(prestamo),
                TipoNotificacion.PRESTAMO_CANCELADO,
                "Solicitud cancelada sobre equipos a tu cargo",
                "La solicitud de préstamo ID " + prestamo.getId() + " fue cancelada por el solicitante.");
    }

    // =========================================================================
    // TAREA AUTOMÁTICA: detectarMoras — cada 15 min (ACTIVO → EN_MORA +
    // notificación/email)
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
                notificarUsuarios(
                        resolverDueniosPrestamo(prestamo),
                        TipoNotificacion.MORA,
                        "Préstamo en mora sobre equipos a tu cargo",
                        "El préstamo ID " + prestamo.getId() + " entró en mora.");
                notificarUsuarios(
                        usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR),
                        TipoNotificacion.MORA,
                        "Préstamo en mora detectado",
                        "El préstamo ID " + prestamo.getId() + " del usuario "
                                + prestamo.getUsuarioSolicitante().getNombreCompleto() + " entró en mora.");
            }
        }
        log.info("Tarea detectarMoras completada.");
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void reenviarAlertasDeMora() {
        List<Prestamo> prestamosEnMora = prestamoRepository.findByEstado(EstadoPrestamo.EN_MORA);
        for (Prestamo prestamo : prestamosEnMora) {
            crearYEnviar(
                    prestamo.getUsuarioSolicitante(),
                    TipoNotificacion.MORA,
                    "Recordatorio de mora",
                    "Tu préstamo ID " + prestamo.getId()
                            + " continúa en mora. Devuelve los equipos lo antes posible.");
            notificarUsuarios(
                    resolverDueniosPrestamo(prestamo),
                    TipoNotificacion.MORA,
                    "Recordatorio de mora sobre equipos a tu cargo",
                    "El préstamo ID " + prestamo.getId() + " sigue en mora y aún no ha sido cerrado.");
        }
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
                notificarUsuarios(
                        resolverDueniosPrestamo(prestamo),
                        TipoNotificacion.RECORDATORIO_VENCIMIENTO,
                        "Préstamo próximo a vencer sobre equipos a tu cargo",
                        "El préstamo ID " + prestamo.getId() + " vencerá el "
                                + prestamo.getFechaHoraDevolucionEstimada() + ".");
            }
        }
        log.info("Tarea enviarRecordatorios completada.");
    }

    public void notificarSolicitudPrestamoAmbiente(Usuario solicitante, Ambiente ambiente,
            LocalDateTime fechaReferencia) {
        String ambienteNombre = ambiente != null ? ambiente.getNombre() : "ambiente solicitado";
        crearYEnviar(
                solicitante,
                TipoNotificacion.SOLICITUD_PRESTAMO_AMBIENTE,
                "Solicitud de ambiente registrada",
                "Tu solicitud del ambiente " + ambienteNombre + " fue registrada y está pendiente de aprobación.");
        notificarGestoresAmbiente(
                ambiente,
                solicitante,
                TipoNotificacion.SOLICITUD_PRESTAMO_AMBIENTE,
                "Nueva solicitud de ambiente a tu cargo",
                "El usuario " + solicitante.getNombreCompleto() + " solicitó el ambiente " + ambienteNombre
                        + " para " + fechaReferencia + ".");
    }

    public void notificarPrestamoAmbienteAprobado(Usuario solicitante, Ambiente ambiente) {
        String ambienteNombre = ambiente != null ? ambiente.getNombre() : "ambiente solicitado";
        crearYEnviar(
                solicitante,
                TipoNotificacion.PRESTAMO_AMBIENTE_APROBADO,
                "Préstamo de ambiente aprobado",
                "Tu solicitud del ambiente " + ambienteNombre + " fue aprobada.");
        notificarGestoresAmbiente(
                ambiente,
                null,
                TipoNotificacion.PRESTAMO_AMBIENTE_APROBADO,
                "Préstamo de ambiente aprobado",
                "Se aprobó una solicitud sobre el ambiente " + ambienteNombre + ".");
    }

    public void notificarPrestamoAmbienteRechazado(Usuario solicitante, Ambiente ambiente,
            String motivo) {
        String ambienteNombre = ambiente != null ? ambiente.getNombre() : "ambiente solicitado";
        crearYEnviar(
                solicitante,
                TipoNotificacion.PRESTAMO_AMBIENTE_RECHAZADO,
                "Préstamo de ambiente rechazado",
                "Tu solicitud del ambiente " + ambienteNombre + " fue rechazada. Motivo: " + motivo);
        notificarGestoresAmbiente(
                ambiente,
                null,
                TipoNotificacion.PRESTAMO_AMBIENTE_RECHAZADO,
                "Préstamo de ambiente rechazado",
                "Se rechazó una solicitud sobre el ambiente " + ambienteNombre + ". Motivo: " + motivo);
    }

    public void notificarPrestamoAmbienteCancelado(Usuario solicitante, Ambiente ambiente) {
        String ambienteNombre = ambiente != null ? ambiente.getNombre() : "ambiente solicitado";
        crearYEnviar(
                solicitante,
                TipoNotificacion.PRESTAMO_AMBIENTE_CANCELADO,
                "Préstamo de ambiente cancelado",
                "La solicitud del ambiente " + ambienteNombre + " fue cancelada.");
        notificarGestoresAmbiente(
                ambiente,
                solicitante,
                TipoNotificacion.PRESTAMO_AMBIENTE_CANCELADO,
                "Solicitud de ambiente cancelada",
                "La solicitud del ambiente " + ambienteNombre + " fue cancelada por el solicitante.");
    }

    public void notificarPrestamoAmbienteDevuelto(Usuario solicitante, Ambiente ambiente) {
        String ambienteNombre = ambiente != null ? ambiente.getNombre() : "ambiente solicitado";
        crearYEnviar(
                solicitante,
                TipoNotificacion.PRESTAMO_AMBIENTE_DEVUELTO,
                "Préstamo de ambiente cerrado",
                "Se registró la devolución del ambiente " + ambienteNombre + ".");
        notificarGestoresAmbiente(
                ambiente,
                null,
                TipoNotificacion.PRESTAMO_AMBIENTE_DEVUELTO,
                "Ambiente devuelto",
                "Se registró la devolución del ambiente " + ambienteNombre + ".");
    }

    private void notificarGestoresAmbiente(Ambiente ambiente, Usuario excluir, TipoNotificacion tipo,
            String titulo, String mensaje) {
        List<Usuario> gestores = resolverGestoresAmbiente(ambiente);
        if (excluir != null) {
            gestores = gestores.stream()
                    .filter(usuario -> !usuario.getId().equals(excluir.getId()))
                    .toList();
        }
        notificarUsuarios(gestores, tipo, titulo, mensaje);
    }

    private List<Usuario> resolverGestoresAmbiente(Ambiente ambiente) {
        if (ambiente == null) {
            return List.of();
        }

        Set<Long> ids = new LinkedHashSet<>();
        List<Usuario> gestores = new ArrayList<>();

        if (ambiente.getPropietario() != null && ids.add(ambiente.getPropietario().getId())) {
            gestores.add(ambiente.getPropietario());
        }
        if (ambiente.getInstructorResponsable() != null && ids.add(ambiente.getInstructorResponsable().getId())) {
            gestores.add(ambiente.getInstructorResponsable());
        }
        if (ambiente.getEncargados() != null) {
            for (Usuario encargado : ambiente.getEncargados()) {
                if (encargado != null && ids.add(encargado.getId())) {
                    gestores.add(encargado);
                }
            }
        }

        return gestores;
    }

    public void enviarCorreoPrueba(String correoSolicitante, String destinatario) {
        Usuario solicitante = usuarioRepository.findByCorreoElectronico(correoSolicitante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoSolicitante));

        String correoDestino = destinatario != null && !destinatario.isBlank()
                ? destinatario.trim()
                : solicitante.getCorreoElectronico();

        if (correoDestino == null || correoDestino.isBlank()) {
            throw new OperacionNoPermitidaException("No hay un correo destino válido para la prueba SMTP.");
        }

        correoServicio.enviarCorreoPruebaObligatorio(correoDestino);
    }

    public void enviarSuiteCorreosUsuario(String correoSolicitante, String destinatario) {
        Usuario solicitante = usuarioRepository.findByCorreoElectronico(correoSolicitante)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + correoSolicitante));

        String correoDestino = destinatario != null && !destinatario.isBlank()
                ? destinatario.trim()
                : solicitante.getCorreoElectronico();

        if (correoDestino == null || correoDestino.isBlank()) {
            throw new OperacionNoPermitidaException("No hay un correo destino válido para la suite de pruebas.");
        }

        correoServicio.enviarCorreoHtmlObligatorio(
                correoDestino,
                "Código de verificación - SIGEA",
                "correos/correo-verificacion",
                java.util.Map.of(
                        "nombreUsuario", "Camilo",
                        "codigo", "482915",
                        "horasValidez", 24));

        enviarVistaPreviaNotificacion(correoDestino, "Camilo", TipoNotificacion.RESERVA_CREADA,
                "Reserva registrada - SIGEA",
                "Tu reserva del equipo Portátil Lenovo T14 fue registrada correctamente.",
                List.of(
                        "Equipo: Portátil Lenovo T14 (EQ-2026-014)",
                        "Cantidad: 1 unidad",
                        "Recogida: 10/04/2026 08:00 a. m.",
                        "Recuerda recogerlo dentro de las 2 horas siguientes."),
                "Puedes revisar el estado desde el módulo de reservas en SIGEA.");

        enviarVistaPreviaNotificacion(correoDestino, "Camilo", TipoNotificacion.EQUIPO_RECOGIDO,
                "Equipo recogido - préstamo activo",
                "La entrega del equipo fue registrada y el préstamo ya se encuentra activo.",
                List.of(
                        "Equipo: Portátil Lenovo T14",
                        "Fecha de entrega: 10/04/2026 08:20 a. m.",
                        "Fecha límite de devolución: 12/04/2026 04:00 p. m."),
                "Devuélvelo dentro del tiempo estimado para evitar entrar en mora.");

        enviarVistaPreviaNotificacion(correoDestino, "Camilo", TipoNotificacion.RECORDATORIO_VENCIMIENTO,
                "Recordatorio: tu préstamo está por vencer",
                "Tu préstamo está próximo a vencer y queremos ayudarte a evitar retrasos.",
                List.of(
                        "Préstamo: PR-2026-0084",
                        "Fecha de devolución estimada: 12/04/2026 04:00 p. m.",
                        "Equipo: Portátil Lenovo T14"),
                "Si necesitas apoyo, coordina la entrega con tiempo desde SIGEA.");

        enviarVistaPreviaNotificacion(correoDestino, "Camilo", TipoNotificacion.MORA,
                "Préstamo en mora",
                "El plazo de devolución ya fue superado y el sistema registró el préstamo en mora.",
                List.of(
                        "Préstamo: PR-2026-0084",
                        "Vencimiento: 12/04/2026 04:00 p. m.",
                        "Equipo: Portátil Lenovo T14"),
                "Devuelve el equipo lo antes posible para normalizar tu estado operativo.");

        correoServicio.enviarCorreoHtmlObligatorio(
                correoDestino,
                "Actualización de préstamo - SIGEA",
                "correos/correo-prestamo-estado",
                java.util.Map.of(
                        "nombreUsuario", "Camilo",
                        "estado", "APROBADO",
                        "equipos", "Portátil Lenovo T14, Mouse inalámbrico",
                        "fechaSolicitud", "10/04/2026 07:40 a. m.",
                        "fechaDevolucion", "12/04/2026 04:00 p. m.",
                        "observacion", "Preséntate con tu documento para la entrega."));

        correoServicio.enviarCorreoHtmlObligatorio(
                correoDestino,
                "Préstamo de ambiente - SIGEA",
                "correos/correo-prestamo-ambiente",
                java.util.Map.of(
                        "nombreUsuario", "Camilo",
                        "estado", "APROBADO",
                        "ambienteNombre", "Laboratorio de Redes 204",
                        "fechaInicio", "15/04/2026",
                        "fechaFin", "15/04/2026",
                        "horaInicio", "08:00 a. m.",
                        "horaFin", "12:00 m.",
                        "proposito", "Práctica de cableado estructurado",
                        "observacion", "Llega 15 minutos antes para apertura del ambiente."));

        enviarVistaPreviaNotificacion(correoDestino, "Camilo", TipoNotificacion.PRESTAMO_AMBIENTE_DEVUELTO,
                "Cierre de préstamo de ambiente",
                "La devolución del ambiente fue registrada satisfactoriamente.",
                List.of(
                        "Ambiente: Laboratorio de Redes 204",
                        "Fecha de cierre: 15/04/2026 12:05 m.",
                        "Estado reportado: Excelente"),
                "Gracias por mantener el espacio en buen estado para los siguientes usuarios.");

        correoServicio.enviarCorreoHtmlObligatorio(
                correoDestino,
                "⚠️ Equipo en mantenimiento correctivo: Portátil Lenovo T14",
                "correos/correo-alerta-estado-equipo",
                java.util.Map.of(
                        "nombreUsuario", "Camilo",
                        "equipoNombre", "Portátil Lenovo T14",
                        "codigoEquipo", "EQ-2026-014",
                        "placaEquipo", "PL-55231",
                        "promedioEstado", "3.20"));
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
        if (notificacion.getUsuarioDestino() == null
                || !notificacion.getUsuarioDestino().getCorreoElectronico().equals(correoUsuario)) {
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
        if (destinatario == null)
            return;
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
            enviado = correoServicio.enviarCorreoNotificacion(
                    destinatario.getCorreoElectronico(),
                    titulo,
                    construirVariablesNotificacion(destinatario, tipo, titulo, mensaje));
        }
        notif.setEstadoEnvio(enviado ? EstadoEnvio.ENVIADA : EstadoEnvio.FALLIDA);
        notif.setFechaEnvio(LocalDateTime.now());
        notificacionRepository.save(notif);
    }

    private void enviarVistaPreviaNotificacion(String correoDestino, String nombreUsuario, TipoNotificacion tipo,
            String titulo, String resumen, List<String> lineasDetalle, String notaFinal) {
        correoServicio.enviarCorreoHtmlObligatorio(
                correoDestino,
                titulo,
                "correos/correo-notificacion-usuario",
                java.util.Map.of(
                        "nombreUsuario", nombreUsuario,
                        "tituloCorreo", titulo,
                        "subtituloCorreo", resolverSubtitulo(tipo),
                        "descripcionCorreo", resolverDescripcion(tipo),
                        "estadoEtiqueta", resolverEtiqueta(tipo),
                        "estiloEstado", resolverEstilo(tipo),
                        "resumen", resumen,
                        "detalleTitulo", "Detalle del movimiento",
                        "lineasDetalle", lineasDetalle,
                        "notaFinal", notaFinal));
    }

    private java.util.Map<String, Object> construirVariablesNotificacion(Usuario destinatario, TipoNotificacion tipo,
            String titulo, String mensaje) {
        return java.util.Map.of(
                "nombreUsuario", destinatario.getNombreCompleto(),
                "tituloCorreo", titulo,
                "subtituloCorreo", resolverSubtitulo(tipo),
                "descripcionCorreo", resolverDescripcion(tipo),
                "estadoEtiqueta", resolverEtiqueta(tipo),
                "estiloEstado", resolverEstilo(tipo),
                "resumen", construirResumen(mensaje),
                "detalleTitulo", "Detalle del movimiento",
                "lineasDetalle", construirLineasDetalle(mensaje),
                "notaFinal", resolverNotaFinal(tipo));
    }

    private String construirResumen(String mensaje) {
        List<String> lineas = construirLineasDetalle(mensaje);
        return lineas.isEmpty() ? "Se registró una actualización en tu cuenta de SIGEA." : lineas.getFirst();
    }

    private List<String> construirLineasDetalle(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            return List.of();
        }
        String[] partes = mensaje.split("\\r?\\n");
        List<String> lineas = new ArrayList<>();
        for (String parte : partes) {
            String limpia = parte.trim();
            if (!limpia.isEmpty()) {
                lineas.add(limpia);
            }
        }
        if (lineas.size() <= 1) {
            return lineas;
        }
        return lineas.subList(1, lineas.size());
    }

    private String resolverSubtitulo(TipoNotificacion tipo) {
        return switch (tipo) {
            case RESERVA_CREADA, RESERVA_CANCELADA, RESERVA_EXPIRADA, EQUIPO_RECOGIDO -> "Reservas y entregas";
            case SOLICITUD_PRESTAMO, PRESTAMO_SALIDA, PRESTAMO_CANCELADO, PRESTAMO_DEVUELTO,
                    RECORDATORIO_VENCIMIENTO, MORA ->
                "Préstamos de equipos";
            case SOLICITUD_PRESTAMO_AMBIENTE, PRESTAMO_AMBIENTE_APROBADO, PRESTAMO_AMBIENTE_RECHAZADO,
                    PRESTAMO_AMBIENTE_CANCELADO, PRESTAMO_AMBIENTE_DEVUELTO ->
                "Préstamos de ambientes";
            case STOCK_BAJO -> "Alertas de inventario";
            default -> "Notificaciones SIGEA";
        };
    }

    private String resolverDescripcion(TipoNotificacion tipo) {
        return switch (tipo) {
            case RESERVA_CREADA -> "Tu reserva fue registrada y el sistema dejó trazabilidad del movimiento.";
            case RESERVA_CANCELADA -> "Se confirmó la cancelación de una reserva registrada en SIGEA.";
            case RESERVA_EXPIRADA -> "La reserva venció por no recogida dentro de la ventana permitida.";
            case EQUIPO_RECOGIDO, PRESTAMO_SALIDA -> "El préstamo fue entregado y ya se encuentra activo.";
            case PRESTAMO_DEVUELTO -> "La devolución fue registrada correctamente en el sistema.";
            case PRESTAMO_CANCELADO -> "La solicitud fue cancelada y el movimiento quedó registrado.";
            case RECORDATORIO_VENCIMIENTO -> "Recuerda la fecha de devolución para evitar entrar en mora.";
            case MORA -> "Tu préstamo requiere atención inmediata por vencimiento del plazo.";
            case SOLICITUD_PRESTAMO_AMBIENTE -> "La solicitud del ambiente fue radicada y quedó pendiente de revisión.";
            case PRESTAMO_AMBIENTE_APROBADO -> "El préstamo del ambiente fue aprobado para la fecha indicada.";
            case PRESTAMO_AMBIENTE_RECHAZADO -> "La solicitud del ambiente no fue aprobada.";
            case PRESTAMO_AMBIENTE_CANCELADO -> "El movimiento del ambiente fue cancelado en SIGEA.";
            case PRESTAMO_AMBIENTE_DEVUELTO -> "El cierre del uso del ambiente quedó registrado correctamente.";
            case STOCK_BAJO -> "El sistema detectó un nivel de inventario que requiere seguimiento.";
            default -> "Se registró una novedad en tu cuenta o en los activos bajo tu responsabilidad.";
        };
    }

    private String resolverEtiqueta(TipoNotificacion tipo) {
        return switch (tipo) {
            case RESERVA_CREADA, SOLICITUD_PRESTAMO, SOLICITUD_PRESTAMO_AMBIENTE -> "Registrado";
            case EQUIPO_RECOGIDO, PRESTAMO_SALIDA, PRESTAMO_DEVUELTO, PRESTAMO_AMBIENTE_APROBADO,
                    PRESTAMO_AMBIENTE_DEVUELTO ->
                "Confirmado";
            case RESERVA_CANCELADA, PRESTAMO_CANCELADO, PRESTAMO_AMBIENTE_CANCELADO -> "Cancelado";
            case MORA, RESERVA_EXPIRADA, PRESTAMO_AMBIENTE_RECHAZADO -> "Atención";
            case RECORDATORIO_VENCIMIENTO, STOCK_BAJO -> "Recordatorio";
            default -> "Actualización";
        };
    }

    private String resolverEstilo(TipoNotificacion tipo) {
        return switch (tipo) {
            case MORA, PRESTAMO_AMBIENTE_RECHAZADO -> "danger";
            case RESERVA_EXPIRADA, RECORDATORIO_VENCIMIENTO, STOCK_BAJO -> "warning";
            case EQUIPO_RECOGIDO, PRESTAMO_SALIDA, PRESTAMO_DEVUELTO, PRESTAMO_AMBIENTE_APROBADO,
                    PRESTAMO_AMBIENTE_DEVUELTO ->
                "success";
            default -> "info";
        };
    }

    private String resolverNotaFinal(TipoNotificacion tipo) {
        return switch (tipo) {
            case MORA -> "Regulariza el préstamo cuanto antes para evitar bloqueos o nuevas restricciones en SIGEA.";
            case RECORDATORIO_VENCIMIENTO ->
                "Programa la entrega con tiempo y evita afectar tu disponibilidad para futuros préstamos.";
            case RESERVA_CREADA -> "Recuerda presentarte en el horario definido para no perder la reserva.";
            case RESERVA_EXPIRADA -> "Si aún necesitas el equipo, registra una nueva reserva desde la plataforma.";
            case PRESTAMO_AMBIENTE_APROBADO ->
                "Verifica el horario aprobado y coordina el uso responsable del espacio.";
            default -> "Puedes revisar más detalle directamente desde SIGEA con tu sesión habitual.";
        };
    }

    private void notificarUsuarios(List<Usuario> destinatarios, TipoNotificacion tipo, String titulo, String mensaje) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }
        Set<Long> idsNotificados = new LinkedHashSet<>();
        for (Usuario destinatario : destinatarios) {
            if (destinatario == null || destinatario.getId() == null || !idsNotificados.add(destinatario.getId())) {
                continue;
            }
            crearYEnviar(destinatario, tipo, titulo, mensaje);
        }
    }

    private List<Usuario> resolverDueniosPrestamo(Prestamo prestamo) {
        return prestamo.getDetalles().stream()
                .map(detalle -> resolverDuenioEquipo(detalle.getEquipo()))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private Usuario resolverDuenioEquipo(Equipo equipo) {
        if (equipo == null) {
            return null;
        }
        if (equipo.getPropietario() != null) {
            return equipo.getPropietario();
        }
        if (equipo.getInventarioActualInstructor() != null) {
            return equipo.getInventarioActualInstructor();
        }
        if (equipo.getAmbiente() != null) {
            return equipo.getAmbiente().getInstructorResponsable();
        }
        return null;
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
