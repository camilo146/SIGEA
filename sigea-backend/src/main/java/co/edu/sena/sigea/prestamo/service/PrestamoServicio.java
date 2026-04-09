package co.edu.sena.sigea.prestamo.service;

// =============================================================================
// SERVICIO: PrestamoServicio
// =============================================================================
// Contiene TODA la lógica de negocio del módulo de préstamos.
// El Controller solo recibe la petición HTTP y delega aquí.
// Esta clase NO sabe nada de HTTP (sin @RequestMapping, sin HttpServletRequest).
//
// PRINCIPIO SRP (Single Responsibility):
//   Este servicio tiene UNA responsabilidad: gestionar el ciclo de vida
//   de los préstamos. No maneja usuarios, no maneja equipos directamente
//   (los repositorios hacen la persistencia).
//
// CICLO DE VIDA DE UN PRÉSTAMO:
//   solicitar() → aprobar()/rechazar() → registrarSalida() → registrarDevolucion()
//
//   SOLICITADO ──→ APROBADO ──→ ACTIVO ──→ DEVUELTO
//        │              │
//        └──→ RECHAZADO ┘
//
//   También: cancelar() cancela un préstamo SOLICITADO antes de que lo aprueben.
//
// REGLAS DE NEGOCIO IMPLEMENTADAS:
//   RN-01: Stock suficiente antes de solicitar (cantidadDisponible >= cantidad)
//   RN-02: Máximo 3 préstamos ACTIVOS simultáneos por usuario
//   RN-03: Documenta el estado del equipo al entregar (registrarSalida)
//   RN-04: Documenta el estado del equipo al devolver (registrarDevolucion)
//   RF-PRE-10: Descuenta stock al registrar salida, repone al devolver
// =============================================================================

import co.edu.sena.sigea.common.enums.EstadoCondicion;
import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.common.enums.TipoUsoEquipo;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.notificacion.service.NotificacionServicio;
import co.edu.sena.sigea.observacion.entity.ObservacionEquipo;
import co.edu.sena.sigea.observacion.repository.ObservacionEquipoRepository;
import co.edu.sena.sigea.prestamo.dto.DetallePrestamoDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoDevolucionDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoDevolucionDetalleDTO;
import co.edu.sena.sigea.prestamo.dto.DetallePrestamoRespuestaDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoCrearDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoRespuestaDTO;
import co.edu.sena.sigea.prestamo.entity.DetallePrestamo;
import co.edu.sena.sigea.prestamo.entity.Prestamo;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.reserva.entity.Reserva;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
// @Transactional a nivel de clase significa que TODOS los métodos públicos
// se ejecutan dentro de una transacción de BD.
// Si ocurre una excepción en plena ejecución, la transacción se revierte
// (rollback):
// no quedan datos a medias en la BD.
// Ej: si descuentas stock de 3 equipos y el 4to falla → los 3 anteriores se
// revierten.
@Transactional
public class PrestamoServicio {

    // =========================================================================
    // INYECCIÓN DE DEPENDENCIAS
    // =========================================================================
    // Constructor injection: la forma RECOMENDADA en Spring Boot.
    // Ventajas sobre @Autowired en campos:
    // 1. Fácil de hacer tests (puedes pasar mocks por constructor)
    // 2. Los campos son final → inmutables después de construcción
    // 3. Detecta ciclos de dependencia en tiempo de arranque, no en runtime
    // =========================================================================
    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final ReservaRepository reservaRepository;
    private final NotificacionServicio notificacionServicio;
    private final ObservacionEquipoRepository observacionEquipoRepository;

    public PrestamoServicio(PrestamoRepository prestamoRepository,
            UsuarioRepository usuarioRepository,
            EquipoRepository equipoRepository,
            ReservaRepository reservaRepository,
            NotificacionServicio notificacionServicio,
            ObservacionEquipoRepository observacionEquipoRepository) {
        this.prestamoRepository = prestamoRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.reservaRepository = reservaRepository;
        this.notificacionServicio = notificacionServicio;
        this.observacionEquipoRepository = observacionEquipoRepository;
    }

