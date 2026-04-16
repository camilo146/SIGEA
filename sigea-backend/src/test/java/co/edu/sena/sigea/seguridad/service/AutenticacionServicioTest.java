package co.edu.sena.sigea.seguridad.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
import co.edu.sena.sigea.seguridad.dto.LoginDTO;
import co.edu.sena.sigea.seguridad.dto.LoginRespuestaDTO;
import co.edu.sena.sigea.seguridad.jwt.JwtProveedor;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class AutenticacionServicioTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtProveedor jwtProveedor;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private VerificacionEmailServicio verificacionEmailServicio;
    @Mock
    private CorreoServicio correoServicio;

    private AutenticacionServicio servicio;

    @BeforeEach
    void setUp() {
        servicio = new AutenticacionServicio(
                authenticationManager,
                jwtProveedor,
                usuarioRepository,
                passwordEncoder,
                verificacionEmailServicio,
                correoServicio,
                true);
    }

    @Test
    void adminSinCorreoDebePoderIniciarSesion() {
        LoginDTO dto = new LoginDTO();
        dto.setNumeroDocumento("999999999");
        dto.setContrasena("password");

        Usuario usuario = Usuario.builder()
                .numeroDocumento("999999999")
                .nombreCompleto("Administrador SIGEA")
                .contrasenaHash("hash")
                .rol(Rol.ADMINISTRADOR)
                .activo(true)
                .estadoAprobacion(EstadoAprobacion.APROBADO)
                .emailVerificado(false)
                .correoElectronico(null)
                .intentosFallidos(0)
                .build();
        usuario.setId(1L);

        when(usuarioRepository.findByNumeroDocumento("999999999")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtProveedor.generarToken("999999999", "ADMINISTRADOR")).thenReturn("token-admin");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRespuestaDTO respuesta = assertDoesNotThrow(() -> servicio.login(dto));

        assertEquals("token-admin", respuesta.getToken());
        verify(jwtProveedor).generarToken("999999999", "ADMINISTRADOR");
    }

    @Test
    void alimentadorNoDebeExigirVerificacionDeCorreoParaEntrar() {
        LoginDTO dto = new LoginDTO();
        dto.setNumeroDocumento("123456789");
        dto.setContrasena("segura");

        Usuario usuario = Usuario.builder()
                .numeroDocumento("123456789")
                .nombreCompleto("Alimentador Operativo")
                .contrasenaHash("hash")
                .rol(Rol.ALIMENTADOR_EQUIPOS)
                .activo(true)
                .estadoAprobacion(EstadoAprobacion.APROBADO)
                .emailVerificado(false)
                .correoElectronico("operativo@sena.edu.co")
                .intentosFallidos(0)
                .build();
        usuario.setId(2L);

        when(usuarioRepository.findByNumeroDocumento("123456789")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("segura", "hash")).thenReturn(true);
        when(jwtProveedor.generarToken("operativo@sena.edu.co", "ALIMENTADOR_EQUIPOS")).thenReturn("token-operativo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoginRespuestaDTO respuesta = assertDoesNotThrow(() -> servicio.login(dto));

        assertEquals("token-operativo", respuesta.getToken());
    }
}
