package co.edu.sena.sigea.seguridad.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.seguridad.dto.LoginDTO;
import co.edu.sena.sigea.seguridad.dto.LoginRespuestaDTO;
import co.edu.sena.sigea.seguridad.dto.RegistroDTO;
import co.edu.sena.sigea.seguridad.jwt.JwtProveedor;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service // Spring registra esta clase como un serviio

public class AutenticacionServicio {

    // Dependencias inyectadas por el constructor

    // "Director de seguridad"
    // Este se ecarga de verificar el correo la contraseña del ususario consultando
    // la BD
    private final AuthenticationManager authenticationManager;

    // este genera y valida en token JWT
    private final JwtProveedor jwtProveedor;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificacionEmailServicio verificacionEmailServicio;
    private final boolean requireEmailVerification;

    // constantes de configuracion ---

    // maximo de intentos fallidos
    private static final int MAX_INTENTOS_FALLIDOS = 3;

    // tiempo de bloqueo en minutos para el primer bloqueo
    private static final int MINUTOS_BLOQUEO_PRIMERO = 5;

    private static final int MINUTOS_BLOQUEO_SEGUNDO = 15;

    // constructor para inyectar las dependencias
    // Spring inyecta las dependecias automaticamente
    public AutenticacionServicio(AuthenticationManager authenticationManager,
            JwtProveedor jwtProveedor,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            VerificacionEmailServicio verificacionEmailServicio,
            @Value("${sigea.auth.require-email-verification:false}") boolean requireEmailVerification) {
        this.authenticationManager = authenticationManager;
        this.jwtProveedor = jwtProveedor;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificacionEmailServicio = verificacionEmailServicio;
        this.requireEmailVerification = requireEmailVerification;
    }

    // Metodo de login---
    // Flujo
    // Busca usuario por correo si no existe 404
    // Verifica si la cuenta esta activa si no esta activa 403
    // Verifica si la cuenta esta bloqueada por intentos fallidos si esta bloqueada
    // 403
    // Intenta autenticar con Spring Security:
    // si es exito resetear intentos fallidos y generar token JWT
    // si es fallo incrementar intentos fallidos y bloquear cuenta si se supera el
    // maximo de intentos

    // @Transactional: toda la operacion es automatica si falla algo se revierte

    @Transactional
    public LoginRespuestaDTO login(LoginDTO loginDTO) {

        // Buscar el usuario por correo
        // si no existe lanzar RecursoNoEncontradoException (404)
        Usuario usuario = usuarioRepository.findByCorreoElectronico(
                loginDTO.getCorreoElectronico())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con correo: " + loginDTO.getCorreoElectronico()));

        // Verificar si la cuenta esta activa

        if (!usuario.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "La cuenta esta desactivada, contacte al administrador");
        }

        // Verificar si la cuenta esta bloqueada

        if (usuario.getCuentaBloqueadaHasta() != null &&
                usuario.getCuentaBloqueadaHasta().isAfter(LocalDateTime.now())) {
            throw new OperacionNoPermitidaException(
                    "La cuenta esta bloqueada por intentos fallidos, intente de nuevo en: " +
                            usuario.getCuentaBloqueadaHasta().toString());

        }

        // Solo los no-administradores deben verificar el correo antes de iniciar sesión
        if (requireEmailVerification
                && usuario.getRol() != co.edu.sena.sigea.common.enums.Rol.ADMINISTRADOR
                && Boolean.FALSE.equals(usuario.getEmailVerificado())
                && usuario.getCorreoElectronico() != null
                && !usuario.getCorreoElectronico().isBlank()) {
            throw new OperacionNoPermitidaException(
                    "Debes verificar tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada e ingresa el código de 6 dígitos que te enviamos.");
        }

        // Verificar aprobación solo para usuarios finales (no aplica a administradores)
        if (usuario.getRol() != co.edu.sena.sigea.common.enums.Rol.ADMINISTRADOR
                && usuario.getEstadoAprobacion() == EstadoAprobacion.PENDIENTE) {
            throw new OperacionNoPermitidaException(
                    "Tu cuenta está pendiente de aprobación por un administrador. Espera a que revisen tu registro.");
        }

        // Intentar autenticar con Spring Security---