    // =========================================================================
    // MÉTODO: solicitar
    // =========================================================================
    // Crea una nueva solicitud de préstamo con estado SOLICITADO.
    // El stock NO se descuenta aquí (se descuenta cuando el admin registra la
    // salida).
    //
    // ¿Por qué no se descuenta aquí?
    // Porque la solicitud puede ser RECHAZADA. Si descontáramos el stock
    // y luego la rechazamos, habríamos bloqueado inventario innecesariamente.
    // Solo se descuenta cuando los equipos SALEN FÍSICAMENTE (registrarSalida).
    //
    // Parámetros:
    // dto → datos de la solicitud (fecha devolución + lista de equipos)
    // correoUsuario → correo del usuario autenticado (viene del token JWT)
    // =========================================================================
    public PrestamoRespuestaDTO solicitar(PrestamoCrearDTO dto, String correoUsuario) {

        // PASO 1: Buscar al usuario por su correo.
        // El correo viene del token JWT (no del body). Ver Controller.
        // Si no existe → 404. Si no está activo → no debería pasar porque
        // UsuarioDetallesServicio ya verifica el estado al hacer login.
        Usuario solicitante = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoUsuario));

        // PASO 2: Verificar límite de préstamos activos (RN-02).
        // "Máximo 3 préstamos ACTIVOS simultáneos por usuario"
        // ACTIVO = los equipos ya salieron y no han sido devueltos.
        // Nota: SOLICITADO y APROBADO no cuentan porque los equipos aún no salieron.
        long prestamosActivos = prestamoRepository
                .countByUsuarioSolicitanteIdAndEstado(solicitante.getId(), EstadoPrestamo.ACTIVO);

        if (prestamosActivos >= 3) {
            throw new OperacionNoPermitidaException(
                    "Ya tienes 3 préstamos activos. Debes devolver equipos antes de solicitar más.");
        }

        // PASO 3: Validar stock disponible para CADA equipo solicitado.
        // Recorremos la lista de detalles ANTES de crear el préstamo.
        // Si algún equipo no tiene stock suficiente → lanzamos excepción
        // INMEDIATAMENTE.
        // Así no creamos un préstamo a medias.
        for (DetallePrestamoDTO detalleDto : dto.getDetalles()) {

            // Buscar el equipo. Si no existe → 404.
            Equipo equipo = equipoRepository.findById(detalleDto.getEquipoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Equipo no encontrado con ID: " + detalleDto.getEquipoId()));

            // Verificar que el equipo esté activo (no dado de baja).
            // Boolean con B mayúscula → Lombok genera getActivo() (no isActivo()).
            if (!Boolean.TRUE.equals(equipo.getActivo())) {
                throw new OperacionNoPermitidaException(
                        "El equipo '" + equipo.getNombre() + "' no está disponible para préstamo.");
            }

            // Verificar stock (RN-01).
            // cantidadDisponible < cantidad solicitada → no hay suficientes unidades.
            if (equipo.getCantidadDisponible() < detalleDto.getCantidad()) {
                throw new OperacionNoPermitidaException(
                        "Stock insuficiente para '" + equipo.getNombre() + "'. " +
                                "Disponible: " + equipo.getCantidadDisponible() +
                                ", Solicitado: " + detalleDto.getCantidad());
            }
        }

        // PASO 4: Crear la entidad Prestamo (cabecera).
        // Usamos el Builder de Lombok (@Builder de la entidad).
        // Estado inicial = SOLICITADO (el admin debe aprobarlo).
        Prestamo prestamo = Prestamo.builder()
                .usuarioSolicitante(solicitante)
                .fechaHoraSolicitud(LocalDateTime.now())
                .fechaHoraDevolucionEstimada(dto.getFechaHoraDevolucionEstimada())
                .estado(EstadoPrestamo.SOLICITADO)
                .observacionesGenerales(dto.getObservacionesGenerales())
                .extensionesRealizadas(0)
                .build();

        // PASO 5: Crear los DetallePrestamo (líneas) y asociarlos al préstamo.
        // Por cada equipo en la solicitud, creamos un registro DetallePrestamo.
        for (DetallePrestamoDTO detalleDto : dto.getDetalles()) {
            Equipo equipo = equipoRepository.findById(detalleDto.getEquipoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Equipo no encontrado con ID: " + detalleDto.getEquipoId()));

            DetallePrestamo detalle = DetallePrestamo.builder()
                    .prestamo(prestamo)
                    .equipo(equipo)
                    .cantidad(detalleDto.getCantidad())
                    .devuelto(false)
                    // Estado al entregar/devolver = null hasta que el admin lo documente
                    .build();

            // Agregar el detalle a la lista del préstamo.
            // Como el Prestamo tiene cascade = ALL, al guardar el préstamo
            // también guarda automáticamente todos los detalles.
            prestamo.getDetalles().add(detalle);
        }

        // PASO 6: Guardar el préstamo (y sus detalles por cascada) en la BD.
        Prestamo guardado = prestamoRepository.save(prestamo);

        try {
            notificacionServicio.notificarSolicitudPrestamo(guardado);
        } catch (Exception e) {
            // No fallar la solicitud si la notificación falla
        }

        return mapearPrestamo(guardado);
    }

    // =========================================================================
    // MÉTODO: aprobar
    // =========================================================================
    // El administrador aprueba una solicitud SOLICITADA.
    // Estado: SOLICITADO → APROBADO
    // El stock sigue sin descontarse (aún no hay entrega física).
    // =========================================================================
    public PrestamoRespuestaDTO aprobar(Long id, String correoAdmin) {

        Prestamo prestamo = buscarEntidadPorId(id);

        // Solo se puede aprobar si está SOLICITADO.
        if (prestamo.getEstado() != EstadoPrestamo.SOLICITADO) {
            throw new OperacionNoPermitidaException(
                    "Solo se pueden aprobar préstamos en estado SOLICITADO. " +
                            "Estado actual: " + prestamo.getEstado());
        }

        Usuario admin = usuarioRepository.findByCorreoElectronico(correoAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoAdmin));

        prestamo.setAdministradorAprueba(admin);
        prestamo.setFechaHoraAprobacion(LocalDateTime.now());
        prestamo.setEstado(EstadoPrestamo.APROBADO);
        Prestamo guardado = prestamoRepository.save(prestamo);
        try {
            notificacionServicio.notificarAprobacion(guardado);
        } catch (Exception ignored) {
        }
        return mapearPrestamo(guardado);
    }

    // =========================================================================
    // MÉTODO: rechazar
    // =========================================================================
    // El administrador rechaza una solicitud SOLICITADA.
    // Estado: SOLICITADO → RECHAZADO
    // No hay impacto en stock porque nunca se descontó.
    // =========================================================================
    public PrestamoRespuestaDTO rechazar(Long id, String correoAdmin) {

        Prestamo prestamo = buscarEntidadPorId(id);

        if (prestamo.getEstado() != EstadoPrestamo.SOLICITADO) {
            throw new OperacionNoPermitidaException(
                    "Solo se pueden rechazar préstamos en estado SOLICITADO. " +
                            "Estado actual: " + prestamo.getEstado());
        }

        Usuario admin = usuarioRepository.findByCorreoElectronico(correoAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoAdmin));

        prestamo.setAdministradorAprueba(admin);
        prestamo.setFechaHoraAprobacion(LocalDateTime.now());
        prestamo.setEstado(EstadoPrestamo.RECHAZADO);
        Prestamo guardado = prestamoRepository.save(prestamo);
        try {
            notificacionServicio.notificarRechazo(guardado);
        } catch (Exception ignored) {
        }
        return mapearPrestamo(guardado);
    }

    /** Elimina un préstamo solo si está en estado SOLICITADO (aún no aprobado). */
    @Transactional
    public void eliminar(Long id) {
        Prestamo prestamo = buscarEntidadPorId(id);
        if (prestamo.getEstado() != EstadoPrestamo.SOLICITADO) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede eliminar una solicitud de préstamo en estado SOLICITADO. Estado actual: "
                            + prestamo.getEstado());
        }
        prestamoRepository.delete(prestamo);
    }

    // =========================================================================
    // MÉTODO: registrarSalida
    // =========================================================================
    // El administrador confirma que los equipos fueron entregados físicamente.
    // Estado: APROBADO → ACTIVO
    //
    // AQUÍ SÍ se descuenta el stock (RF-PRE-10):
    // equipo.cantidadDisponible -= detalle.cantidad
    //
    // También se documenta el estado de cada equipo al salir (RN-03).
    // =========================================================================
    public PrestamoRespuestaDTO registrarSalida(Long id, String correoAdmin) {

        Prestamo prestamo = buscarEntidadPorId(id);

        if (prestamo.getEstado() != EstadoPrestamo.APROBADO) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede registrar salida de préstamos APROBADOS. " +
                            "Estado actual: " + prestamo.getEstado());
        }

        Usuario admin = usuarioRepository.findByCorreoElectronico(correoAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoAdmin));

        // Descontar stock de CADA equipo en los detalles (RF-PRE-10).
        for (DetallePrestamo detalle : prestamo.getDetalles()) {
            Equipo equipo = detalle.getEquipo();

            // Documentar el estado del equipo al momento de la entrega (RN-03).
            // Por defecto se registra como BUENO. Solo el admin puede cambiarlo
            // si el equipo ya salía en otra condición.
            detalle.setEstadoEquipoEntrega(EstadoCondicion.BUENO);

            // Descontar del stock disponible.
            // Ej: cantidadDisponible=10, cantidad prestada=2 → cantidadDisponible=8.
            equipo.setCantidadDisponible(equipo.getCantidadDisponible() - detalle.getCantidad());

            if (equipo.getTipoUso() == TipoUsoEquipo.CONSUMIBLE) {
                detalle.setDevuelto(true);
                detalle.setEstadoEquipoDevolucion(EstadoCondicion.BUENO);
                detalle.setObservacionesDevolucion("Consumible entregado; no requiere devolución.");
            }

            // Guardar el equipo con el nuevo stock.
            // @Transactional garantiza que si algo falla, todo se revierte.
            equipoRepository.save(equipo);
        }

        prestamo.setFechaHoraSalida(LocalDateTime.now());

        boolean todosConsumibles = prestamo.getDetalles().stream()
                .allMatch(detalle -> detalle.getEquipo().getTipoUso() == TipoUsoEquipo.CONSUMIBLE);

        if (todosConsumibles) {
            prestamo.setAdministradorRecibe(admin);
            prestamo.setFechaHoraDevolucionReal(LocalDateTime.now());
            prestamo.setEstado(EstadoPrestamo.DEVUELTO);
            Reserva reserva = prestamo.getReserva();
            if (reserva != null) {
                reserva.setEstado(EstadoReserva.COMPLETADA);
                reservaRepository.save(reserva);
            }
        } else {
            prestamo.setEstado(EstadoPrestamo.ACTIVO);
        }

        Prestamo guardado = prestamoRepository.save(prestamo);
        try {
            notificacionServicio.notificarSalidaPrestamo(guardado);
        } catch (Exception ignored) {
        }
        return mapearPrestamo(guardado);
    }

    // =========================================================================
    // MÉTODO: registrarDevolucion
    // =========================================================================
    // El administrador confirma que TODOS los equipos fueron devueltos.
    // Estado: ACTIVO → DEVUELTO
    //
    // Acciones:
    // 1. Marca TODOS los detalles como devuelto = true
    // 2. Repone el stock de cada equipo (RF-PRE-10)
    // 3. Si todos los detalles están devueltos → estado = DEVUELTO
    // (con allMatch verifica que no quede ninguno pendiente)
    // =========================================================================
    public PrestamoRespuestaDTO registrarDevolucion(Long id, PrestamoDevolucionDTO dto, String correoAdmin) {

        Prestamo prestamo = buscarEntidadPorId(id);

        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO &&
                prestamo.getEstado() != EstadoPrestamo.EN_MORA) {
            throw new OperacionNoPermitidaException(
                    "Solo se puede registrar devolución de préstamos ACTIVOS o EN MORA. " +
                            "Estado actual: " + prestamo.getEstado());
        }

        Usuario admin = usuarioRepository.findByCorreoElectronico(correoAdmin)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoAdmin));

        List<DetallePrestamo> pendientes = prestamo.getDetalles().stream()
            .filter(detalle -> !Boolean.TRUE.equals(detalle.getDevuelto()))
            .filter(detalle -> detalle.getEquipo().getTipoUso() != TipoUsoEquipo.CONSUMIBLE)
            .toList();

        Map<Long, PrestamoDevolucionDetalleDTO> devolucionesPorDetalle = dto.getDetalles().stream()
            .collect(Collectors.toMap(
                PrestamoDevolucionDetalleDTO::getDetalleId,
                Function.identity(),
                (primero, segundo) -> {
                    throw new OperacionNoPermitidaException(
                        "No puedes registrar dos veces el mismo equipo en la devolución.");
                }));

        if (pendientes.size() != devolucionesPorDetalle.size()
            || pendientes.stream().anyMatch(detalle -> !devolucionesPorDetalle.containsKey(detalle.getId()))) {
            throw new OperacionNoPermitidaException(
                "Debes calificar y describir el estado de todos los equipos pendientes por devolver.");
        }

        // Reponer stock de CADA equipo y marcar como devuelto (RF-PRE-10).
        for (DetallePrestamo detalle : prestamo.getDetalles()) {

            // Solo procesar los que aún no fueron devueltos.
            // (Útil si en el futuro se permite devolución parcial)
            if (!detalle.getDevuelto()) {
                Equipo equipo = detalle.getEquipo();

                if (equipo.getTipoUso() == TipoUsoEquipo.CONSUMIBLE) {
                    detalle.setDevuelto(true);
                    detalle.setEstadoEquipoDevolucion(EstadoCondicion.BUENO);
                    detalle.setObservacionesDevolucion("Consumible entregado; no requiere devolución.");
                    continue;
                }

                PrestamoDevolucionDetalleDTO devolucionDetalle = devolucionesPorDetalle.get(detalle.getId());
                if (devolucionDetalle == null) {
                    throw new OperacionNoPermitidaException(
                            "Falta registrar el estado del equipo " + equipo.getNombre() + " en la devolución.");
                }

                detalle.setEstadoEquipoDevolucion(mapearEstadoCondicion(devolucionDetalle.getEstadoDevolucion()));
                detalle.setObservacionesDevolucion(devolucionDetalle.getObservacionesDevolucion().trim());

                // Reponer stock.
                // Ej: cantidadDisponible=8, cantidad devuelta=2 → cantidadDisponible=10.
                equipo.setCantidadDisponible(equipo.getCantidadDisponible() + detalle.getCantidad());
                equipo.setEstadoEquipoEscala(devolucionDetalle.getEstadoDevolucion());
                equipoRepository.save(equipo);

                observacionEquipoRepository.save(ObservacionEquipo.builder()
                        .prestamo(prestamo)
                        .equipo(equipo)
                        .usuarioDuenio(equipo.getPropietario() != null ? equipo.getPropietario() : admin)
                        .usuarioPrestatario(prestamo.getUsuarioSolicitante())
                        .observaciones(devolucionDetalle.getObservacionesDevolucion().trim())
                        .estadoDevolucion(devolucionDetalle.getEstadoDevolucion())
                        .fechaRegistro(LocalDateTime.now())
                        .build());

                // Marcar este equipo como devuelto.
                detalle.setDevuelto(true);
            }
        }

        // Verificar si TODOS los detalles fueron devueltos usando allMatch.
        // allMatch recorre la lista y retorna true SOLO si TODOS cumplen la condición.
        // Si la lista tiene [devuelto=true, devuelto=true, devuelto=true] → true →
        // DEVUELTO
        boolean todosDevueltos = prestamo.getDetalles().stream()
                .allMatch(DetallePrestamo::getDevuelto);

        if (todosDevueltos) {
            prestamo.setAdministradorRecibe(admin);
            prestamo.setFechaHoraDevolucionReal(LocalDateTime.now());
            prestamo.setEstado(EstadoPrestamo.DEVUELTO);
            // Si el préstamo vino de una reserva (equipo recogido), marcar reserva como
            // COMPLETADA
            Reserva reserva = prestamo.getReserva();
            if (reserva != null) {
                reserva.setEstado(EstadoReserva.COMPLETADA);
                reservaRepository.save(reserva);
            }
        }

        Prestamo guardado = prestamoRepository.save(prestamo);
        try {
            notificacionServicio.notificarPrestamoDevuelto(guardado);
        } catch (Exception ignored) {
        }
        return mapearPrestamo(guardado);
    }

    // =========================================================================
    // MÉTODO: cancelar
    // =========================================================================
    // El propio usuario cancela su solicitud antes de que sea aprobada.
    // Solo se puede cancelar cuando está SOLICITADO.
    // No hay impacto en stock.
    // =========================================================================
    public void cancelar(Long id, String correoUsuario) {

        Prestamo prestamo = buscarEntidadPorId(id);

        // Solo se puede cancelar si aún está SOLICITADO (sin respuesta del admin).
        if (prestamo.getEstado() != EstadoPrestamo.SOLICITADO) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes cancelar solicitudes en estado SOLICITADO. " +
                            "Estado actual: " + prestamo.getEstado());
        }

        // Verificar que sea el propio usuario quien cancele.
        // Comparamos el correo del solicitante con el correo del usuario autenticado.
        if (!prestamo.getUsuarioSolicitante().getCorreoElectronico().equals(correoUsuario)) {
            throw new OperacionNoPermitidaException(
                    "Solo puedes cancelar tus propias solicitudes.");
        }

        prestamo.setEstado(EstadoPrestamo.CANCELADO);
        Prestamo guardado = prestamoRepository.save(prestamo);
        try {
            notificacionServicio.notificarPrestamoCancelado(guardado);
        } catch (Exception ignored) {
        }
    }

    // =========================================================================
    // MÉTODOS DE CONSULTA (read-only)
    // =========================================================================
    // @Transactional(readOnly = true) le indica a Spring que esta transacción
    // solo LEE datos (no escribe). Ventajas:
    // - El motor de BD puede optimizar (ej: leer desde réplica de lectura)
    // - Spring no hace flush del contexto de persistencia al finalizar
    // - Mejor rendimiento en consultas grandes
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PrestamoRespuestaDTO> listarTodos() {
        return prestamoRepository.findAll()
                .stream()
                .map(this::mapearPrestamo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoRespuestaDTO> listarPorEstado(EstadoPrestamo estado) {
        return prestamoRepository.findByEstado(estado)
                .stream()
                .map(this::mapearPrestamo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoRespuestaDTO> listarPorUsuario(Long usuarioId) {
        return prestamoRepository.findByUsuarioSolicitanteId(usuarioId)
                .stream()
                .map(this::mapearPrestamo)
                .toList();
    }

    // Lista los préstamos del usuario autenticado (usa su correo para encontrarlo).
    @Transactional(readOnly = true)
    public List<PrestamoRespuestaDTO> listarMisPrestamos(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado: " + correoUsuario));

        return prestamoRepository.findByUsuarioSolicitanteId(usuario.getId())
                .stream()
                .map(this::mapearPrestamo)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrestamoRespuestaDTO buscarPorId(Long id) {
        return mapearPrestamo(buscarEntidadPorId(id));
    }

    // =========================================================================
    // MÉTODO PRIVADO: buscarEntidadPorId
    // =========================================================================
    // Reutilizable: busca el Prestamo y lanza 404 si no existe.
    // Esto evita repetir el mismo bloque orElseThrow en cada método.
    // Es un método interno del servicio, el Controller no lo llama directamente.
    // =========================================================================
    private Prestamo buscarEntidadPorId(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Préstamo no encontrado con ID: " + id));
    }

    // =========================================================================
    // MÉTODO PRIVADO: mapearPrestamo
    // =========================================================================
    // Convierte una entidad Prestamo → PrestamoRespuestaDTO.
    //
    // ¿Por qué es un método separado y no está inline en cada método?
    // PRINCIPIO DRY (Don't Repeat Yourself):
    // Todos los métodos retornan PrestamoRespuestaDTO.
    // Si cambiamos un campo, lo cambiamos en UN solo lugar.
    //
    // El operador ternario (condicion ? valorSi : valorNo) se usa para
    // campos que pueden ser null. Ejemplo:
    // administradorAprueba puede ser null si aún no aprobaron.
    // Si lo accedemos directamente sin verificar → NullPointerException.
    // Con el ternario: prestamo.getAdministradorAprueba() != null
    // ? prestamo.getAdministradorAprueba().getNombreCompleto()
    // : null → seguro
    // =========================================================================
    private PrestamoRespuestaDTO mapearPrestamo(Prestamo prestamo) {

        // Mapear la lista de detalles usando stream + map.
        // Cada DetallePrestamo de la entidad → DetallePrestamoRespuestaDTO.
        List<DetallePrestamoRespuestaDTO> detallesDTO = prestamo.getDetalles()
                .stream()
                .map(this::mapearDetalle)
                .toList();

        return PrestamoRespuestaDTO.builder()
                .id(prestamo.getId())
                .usuarioSolicitanteId(prestamo.getUsuarioSolicitante().getId())
                .nombreUsuarioSolicitante(
                        prestamo.getUsuarioSolicitante().getNombreCompleto())
                .correoUsuarioSolicitante(prestamo.getUsuarioSolicitante().getCorreoElectronico())
                // Operador ternario para campos nullable:
                .nombreAdministradorAprueba(
                        prestamo.getAdministradorAprueba() != null
                                ? prestamo.getAdministradorAprueba().getNombreCompleto()
                                : null)
                .nombreAdministradorRecibe(
                        prestamo.getAdministradorRecibe() != null
                                ? prestamo.getAdministradorRecibe().getNombreCompleto()
                                : null)
                .fechaHoraSolicitud(prestamo.getFechaHoraSolicitud())
                .fechaHoraAprobacion(prestamo.getFechaHoraAprobacion())
                .fechaHoraSalida(prestamo.getFechaHoraSalida())
                .fechaHoraDevolucionEstimada(prestamo.getFechaHoraDevolucionEstimada())
                .fechaHoraDevolucionReal(prestamo.getFechaHoraDevolucionReal())
                .estado(prestamo.getEstado())
                .observacionesGenerales(prestamo.getObservacionesGenerales())
                .extensionesRealizadas(prestamo.getExtensionesRealizadas())
                .detalles(detallesDTO)
                .build();
    }

    // =========================================================================
    // MÉTODO PRIVADO: mapearDetalle
    // =========================================================================
    // Convierte un DetallePrestamo (entidad) → DetallePrestamoRespuestaDTO.
    // También enriquece con el nombre y código del equipo.
    // =========================================================================
    private DetallePrestamoRespuestaDTO mapearDetalle(DetallePrestamo detalle) {
        return DetallePrestamoRespuestaDTO.builder()
                .id(detalle.getId())
                .equipoId(detalle.getEquipo().getId())
                .nombreEquipo(detalle.getEquipo().getNombre())
                .codigoEquipo(detalle.getEquipo().getCodigoUnico())
                .cantidad(detalle.getCantidad())
                .tipoUso(detalle.getEquipo().getTipoUso())
                .estadoEquipoEntrega(detalle.getEstadoEquipoEntrega())
                .observacionesEntrega(detalle.getObservacionesEntrega())
                .estadoEquipoDevolucion(detalle.getEstadoEquipoDevolucion())
                .observacionesDevolucion(detalle.getObservacionesDevolucion())
                .devuelto(detalle.getDevuelto())
                .build();
    }

    private EstadoCondicion mapearEstadoCondicion(Integer estadoDevolucion) {
        if (estadoDevolucion == null) {
            return EstadoCondicion.BUENO;
        }
        if (estadoDevolucion >= 9) {
            return EstadoCondicion.EXCELENTE;
        }
        if (estadoDevolucion >= 7) {
            return EstadoCondicion.BUENO;
        }
        if (estadoDevolucion >= 4) {
            return EstadoCondicion.REGULAR;
        }
        return EstadoCondicion.MALO;
    }

    /**
     * Pasa préstamos ACTIVOS a EN_MORA cuando la fecha de devolución ya pasó.
     * NotificacionServicio.detectarMoras (cada 15 min) hace lo mismo y además envía
     * notificación/email; esta tarea es respaldo por si el estado no se actualizó a
     * tiempo.
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void expirarPrestamosEnMora() {
        List<Prestamo> prestamosVencidos = prestamoRepository.findByFechaHoraDevolucionEstimadaBeforeAndEstado(
                LocalDateTime.now(), EstadoPrestamo.ACTIVO);
        for (Prestamo prestamo : prestamosVencidos) {
            prestamo.setEstado(EstadoPrestamo.EN_MORA);
            prestamoRepository.save(prestamo);
        }
    }
}
