package co.edu.sena.sigea.mantenimiento.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCerrarDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoCrearDTO;
import co.edu.sena.sigea.mantenimiento.dto.MantenimientoRespuestaDTO;
import co.edu.sena.sigea.mantenimiento.entity.Mantenimiento;
import co.edu.sena.sigea.mantenimiento.repository.MantenimientoRepository;

@ExtendWith(MockitoExtension.class)
class MantenimientoServicioTest {

    @Mock
    private MantenimientoRepository mantenimientoRepository;
    @Mock
    private EquipoRepository equipoRepository;

    @InjectMocks
    private MantenimientoServicio servicio;

    private Equipo equipo;
    private MantenimientoCrearDTO crearDTO;

    @BeforeEach
    void setUp() {
        equipo = new Equipo();
        equipo.setId(1L);
        equipo.setNombre("Osciloscopio");
        equipo.setCodigoUnico("EQ-OSC-01");
        equipo.setActivo(true);

        crearDTO = new MantenimientoCrearDTO();
        crearDTO.setEquipoId(1L);
        crearDTO.setTipo(TipoMantenimiento.PREVENTIVO);
        crearDTO.setDescripcion("Calibración anual");
        crearDTO.setFechaInicio(LocalDate.now());
        crearDTO.setResponsable("Técnico SENA");
        crearDTO.setObservaciones(null);
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("lanza cuando equipo no existe")
        void lanzaCuandoEquipoNoExiste() {
            when(equipoRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.crear(crearDTO))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Equipo no encontrado");
        }

        @Test
        @DisplayName("lanza cuando equipo no está activo")
        void lanzaCuandoEquipoInactivo() {
            equipo.setActivo(false);
            when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo));

            assertThatThrownBy(() -> servicio.crear(crearDTO))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("no está activo");
        }

        @Test
        @DisplayName("lanza cuando fecha fin es anterior a fecha inicio")
        void lanzaCuandoFechaFinAnterior() {
            crearDTO.setFechaFin(LocalDate.now().minusDays(1));
            when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo));

            assertThatThrownBy(() -> servicio.crear(crearDTO))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("fecha de fin no puede ser anterior");
        }

        @Test
        @DisplayName("guarda mantenimiento y retorna DTO cuando datos válidos")
        void guardaYRetornaDTO() {
            Mantenimiento guardado = Mantenimiento.builder()
                    .equipo(equipo)
                    .tipo(TipoMantenimiento.PREVENTIVO)
                    .descripcion(crearDTO.getDescripcion())
                    .fechaInicio(crearDTO.getFechaInicio())
                    .responsable(crearDTO.getResponsable())
                    .build();
            guardado.setId(1L);
            when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipo));
            when(mantenimientoRepository.save(any(Mantenimiento.class))).thenReturn(guardado);

            MantenimientoRespuestaDTO resultado = servicio.crear(crearDTO);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEquipoId()).isEqualTo(1L);
            assertThat(resultado.getNombreEquipo()).isEqualTo("Osciloscopio");
            assertThat(resultado.getTipo()).isEqualTo(TipoMantenimiento.PREVENTIVO);
            verify(mantenimientoRepository).save(any(Mantenimiento.class));
        }
    }

    @Nested
    @DisplayName("cerrar")
    class Cerrar {

        @Test
        @DisplayName("lanza cuando mantenimiento no existe")
        void lanzaCuandoNoExiste() {
            MantenimientoCerrarDTO cerrarDTO = new MantenimientoCerrarDTO(LocalDate.now(), null);
            when(mantenimientoRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.cerrar(999L, cerrarDTO))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Mantenimiento no encontrado");
        }

        @Test
        @DisplayName("lanza cuando ya está cerrado")
        void lanzaCuandoYaCerrado() {
            Mantenimiento m = Mantenimiento.builder()
                    .equipo(equipo)
                    .fechaInicio(LocalDate.now().minusDays(2))
                    .fechaFin(LocalDate.now().minusDays(1))
                    .build();
            m.setId(1L);
            MantenimientoCerrarDTO cerrarDTO = new MantenimientoCerrarDTO(LocalDate.now(), null);
            when(mantenimientoRepository.findById(1L)).thenReturn(Optional.of(m));

            assertThatThrownBy(() -> servicio.cerrar(1L, cerrarDTO))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("ya está cerrado");
        }

        @Test
        @DisplayName("actualiza fecha fin y retorna DTO cuando en curso")
        void cierraCorrectamente() {
            Mantenimiento m = Mantenimiento.builder()
                    .equipo(equipo)
                    .tipo(TipoMantenimiento.CORRECTIVO)
                    .descripcion("Reparación")
                    .fechaInicio(LocalDate.now().minusDays(2))
                    .fechaFin(null)
                    .responsable("Técnico")
                    .build();
            m.setId(1L);
            MantenimientoCerrarDTO cerrarDTO = new MantenimientoCerrarDTO(LocalDate.now(), "Completado OK");
            when(mantenimientoRepository.findById(1L)).thenReturn(Optional.of(m));
            when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(i -> i.getArgument(0));

            MantenimientoRespuestaDTO resultado = servicio.cerrar(1L, cerrarDTO);

            assertThat(resultado).isNotNull();
            assertThat(resultado.getFechaFin()).isEqualTo(cerrarDTO.getFechaFin());
            verify(mantenimientoRepository).save(any(Mantenimiento.class));
        }
    }

    @Test
    @DisplayName("listarTodos retorna lista vacía cuando no hay datos")
    void listarTodosVacio() {
        when(mantenimientoRepository.findAll()).thenReturn(List.of());

        List<MantenimientoRespuestaDTO> lista = servicio.listarTodos();

        assertThat(lista).isEmpty();
    }
}
