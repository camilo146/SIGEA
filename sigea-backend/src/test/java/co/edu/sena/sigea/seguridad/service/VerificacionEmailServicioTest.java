package co.edu.sena.sigea.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
import co.edu.sena.sigea.notificacion.service.WhatsAppServicio;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class VerificacionEmailServicioTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CorreoServicio correoServicio;

    @Mock
    private WhatsAppServicio whatsAppServicio;

    private VerificacionEmailServicio servicio;

    @BeforeEach
    void setUp() {
        servicio = new VerificacionEmailServicio(usuarioRepository, correoServicio, whatsAppServicio);
    }

    @Test
    void debeRechazarCodigoIncorrecto() {
        Usuario usuario = Usuario.builder()
                .nombreCompleto("Aprendiz Prueba")
                .correoElectronico("aprendiz@sena.edu.co")
                .emailVerificado(false)
                .tokenVerificacion("482915")
                .tokenVerificacionExpira(LocalDateTime.now().plusHours(2))
                .build();

        when(usuarioRepository.findByCorreoElectronico("aprendiz@sena.edu.co"))
                .thenReturn(Optional.of(usuario));

        OperacionNoPermitidaException ex = assertThrows(
                OperacionNoPermitidaException.class,
                () -> servicio.verificarCodigo("aprendiz@sena.edu.co", "123456"));

        assertEquals("El código de verificación no es correcto.", ex.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void debeVerificarCorreoConCodigoCorrecto() {
        Usuario usuario = Usuario.builder()
                .nombreCompleto("Aprendiz Prueba")
                .correoElectronico("aprendiz@sena.edu.co")
                .emailVerificado(false)
                .tokenVerificacion("482915")
                .tokenVerificacionExpira(LocalDateTime.now().plusHours(2))
                .build();

        when(usuarioRepository.findByCorreoElectronico("aprendiz@sena.edu.co"))
                .thenReturn(Optional.of(usuario));

        String mensaje = servicio.verificarCodigo("aprendiz@sena.edu.co", "482915");

        assertEquals("Correo verificado correctamente. Ya puedes iniciar sesión.", mensaje);
        verify(usuarioRepository).save(usuario);
        assertEquals(Boolean.TRUE, usuario.getEmailVerificado());
    }
}