        try {
            // llama a UsuarioDetallesServicio.LoadUserByUsername (correo)
            // carga el usuario de la BD
            // Compara la contraseña enviada con el hash usando BCrypt
            // si coinciden retorna la validacion exitosa
            // si no coincide lanza BadCredentialsException

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDTO.getCorreoElectronico(),
                            loginDTO.getContrasena()));

            // si el login es exitoso entonces resetear intenod fallidos
            usuario.setIntentosFallidos(0);// resetear intentos fallidos
            usuario.setCuentaBloqueadaHasta(null);// quitar bloqueos
            usuarioRepository.save(usuario);// guardar cambios en la BD

            // Gnerar token JWT con la informacion del usuario
            String token = jwtProveedor.generarToken(
                    usuario.getCorreoElectronico(),
                    usuario.getRol().name()

            );

            // construir i retornar una respuesta

            return LoginRespuestaDTO.builder()
                    .token(token)
                    .tipo("Bearer")
                    .nombreCompleto(usuario.getNombreCompleto())
                    .rol(usuario.getRol().name())
                    .build();

        } catch (BadCredentialsException e) {
            // login fallido manejo de intentos fallidos
            manejarLoginFallido(usuario);
            throw new OperacionNoPermitidaException(
                    "Credenciales invalidas, intentos fallidos:" + usuario.getIntentosFallidos());
        }
    }

    // Metodo para manejar intentos fallidos

    /*
     * como funciona
     * este se llama cuando el usuario escribe la contraseña incorecta
     * Logica de bloqueo:
     * si no fue bloqueada antes bloquear 5 minutos BLOQUEO PRIMERO
     * si ya fue bloqueada antes bloquear 15 minutos BLOQUEO SEGUNDO
     * Resetara intentos fallidos a 0, ciclo nuevo tras el bloqueo
     * 
     * 
     */
    //
    private void manejarLoginFallido(Usuario usuario) {
        // incremeta los intetos fallidos
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);

        // si se supera el maximo permitido
        if (intentos >= MAX_INTENTOS_FALLIDOS) {
            // determiinar la duracion delbloqueo
            int minutosBloqueo = MINUTOS_BLOQUEO_PRIMERO;

            // si CuentaBloqueadaHasta no es null - ya fue bloqueado antes
            // segunda vez consecutiva bloqueo masd tiempo
            if (usuario.getCuentaBloqueadaHasta() != null) {
                minutosBloqueo = MINUTOS_BLOQUEO_SEGUNDO;
            }

            // establecer la fecha y la hora del bloqueo
            usuario.setCuentaBloqueadaHasta(
                    LocalDateTime.now().plusMinutes(minutosBloqueo));

            // resetear los intentos faliids nuevo ciclo despues del bloqueo
            usuario.setIntentosFallidos(0);
        }
        // guaradar los cambios en la BD
        usuarioRepository.save(usuario);

    }

    // metodo para registrar ---
    /*
     * funciona asi
     * logica:
     * verificar que el numero de documento no este duplicado
     * veriifiicar que el correo no este duplicado
     * encriptar la contraseña con BCrypt
     * crear el usuario con lo de USUARIO_ESTANDARD y guardar en la BD
     * 
     * El rol siempre sera de USUARIO_ESTANDARD para cambiar el rol un admin lo
     * tiene que hacer
     */

    @Transactional
    public void registrar(RegistroDTO registroDTO) {
        // verificar que el numero de documento no este duplicado
        // La BD tiene una restriccion de unicidad pero es mejor validar
        if (usuarioRepository.existsByNumeroDocumento(registroDTO.getNumeroDocumento())) {
            throw new OperacionNoPermitidaException(
                    "El numero de documento ya esta registrado: " + registroDTO.getNumeroDocumento());

        }
        // verificar correo duplicado
        if (registroDTO.getCorreoElectronico() != null
                && !registroDTO.getCorreoElectronico().isBlank()
                && usuarioRepository.existsByCorreoElectronico(
                        registroDTO.getCorreoElectronico())) {
            throw new OperacionNoPermitidaException(
                    "El correo ya esta registrado: " + registroDTO.getCorreoElectronico());
        }

        Usuario nuevoUsuario = Usuario.builder()
                .nombreCompleto(registroDTO.getNombre())
                .tipoDocumento(registroDTO.getTipoDocumento())
                .numeroDocumento(registroDTO.getNumeroDocumento())
                .correoElectronico(registroDTO.getCorreoElectronico())
                .programaFormacion(registroDTO.getProgramaFormacion())
                .telefono(registroDTO.getTelefono())
                .ficha(registroDTO.getNumeroFicha())
                .contrasenaHash(passwordEncoder.encode(registroDTO.getContrasena()))
                .rol(co.edu.sena.sigea.common.enums.Rol.USUARIO_ESTANDAR)
                .esSuperAdmin(false)
                .intentosFallidos(0)
                .activo(true)
                .estadoAprobacion(EstadoAprobacion.PENDIENTE)
                .emailVerificado(!requireEmailVerification)
                .build();

        if (requireEmailVerification) {
            String codigo = verificacionEmailServicio.generarCodigoVerificacion();
            nuevoUsuario.setTokenVerificacion(codigo);
            nuevoUsuario.setTokenVerificacionExpira(LocalDateTime.now().plusHours(24)); // código válido 24 h
        } else {
            nuevoUsuario.setTokenVerificacion(null);
            nuevoUsuario.setTokenVerificacionExpira(null);
        }

        usuarioRepository.save(nuevoUsuario);

        if (requireEmailVerification
                && nuevoUsuario.getCorreoElectronico() != null
                && !nuevoUsuario.getCorreoElectronico().isBlank()) {
            verificacionEmailServicio.enviarEmailVerificacion(nuevoUsuario);
        }

    }

}