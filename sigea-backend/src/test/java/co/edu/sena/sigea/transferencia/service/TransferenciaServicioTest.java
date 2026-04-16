package co.edu.sena.sigea.transferencia.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.equipo.entity.Equipo;
import co.edu.sena.sigea.equipo.repository.EquipoRepository;
import co.edu.sena.sigea.transferencia.dto.TransferenciaCrearDTO;
import co.edu.sena.sigea.transferencia.dto.TransferenciaRespuestaDTO;
import co.edu.sena.sigea.transferencia.entity.Transferencia;
import co.edu.sena.sigea.transferencia.repository.TransferenciaRepository;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class TransferenciaServicioTest {

    @Mock
    private TransferenciaRepository transferenciaRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private AmbienteRepository ambienteRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private TransferenciaServicio servicio;

    private Usuario admin;
    private Usuario instructorOrigen;
    private Usuario instructorDestino;
    private Equipo equipo;
    private Ambiente ubicacionOrigen;
    private Ambiente ubicacionDestino;
    private TransferenciaCrearDTO dto;

    @BeforeEach
    void setUp() {
        admin = new Usuario();
        admin.setId(1L);
        admin.setNombreCompleto("Admin Test");
        admin.setCorreoElectronico("admin@test.com");
        admin.setRol(Rol.ADMINISTRADOR);

        instructorOrigen = new Usuario();
        instructorOrigen.setId(2L);
        instructorOrigen.setNombreCompleto("Instructor Origen");
        instructorOrigen.setRol(Rol.INSTRUCTOR);

        instructorDestino = new Usuario();
        instructorDestino.setId(3L);
        instructorDestino.setNombreCompleto("Instructor Destino");
        instructorDestino.setRol(Rol.INSTRUCTOR);

        equipo = new Equipo();
        equipo.setId(10L);
        equipo.setNombre("Multímetro");
        equipo.setCodigoUnico("EQ-001");
        equipo.setActivo(true);
        equipo.setCantidadTotal(5);
        equipo.setCantidadDisponible(5);
        equipo.setPropietario(instructorOrigen);
        equipo.setInventarioActualInstructor(instructorOrigen);

        ubicacionOrigen = new Ambiente();
        ubicacionOrigen.setId(100L);
        ubicacionOrigen.setNombre("Lab 1");
        equipo.setAmbiente(ubicacionOrigen);

        ubicacionDestino = new Ambiente();
        ubicacionDestino.setId(101L);
        ubicacionDestino.setNombre("Lab 2");

        dto = new TransferenciaCrearDTO();
        dto.setEquipoId(10L);
        dto.setInstructorDestinoId(3L);
        dto.setUbicacionDestinoId(101L);
        dto.setCantidad(1);
        dto.setMotivo("Reubicación");
        dto.setFechaTransferencia(LocalDateTime.now());
    }

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        @DisplayName("lanza cuando origen y destino son iguales")
        void lanzaCuandoOrigenYDestinoIguales() {
            dto.setInstructorDestinoId(2L);

            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.of(admin));
            when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(instructorOrigen));

            assertThatThrownBy(() -> servicio.crear(dto, "admin@test.com"))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("origen y destino no pueden ser el mismo");
        }

        @Test
        @DisplayName("lanza cuando usuario admin no existe")
        void lanzaCuandoAdminNoExiste() {
            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.crear(dto, "admin@test.com"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }

        @Test
        @DisplayName("lanza cuando equipo no existe")
        void lanzaCuandoEquipoNoExiste() {
            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.of(admin));
            when(equipoRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.crear(dto, "admin@test.com"))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Equipo no encontrado");
        }

        @Test
        @DisplayName("lanza cuando equipo no está activo")
        void lanzaCuandoEquipoInactivo() {
            equipo.setActivo(false);
            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.of(admin));
            when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(instructorDestino));
            when(ambienteRepository.findById(101L)).thenReturn(Optional.of(ubicacionDestino));

            assertThatThrownBy(() -> servicio.crear(dto, "admin@test.com"))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("no está activo");
        }

        @Test
        @DisplayName("lanza cuando cantidad supera disponible")
        void lanzaCuandoCantidadSuperaDisponible() {
            dto.setCantidad(10);
            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.of(admin));
            when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(instructorDestino));
            when(ambienteRepository.findById(101L)).thenReturn(Optional.of(ubicacionDestino));

            assertThatThrownBy(() -> servicio.crear(dto, "admin@test.com"))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessageContaining("Cantidad solicitada");
        }

        @Test
        @DisplayName("guarda transferencia y retorna DTO cuando datos válidos")
        void guardaYRetornaDTO() {
            Transferencia guardada = Transferencia.builder()
                    .equipo(equipo)
                    .inventarioOrigenInstructor(instructorOrigen)
                    .inventarioDestinoInstructor(instructorDestino)
                    .propietarioEquipo(instructorOrigen)
                    .ubicacionDestino(ubicacionDestino)
                    .cantidad(1)
                    .administradorAutoriza(admin)
                    .motivo(dto.getMotivo())
                    .fechaTransferencia(dto.getFechaTransferencia())
                    .build();
            guardada.setId(1L);
            when(usuarioRepository.findByIdentificador("admin@test.com")).thenReturn(Optional.of(admin));
            when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(instructorDestino));
            when(ambienteRepository.findById(101L)).thenReturn(Optional.of(ubicacionDestino));
            when(transferenciaRepository.save(any(Transferencia.class))).thenReturn(guardada);

            TransferenciaRespuestaDTO resultado = servicio.crear(dto, "admin@test.com");

            assertThat(resultado).isNotNull();
            assertThat(resultado.getEquipoId()).isEqualTo(10L);
            assertThat(resultado.getNombreEquipo()).isEqualTo("Multímetro");
            assertThat(resultado.getNombreInventarioOrigenInstructor()).isEqualTo("Instructor Origen");
            assertThat(resultado.getNombreInventarioDestinoInstructor()).isEqualTo("Instructor Destino");
            assertThat(resultado.getNombreUbicacionDestino()).isEqualTo("Lab 2");
            verify(transferenciaRepository).save(any(Transferencia.class));
        }
    }

    @Nested
    @DisplayName("listar y buscar")
    class ListarYBuscar {

        @Test
        @DisplayName("listarTodos retorna lista vacía cuando no hay datos")
        void listarTodosVacio() {
            when(transferenciaRepository.findAll()).thenReturn(List.of());

            List<TransferenciaRespuestaDTO> lista = servicio.listarTodos();

            assertThat(lista).isEmpty();
        }

        @Test
        @DisplayName("buscarPorId lanza cuando no existe")
        void buscarPorIdNoExiste() {
            when(transferenciaRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> servicio.buscarPorId(999L))
                    .isInstanceOf(RecursoNoEncontradoException.class)
                    .hasMessageContaining("Transferencia no encontrada");
        }
    }
}
