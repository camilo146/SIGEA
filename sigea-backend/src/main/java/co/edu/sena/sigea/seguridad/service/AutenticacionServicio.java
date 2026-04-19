package co.edu.sena.sigea.seguridad.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
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
    private final CorreoServicio correoServicio;
    private final boolean requireEmailVerification;

    // constantes de configuracion ---

    // maximo de intentos fallidos
    private static final int MAX_INTENTOS_FALLIDOS = 3;

    // tiempo de bloqueo en minutos para el primer bloqueo
    private static final int MINUTOS_BLOQUEO_PRIMERO = 5;

    private static final int MINUTOS_BLOQUEO_SEGUNDO = 15;

    private static final int MINUTOS_VALIDEZ_RECUPERACION = 30;

    // constructor para inyectar las dependencias
    // Spring inyecta las dependecias automaticamente
    public AutenticacionServicio(AuthenticationManager authenticationManager,
            JwtProveedor jwtProveedor,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            VerificacionEmailServicio verificacionEmailServicio,
            CorreoServicio correoServicio,
            @Value("${sigea.auth.require-email-verification:false}") boolean requireEmailVerification) {
        this.authenticationManager = authenticationManager;
        this.jwtProveedor = jwtProveedor;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.verificacionEmailServicio = verificacionEmailServicio;
        this.correoServicio = correoServicio;
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

        // Buscar el usuario por número de documento (CAMBIO 6)
        // Se responde "Credenciales inválidas" genérico para evitar enumeración
        Usuario usuario = usuarioRepository.findByNumeroDocumento(
                loginDTO.getNumeroDocumento())
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "Credenciales inválidas"));

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

        // Solo los usuarios finales deben verificar el correo antes de iniciar sesión
        if (debeVerificarCorreoEnLogin(usuario)) {
            throw new OperacionNoPermitidaException(
                    "Debes verificar tu correo electrónico antes de iniciar sesión. Revisa tu bandeja de entrada e ingresa el código de 6 dígitos que te enviamos.");
        }

        // La aprobación administrativa aplica solo a cuentas de usuario final
        if (requiereAprobacionAdministrativa(usuario)
                && usuario.getEstadoAprobacion() == EstadoAprobacion.PENDIENTE) {
            throw new OperacionNoPermitidaException(
                    "Tu cuenta está pendiente de aprobación por un administrador. Espera a que revisen tu registro.");
        }

        // Verificar contraseña directamente con BCrypt
        // (no usamos authenticationManager para desacoplar del email-based UserDetails)
        if (!passwordEncoder.matches(loginDTO.getContrasena(), usuario.getContrasenaHash())) {
            manejarLoginFallido(usuario);
            throw new OperacionNoPermitidaException(
                    "Credenciales invalidas, intentos fallidos:" + usuario.getIntentosFallidos());
        }

        // Login exitoso: resetear intentos fallidos
        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueadaHasta(null);
        usuarioRepository.save(usuario);

        // Generar token JWT usando un identificador estable del usuario
        String token = jwtProveedor.generarToken(
                resolverIdentificadorSesion(usuario),
                usuario.getRol().name());

        return LoginRespuestaDTO.builder()
                .id(usuario.getId())
                .token(token)
                .tipo("Bearer")
                .nombreCompleto(usuario.getNombreCompleto())
                .rol(usuario.getRol().name())
                .correoElectronico(usuario.getCorreoElectronico())
                .esSuperAdmin(Boolean.TRUE.equals(usuario.getEsSuperAdmin()))
                .build();
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
    private boolean debeVerificarCorreoEnLogin(Usuario usuario) {
        if (!requireEmailVerification || usuario == null) {
            return false;
        }
        return switch (usuario.getRol()) {
            case ADMINISTRADOR, INSTRUCTOR, ALIMENTADOR_EQUIPOS -> false;
            default -> Boolean.FALSE.equals(usuario.getEmailVerificado())
                    && usuario.getCorreoElectronico() != null
                    && !usuario.getCorreoElectronico().isBlank();
        };
    }

    private boolean requiereAprobacionAdministrativa(Usuario usuario) {
        if (usuario == null) {
            return false;
        }
        return switch (usuario.getRol()) {
            case ADMINISTRADOR, INSTRUCTOR, ALIMENTADOR_EQUIPOS -> false;
            default -> true;
        };
    }

    private String resolverIdentificadorSesion(Usuario usuario) {
        if (usuario.getCorreoElectronico() != null && !usuario.getCorreoElectronico().isBlank()) {
            return usuario.getCorreoElectronico().trim();
        }
        if (usuario.getNumeroDocumento() != null && !usuario.getNumeroDocumento().isBlank()) {
            return usuario.getNumeroDocumento().trim();
        }
        throw new OperacionNoPermitidaException(
                "La cuenta no tiene un identificador válido para iniciar sesión.");
    }

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

    @Transactional
    public String solicitarRecuperacionContrasena(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new OperacionNoPermitidaException("El correo es obligatorio para recuperar la contrasena.");
        }

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo.trim()).orElse(null);
        if (usuario == null || usuario.getCorreoElectronico() == null || usuario.getCorreoElectronico().isBlank()) {
            return "Si el correo existe en SIGEA, enviaremos un codigo de recuperacion.";
        }

        String codigo = verificacionEmailServicio.generarCodigoVerificacion();
        usuario.setTokenVerificacion(codigo);
        usuario.setTokenVerificacionExpira(LocalDateTime.now().plusMinutes(MINUTOS_VALIDEZ_RECUPERACION));
        usuarioRepository.save(usuario);

        correoServicio.enviarCorreoHtmlObligatorio(
                usuario.getCorreoElectronico(),
                "Código de recuperación de contraseña - SIGEA",
                "correos/correo-recuperacion-contrasena",
                Map.of(
                        "nombreUsuario", usuario.getNombreCompleto(),
                        "codigo", codigo,
                        "minutosValidez", MINUTOS_VALIDEZ_RECUPERACION));

        return "Si el correo existe en SIGEA, enviaremos un codigo de recuperacion.";
    }

    @Transactional
    public String restablecerContrasena(String correo, String codigo, String nuevaContrasena) {
        if (correo == null || correo.isBlank() || codigo == null || codigo.isBlank() || nuevaContrasena == null
                || nuevaContrasena.isBlank()) {
            throw new OperacionNoPermitidaException("Correo, codigo y nueva contrasena son obligatorios.");
        }

        validarContrasenaSegura(nuevaContrasena);

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo.trim())
                .orElseThrow(() -> new OperacionNoPermitidaException("El codigo de recuperacion no es valido."));

        String codigoLimpio = codigo.trim();
        if (usuario.getTokenVerificacion() == null || !usuario.getTokenVerificacion().equals(codigoLimpio)) {
            throw new OperacionNoPermitidaException("El codigo de recuperacion no es valido.");
        }

        if (usuario.getTokenVerificacionExpira() == null
                || usuario.getTokenVerificacionExpira().isBefore(LocalDateTime.now())) {
            throw new OperacionNoPermitidaException("El codigo de recuperacion expiro. Solicita uno nuevo.");
        }

        usuario.setContrasenaHash(passwordEncoder.encode(nuevaContrasena));
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpira(null);
        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueadaHasta(null);
        usuarioRepository.save(usuario);

        return "La contrasena fue restablecida correctamente. Ya puedes iniciar sesion.";
    }

    private void validarContrasenaSegura(String contrasena) {
        if (contrasena.length() < 8
                || !contrasena.chars().anyMatch(Character::isUpperCase)
                || !contrasena.chars().anyMatch(Character::isDigit)
                || contrasena.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new OperacionNoPermitidaException(
                    "La nueva contrasena debe tener minimo 8 caracteres, una mayuscula, un numero y un caracter especial.");
        }
    }

}