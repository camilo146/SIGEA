package co.edu.sena.sigea.prestamoambiente.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.ambiente.repository.AmbienteRepository;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.notificacion.service.NotificacionServicio;
import co.edu.sena.sigea.prestamoambiente.dto.PrestamoAmbienteRespuestaDTO;
import co.edu.sena.sigea.prestamoambiente.entity.PrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.TipoActividad;
import co.edu.sena.sigea.prestamoambiente.repository.PrestamoAmbienteRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PrestamoAmbienteServicioTest {

    @Mock
    private PrestamoAmbienteRepository prestamoAmbienteRepository;

    @Mock
    private AmbienteRepository ambienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionServicio notificacionServicio;

    private PrestamoAmbienteServicio servicio;

    @BeforeEach
    void setUp() {
        servicio = new PrestamoAmbienteServicio(
                prestamoAmbienteRepository,
                ambienteRepository,
                usuarioRepository,
                notificacionServicio);
    }

    @Test
    void debePermitirAUnEncargadoAprobarReservaDeAmbiente() {
        Usuario propietario = Usuario.builder()
                .nombreCompleto("Propietario Ambiente")
                .correoElectronico("propietario@sena.edu.co")
                .rol(Rol.INSTRUCTOR)
                .activo(true)
                .build();
        propietario.setId(1L);

        Usuario encargado = Usuario.builder()
                .nombreCompleto("Encargado Operativo")
                .correoElectronico("encargado@sena.edu.co")
                .rol(Rol.FUNCIONARIO)
                .activo(true)
                .build();
        encargado.setId(2L);

        Usuario solicitante = Usuario.builder()
                .nombreCompleto("Aprendiz Reserva")
                .correoElectronico("aprendiz@sena.edu.co")
                .rol(Rol.USUARIO_ESTANDAR)
                .activo(true)
                .build();
        solicitante.setId(3L);

        Ambiente ambiente = Ambiente.builder()
                .nombre("Laboratorio 204")
                .propietario(propietario)
                .instructorResponsable(propietario)
                .encargados(List.of(encargado))
                .activo(true)
                .build();
        ambiente.setId(20L);

        PrestamoAmbiente prestamo = PrestamoAmbiente.builder()
                .ambiente(ambiente)
                .solicitante(solicitante)
                .propietarioAmbiente(propietario)
                .fechaInicio(LocalDate.now().plusDays(1))
                .fechaFin(LocalDate.now().plusDays(1))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(10, 0))
                .proposito("Práctica guiada")
                .numeroParticipantes(15)
                .tipoActividad(TipoActividad.CLASE)
                .fechaSolicitud(LocalDateTime.now())
                .estado(EstadoPrestamoAmbiente.SOLICITADO)
                .build();
        prestamo.setId(50L);

        when(prestamoAmbienteRepository.findById(50L)).thenReturn(Optional.of(prestamo));
        when(usuarioRepository.findByCorreoElectronico("encargado@sena.edu.co")).thenReturn(Optional.of(encargado));
        when(prestamoAmbienteRepository.existeSolapamiento(any(), any(), any(), any(), any(), any())).thenReturn(false);
        when(prestamoAmbienteRepository.save(any(PrestamoAmbiente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PrestamoAmbienteRespuestaDTO respuesta = servicio.aprobar(50L, "encargado@sena.edu.co");

        assertEquals(EstadoPrestamoAmbiente.APROBADO.name(), respuesta.getEstado().name());
    }
}
