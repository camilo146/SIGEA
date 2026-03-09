package co.edu.sena.sigea.reserva.service;

// =============================================================================
// SERVICIO: ReservaServicio
// =============================================================================
// Contiene la lógica de negocio del módulo de reservas anticipadas.
// El controlador solo recibe la petición HTTP y delega aquí (SRP).
//
// CICLO DE VIDA:
//   crear() → ACTIVA
//   cancelar() → CANCELADA (solo antes de fechaHoraInicio, RF-RES-03)
//   expirarReservasNoRecogidas() @Scheduled → EXPIRADA (RF-RES-02: 2h sin recoger)
//
// REGLAS IMPLEMENTADAS:
//   RF-RES-01: Máximo 5 días hábiles de anticipación
//   RF-RES-02: Cancelación automática si no recoge en 2 horas (tarea programada)
//   RF-RES-03: Usuario puede cancelar antes de la hora de inicio
//   RF-RES-04: Disponibilidad = cantidadDisponible - reservas ACTIVAS solapadas
// =============================================================================

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.common.util.FechasUtil;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.reserva.dto.ReservaCrearDTO;
import co.edu.sena.sigea.reserva.dto.ReservaRespuestaDTO;
import co.edu.sena.sigea.reserva.entity.Reserva;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
@Transactional
public class ReservaServicio {

    private static final Logger log = LoggerFactory.getLogger(ReservaServicio.class);

    /** RF-RES-01: máximo días hábiles de anticipación para la fecha de inicio. */
    private static final int MAX_DIAS_HABILES_ANTICIPACION = 5;

    /** RF-RES-02: ventana en horas para recoger; después se expira. */
    private static final int HORAS_VENTANA_RECOGIDA = 2;

    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;

    public ReservaServicio(ReservaRepository reservaRepository,
                           UsuarioRepository usuarioRepository,
                           EquipoRepository equipoRepository) {
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
    }

