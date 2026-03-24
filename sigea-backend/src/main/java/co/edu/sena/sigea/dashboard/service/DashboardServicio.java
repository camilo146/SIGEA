package co.edu.sena.sigea.dashboard.service;

// =============================================================================
// SERVICIO: DashboardServicio
// =============================================================================
// Agrega estadísticas para el panel de control (dashboard).
// Solo lectura; utiliza los repositorios existentes para contar registros.
// =============================================================================

import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.dashboard.dto.DashboardEstadisticasDTO;
import co.edu.sena.sigea.dashboard.dto.EquiposPorCategoriaDTO;
import co.edu.sena.sigea.dashboard.dto.PrestamosPorMesDTO;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.categoria.repository.CategoriaRepository;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.transferencia.repository.TransferenciaRepository;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service
public class DashboardServicio {

    private final EquipoRepository equipoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AmbienteRepository ambienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;
    private final MantenimientoRepository mantenimientoRepository;
    private final TransferenciaRepository transferenciaRepository;

    public DashboardServicio(EquipoRepository equipoRepository,
                             CategoriaRepository categoriaRepository,
                             AmbienteRepository ambienteRepository,
                             UsuarioRepository usuarioRepository,
                             PrestamoRepository prestamoRepository,
                             ReservaRepository reservaRepository,
                             MantenimientoRepository mantenimientoRepository,
                             TransferenciaRepository transferenciaRepository) {
        this.equipoRepository = equipoRepository;
        this.categoriaRepository = categoriaRepository;
        this.ambienteRepository = ambienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.prestamoRepository = prestamoRepository;
        this.reservaRepository = reservaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
        this.transferenciaRepository = transferenciaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardEstadisticasDTO obtenerEstadisticas() {

        return DashboardEstadisticasDTO.builder()
                .totalEquipos(equipoRepository.count())
                .equiposActivos(equipoRepository.countByActivoTrue())
                .totalCategorias(categoriaRepository.count())
                .totalAmbientes(ambienteRepository.count())
                .totalUsuarios(usuarioRepository.count())
                .prestamosSolicitados(prestamoRepository.countByEstado(EstadoPrestamo.SOLICITADO))
                .prestamosActivos(prestamoRepository.countByEstado(EstadoPrestamo.ACTIVO))
                .prestamosEnMora(prestamoRepository.countByEstado(EstadoPrestamo.EN_MORA))
                .prestamosDevueltos(prestamoRepository.countByEstado(EstadoPrestamo.DEVUELTO))
                .reservasActivas(reservaRepository.countByEstado(EstadoReserva.ACTIVA))
                .mantenimientosEnCurso(mantenimientoRepository.countByFechaFinIsNull())
                .totalTransferencias(transferenciaRepository.count())
                .equiposStockBajo(equipoRepository.countEquiposConStockBajo())
                .build();
    }

    /** Últimos 6 meses con cantidad de préstamos solicitados por mes. */
    @Transactional(readOnly = true)
    public List<PrestamosPorMesDTO> prestamosPorMes() {
        LocalDateTime hace6Meses = LocalDateTime.now().minusMonths(6);
        List<co.edu.sena.sigea.prestamo.entity.Prestamo> prestamos =
                prestamoRepository.findByFechaHoraSolicitudBetween(hace6Meses, LocalDateTime.now());
        List<PrestamosPorMesDTO> resultado = new ArrayList<>();
        Year year = Year.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime inicio = LocalDateTime.now().minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime fin = inicio.plusMonths(1);
            long count = prestamos.stream()
                    .filter(p -> p.getFechaHoraSolicitud() != null
                            && !p.getFechaHoraSolicitud().isBefore(inicio)
                            && p.getFechaHoraSolicitud().isBefore(fin))
                    .count();
            Month month = inicio.getMonth();
            String mesLabel = month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es")) + " " + inicio.getYear();
            resultado.add(new PrestamosPorMesDTO(mesLabel, count));
        }
        return resultado;
    }

    /** Cantidad de equipos activos por categoría (para gráfico de distribución). */
    @Transactional(readOnly = true)
    public List<EquiposPorCategoriaDTO> equiposPorCategoria() {
        List<EquiposPorCategoriaDTO> resultado = new ArrayList<>();
        for (co.edu.sena.sigea.categoria.entity.Categoria cat : categoriaRepository.findAll()) {
            long cantidad = equipoRepository.countByCategoriaId(cat.getId());
            if (cantidad > 0) {
                resultado.add(new EquiposPorCategoriaDTO(cat.getNombre(), cantidad));
            }
        }
        return resultado;
    }
}
