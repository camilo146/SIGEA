package co.edu.sena.sigea.seguridad.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.notificacion.service.CorreoServicio;
import co.edu.sena.sigea.notificacion.service.WhatsAppServicio;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

/**
 * Servicio para verificación de email en registro: genera un código de 6
 * dígitos,
 * lo envía por correo y valida el código cuando el usuario lo ingresa en la
 * app.
 * (No usa enlaces porque en desarrollo/proyecto no hay URL pública.)
 */
@Service
public class VerificacionEmailServicio {

    private static final int HORAS_VALIDEZ_CODIGO = 24;
    private static final int DIGITOS_CODIGO = 6;

    private final UsuarioRepository usuarioRepository;
    private final CorreoServicio correoServicio;
    private final WhatsAppServicio whatsAppServicio;

    public VerificacionEmailServicio(UsuarioRepository usuarioRepository,
            CorreoServicio correoServicio,
            WhatsAppServicio whatsAppServicio) {
        this.usuarioRepository = usuarioRepository;
        this.correoServicio = correoServicio;
        this.whatsAppServicio = whatsAppServicio;
    }

    /** Genera un código numérico de 6 dígitos para verificación por correo. */
    public String generarCodigoVerificacion() {
        int min = (int) Math.pow(10, DIGITOS_CODIGO - 1);
        int max = (int) Math.pow(10, DIGITOS_CODIGO) - 1;
        return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
    }

    /**
     * Envía el correo con el código de verificación (no enlace).
     * El usuario ya debe estar guardado con tokenVerificacion (código) y
     * tokenVerificacionExpira asignados.
     */
    public void enviarEmailVerificacion(Usuario usuario) {
        String codigo = usuario.getTokenVerificacion();

        if (usuario.getTelefono() != null && !usuario.getTelefono().isBlank() && whatsAppServicio.estaHabilitado()) {
            whatsAppServicio.enviarCodigoVerificacionObligatorio(usuario, codigo);
            return;
        }

        if (usuario.getCorreoElectronico() == null || usuario.getCorreoElectronico().isBlank()) {
            throw new OperacionNoPermitidaException(
                    "No se puede enviar el codigo: el usuario no tiene correo y WhatsApp no esta habilitado.");
        }

        String asunto = "Código de verificación - SIGEA";
        correoServicio.enviarCorreoHtmlObligatorio(
                usuario.getCorreoElectronico(),
                asunto,
                "correos/correo-verificacion",
                Map.of(
                        "nombreUsuario", usuario.getNombreCompleto(),
                        "codigo", codigo,
                        "horasValidez", HORAS_VALIDEZ_CODIGO));
    }

    /**
     * Verifica el código enviado al correo y marca el correo como verificado.
     * 
     * @param correo correo del usuario
     * @param codigo código de 6 dígitos recibido por email
     * @return mensaje de éxito
     */
    @Transactional
    public String verificarCodigo(String correo, String codigo) {
        if (correo == null || correo.isBlank() || codigo == null || codigo.isBlank()) {
            throw new OperacionNoPermitidaException("Correo y código son obligatorios.");
        }
        String codigoLimpio = codigo.trim();
        if (codigoLimpio.length() != DIGITOS_CODIGO || !codigoLimpio.matches("\\d+")) {
            throw new OperacionNoPermitidaException("El código debe tener 6 dígitos numéricos.");
        }
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correo.trim())
                .orElseThrow(() -> new OperacionNoPermitidaException(
                        "No existe una cuenta con ese correo."));
        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            return "Este correo ya está verificado. Puedes iniciar sesión.";
        }
        if (usuario.getTokenVerificacion() == null || !usuario.getTokenVerificacion().equals(codigoLimpio)) {
            throw new OperacionNoPermitidaException("El código de verificación no es correcto.");
        }
        if (usuario.getTokenVerificacionExpira() == null
                || usuario.getTokenVerificacionExpira().isBefore(LocalDateTime.now())) {
            throw new OperacionNoPermitidaException(
                    "El código ha expirado. Regístrate de nuevo o solicita un nuevo código.");
        }
        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuario.setTokenVerificacionExpira(null);
        usuarioRepository.save(usuario);
        return "Correo verificado correctamente. Ya puedes iniciar sesión.";
    }
}