    // =========================================================================
    // MÉTODO: crear
    // =========================================================================
    // Crea una reserva. Usuario viene del token (correo).
    // Valida: 5 días hábiles (RF-RES-01), disponibilidad en el periodo (RF-RES-04),
    // equipo activo. Fija fechaHoraFin = fechaHoraInicio + 2 horas (RF-RES-02).
    // =========================================================================
    public ReservaRespuestaDTO crear(ReservaCrearDTO dto, String correoUsuario) {

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoUsuario));

        Equipo equipo = equipoRepository.findById(dto.getEquipoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Equipo no encontrado con ID: " + dto.getEquipoId()));

        if (!Boolean.TRUE.equals(equipo.getActivo())) {
            throw new OperacionNoPermitidaException(
                    "El equipo '" + equipo.getNombre() + "' no está disponible para reserva.");
        }

        LocalDateTime inicio = dto.getFechaHoraInicio();
        LocalDateTime fin = inicio.plusHours(HORAS_VENTANA_RECOGIDA);

        // RF-RES-01: fecha de inicio no puede superar 5 días hábiles desde hoy.
        LocalDate hoy = LocalDate.now();
        LocalDate maxFechaInicio = FechasUtil.sumarDiasHabiles(hoy, MAX_DIAS_HABILES_ANTICIPACION);
        if (inicio.toLocalDate().isAfter(maxFechaInicio)) {
            throw new OperacionNoPermitidaException(
                    "La fecha de inicio no puede superar " + MAX_DIAS_HABILES_ANTICIPACION
                            + " días hábiles de anticipación. Fecha máxima permitida: " + maxFechaInicio);
        }

        if (inicio.isBefore(LocalDateTime.now())) {
            throw new OperacionNoPermitidaException(
                    "La fecha y hora de inicio debe ser en el futuro.");
        }

        // RF-RES-04: disponibilidad = cantidadDisponible - cantidad ya reservada en ese periodo.
        List<Reserva> solapadas = reservaRepository.findReservasSolapadas(
                equipo.getId(), EstadoReserva.ACTIVA, inicio, fin);
        int yaReservado = solapadas.stream().mapToInt(Reserva::getCantidad).sum();
        int disponibleEnPeriodo = equipo.getCantidadDisponible() - yaReservado;

        if (disponibleEnPeriodo < dto.getCantidad()) {
            throw new OperacionNoPermitidaException(
                    "No hay disponibilidad suficiente para '" + equipo.getNombre()
                            + "' en el periodo indicado. Disponible en ese horario: " + disponibleEnPeriodo);
        }

        Reserva reserva = Reserva.builder()
                .usuario(usuario)
                .equipo(equipo)
                .cantidad(dto.getCantidad())
                .fechaHoraInicio(inicio)
                .fechaHoraFin(fin)
                .estado(EstadoReserva.ACTIVA)
                .build();

        Reserva guardada = reservaRepository.save(reserva);
        return mapearReserva(guardada);
    }

    // =========================================================================
    // MÉTODO: cancelar
    // =========================================================================
    // RF-RES-03: el usuario puede cancelar solo si la reserva está ACTIVA
    // y la hora actual es anterior a fechaHoraInicio.
    // Solo el dueño de la reserva puede cancelarla.
    // =========================================================================
    public void cancelar(Long id, String correoUsuario) {

        Reserva reserva = buscarEntidadPorId(id);

        if (reserva.getEstado() != EstadoReserva.ACTIVA) {
            throw new OperacionNoPermitidaException(
                    "Solo se pueden cancelar reservas en estado ACTIVA. Estado actual: "
                            + reserva.getEstado());
        }

        if (LocalDateTime.now().isAfter(reserva.getFechaHoraInicio())
                || LocalDateTime.now().equals(reserva.getFechaHoraInicio())) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes cancelar la reserva antes de la hora de inicio ("
                            + reserva.getFechaHoraInicio() + ").");
        }

        if (!reserva.getUsuario().getCorreoElectronico().equals(correoUsuario)) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes cancelar tus propias reservas.");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
    }

    // =========================================================================
    // TAREA PROGRAMADA: expirar reservas no recogidas
    // =========================================================================
    // RF-RES-02: si no recogió en las 2 horas siguientes al inicio, la reserva
    // pasa a EXPIRADA. Se ejecuta cada 10 minutos.
    // =========================================================================
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void expirarReservasNoRecogidas() {
        List<Reserva> aExpirar = reservaRepository.findByEstadoAndFechaHoraFinBefore(
                EstadoReserva.ACTIVA, LocalDateTime.now());

        for (Reserva r : aExpirar) {
            r.setEstado(EstadoReserva.EXPIRADA);
            reservaRepository.save(r);
            log.info("Reserva ID {} expirada por no recogida en ventana de 2h.", r.getId());
        }
    }

    // =========================================================================
    // MÉTODOS DE CONSULTA
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ReservaRespuestaDTO> listarTodos() {
        return reservaRepository.findAll()
                .stream()
                .map(this::mapearReserva)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuestaDTO> listarPorEstado(EstadoReserva estado) {
        return reservaRepository.findByEstado(estado)
                .stream()
                .map(this::mapearReserva)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuestaDTO> listarPorUsuario(Long usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapearReserva)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaRespuestaDTO> listarMisReservas(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoUsuario));
        return listarPorUsuario(usuario.getId());
    }

    @Transactional(readOnly = true)
    public ReservaRespuestaDTO buscarPorId(Long id) {
        return mapearReserva(buscarEntidadPorId(id));
    }

    private Reserva buscarEntidadPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Reserva no encontrada con ID: " + id));
    }

    private ReservaRespuestaDTO mapearReserva(Reserva r) {
        return ReservaRespuestaDTO.builder()
                .id(r.getId())
                .usuarioId(r.getUsuario().getId())
                .nombreUsuario(r.getUsuario().getNombreCompleto())
                .correoUsuario(r.getUsuario().getCorreoElectronico())
                .equipoId(r.getEquipo().getId())
                .nombreEquipo(r.getEquipo().getNombre())
                .codigoEquipo(r.getEquipo().getCodigoUnico())
                .cantidad(r.getCantidad())
                .fechaHoraInicio(r.getFechaHoraInicio())
                .fechaHoraFin(r.getFechaHoraFin())
                .estado(r.getEstado())
                .fechaCreacion(r.getFechaCreacion())
                .build();
    }
}
