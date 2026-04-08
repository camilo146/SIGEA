package co.edu.sena.sigea.evaluacion.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.mantenimiento.entity.Mantenimiento;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
import co.edu.sena.sigea.observacion.repository.ObservacionEquipoRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Servicio que ejecuta automáticamente el primer día de cada mes la evaluación
 * del estado de todos los equipos activos a partir del promedio de sus
 * observaciones de devolución del mes anterior.
 *
 * Si el promedio es menor a 4 (en escala 1-10), el equipo se pasa a estado
 * EN_MANTENIMIENTO con un mantenimiento CORRECTIVO, y se notifica a los
 * administradores del sistema y al propietario del equipo.
 */
@Service
@RequiredArgsConstructor
public class EvaluacionMensualServicio {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionMensualServicio.class);

    /** Umbral por debajo del cual se genera mantenimiento correctivo automático */
    private static final double UMBRAL_ESTADO_CRITICO = 4.0;

    private final EquipoRepository equipoRepository;
    private final ObservacionEquipoRepository observacionRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CorreoServicio correoServicio;

    /**
     * Se ejecuta el día 1 de cada mes a las 00:05 AM.
     * Evalúa el estado de todos los equipos activos del mes anterior.
     */
    @Scheduled(cron = "0 5 0 1 * *")
    @Transactional
    public void evaluarEstadoEquipos() {
        log.info("[EvaluacionMensual] Iniciando evaluación mensual de equipos...");

        LocalDateTime inicioMesAnterior = LocalDateTime.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        LocalDateTime finMesAnterior = LocalDateTime.now()
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0)
                .minusSeconds(1);

        List<Equipo> equiposActivos = equipoRepository.findByActivoTrue();
        int totalEvaluados = 0;
        int totalEnMantenimiento = 0;

        for (Equipo equipo : equiposActivos) {
            Double promedio = observacionRepository.promedioEstadoDevolucionEnPeriodo(
                    equipo.getId(), inicioMesAnterior, finMesAnterior);

            if (promedio == null) {
                // Sin observaciones en el mes anterior: sin cambios
                continue;
            }

            totalEvaluados++;
            equipo.setEstadoEquipoEscala((int) Math.round(promedio));
            equipoRepository.save(equipo);

            if (promedio < UMBRAL_ESTADO_CRITICO) {
                totalEnMantenimiento++;
                generarMantenimientoCorrectivo(equipo, promedio);
            }
        }

        log.info("[EvaluacionMensual] Completada. Equipos evaluados: {}, enviados a mantenimiento: {}",
                totalEvaluados, totalEnMantenimiento);
    }

    private void generarMantenimientoCorrectivo(Equipo equipo, double promedioEstado) {
        // Cambiar estado del equipo a EN_MANTENIMIENTO
        equipo.setEstado(EstadoEquipo.EN_MANTENIMIENTO);
        equipoRepository.save(equipo);

        // Crear registro de mantenimiento correctivo
        String descripcion = String.format(
                "Mantenimiento correctivo generado automáticamente. " +
                        "Promedio de estado en devoluciones del mes anterior: %.2f/10. " +
                        "El equipo requiere revisión técnica.",
                promedioEstado);

        Mantenimiento mantenimiento = Mantenimiento.builder()
                .equipo(equipo)
                .tipo(TipoMantenimiento.CORRECTIVO)
                .descripcion(descripcion)
                .fechaInicio(LocalDate.now())
                .responsable("Sistema SIGEA - Evaluación Automática")
                .observaciones("Generado por evaluación mensual automática de estado de equipos")
                .build();

        mantenimientoRepository.save(mantenimiento);

        // Enviar notificaciones con plantilla HTML
        String asunto = "⚠️ Equipo en mantenimiento correctivo: " + equipo.getNombre();
        Map<String, Object> vars = Map.of(
                "equipoNombre", equipo.getNombre(),
                "codigoEquipo", equipo.getCodigoUnico() != null ? equipo.getCodigoUnico() : "N/A",
                "placaEquipo", equipo.getPlaca() != null ? equipo.getPlaca() : "Sin placa",
                "promedioEstado", String.format("%.2f", promedioEstado));

        // Notificar a todos los administradores
        List<Usuario> admins = usuarioRepository.findByRolAndActivoTrue(Rol.ADMINISTRADOR);
        for (Usuario admin : admins) {
            Map<String, Object> varsAdmin = new java.util.HashMap<>(vars);
            varsAdmin.put("nombreUsuario", admin.getNombreCompleto());
            correoServicio.enviarCorreoHtml(admin.getCorreoElectronico(), asunto,
                    "correos/correo-alerta-estado-equipo", varsAdmin);
        }

        // Notificar al propietario del equipo si existe y no es admin
        Usuario propietario = equipo.getPropietario();
        if (propietario != null && propietario.getRol() != Rol.ADMINISTRADOR) {
            Map<String, Object> varsProp = new java.util.HashMap<>(vars);
            varsProp.put("nombreUsuario", propietario.getNombreCompleto());
            correoServicio.enviarCorreoHtml(propietario.getCorreoElectronico(), asunto,
                    "correos/correo-alerta-estado-equipo", varsProp);
        }

        log.warn("[EvaluacionMensual] Equipo '{}' (ID:{}) enviado a mantenimiento correctivo. Promedio: {}",
                equipo.getNombre(), equipo.getId(), String.format("%.2f", promedioEstado));
    }
}
