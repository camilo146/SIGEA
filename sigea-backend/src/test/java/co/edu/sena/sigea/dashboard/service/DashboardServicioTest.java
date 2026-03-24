package co.edu.sena.sigea.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.categoria.repository.CategoriaRepository;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;
import co.edu.sena.sigea.prestamo.repository.PrestamoRepository;
import co.edu.sena.sigea.reserva.repository.ReservaRepository;
import co.edu.sena.sigea.transferencia.repository.TransferenciaRepository;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class DashboardServicioTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private AmbienteRepository ambienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PrestamoRepository prestamoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private MantenimientoRepository mantenimientoRepository;
    @Mock
    private TransferenciaRepository transferenciaRepository;

    @InjectMocks
    private DashboardServicio servicio;

    @Test
    @DisplayName("obtenerEstadisticas retorna DTO con conteos desde repositorios")
    void obtenerEstadisticasRetornaConteos() {
        when(equipoRepository.count()).thenReturn(50L);
        when(equipoRepository.countByActivoTrue()).thenReturn(48L);
        when(equipoRepository.countEquiposConStockBajo()).thenReturn(3L);
        when(categoriaRepository.count()).thenReturn(5L);
        when(ambienteRepository.count()).thenReturn(8L);
        when(usuarioRepository.count()).thenReturn(120L);
        when(prestamoRepository.countByEstado(EstadoPrestamo.SOLICITADO)).thenReturn(2L);
        when(prestamoRepository.countByEstado(EstadoPrestamo.ACTIVO)).thenReturn(10L);
        when(prestamoRepository.countByEstado(EstadoPrestamo.EN_MORA)).thenReturn(1L);
        when(prestamoRepository.countByEstado(EstadoPrestamo.DEVUELTO)).thenReturn(100L);
        when(reservaRepository.countByEstado(EstadoReserva.ACTIVA)).thenReturn(4L);
        when(mantenimientoRepository.countByFechaFinIsNull()).thenReturn(2L);
        when(transferenciaRepository.count()).thenReturn(15L);

        var dto = servicio.obtenerEstadisticas();

        assertThat(dto).isNotNull();
        assertThat(dto.getTotalEquipos()).isEqualTo(50);
        assertThat(dto.getEquiposActivos()).isEqualTo(48);
        assertThat(dto.getEquiposStockBajo()).isEqualTo(3);
        assertThat(dto.getTotalCategorias()).isEqualTo(5);
        assertThat(dto.getTotalAmbientes()).isEqualTo(8);
        assertThat(dto.getTotalUsuarios()).isEqualTo(120);
        assertThat(dto.getPrestamosSolicitados()).isEqualTo(2);
        assertThat(dto.getPrestamosActivos()).isEqualTo(10);
        assertThat(dto.getPrestamosEnMora()).isEqualTo(1);
        assertThat(dto.getPrestamosDevueltos()).isEqualTo(100);
        assertThat(dto.getReservasActivas()).isEqualTo(4);
        assertThat(dto.getMantenimientosEnCurso()).isEqualTo(2);
        assertThat(dto.getTotalTransferencias()).isEqualTo(15);
    }
}
