package co.edu.sena.sigea.notificacion.service;

import java.util.Map;

import co.edu.sena.sigea.common.exception.ServicioCorreoException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class CorreoServicio {

    private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:sigea@sena.edu.co}")
    private String remitente;

    public CorreoServicio(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /** Envía correo de texto plano. Retorna true si fue exitoso. */
    public boolean enviarCorreo(String destinatario, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mensaje.setFrom(remitente);
            mailSender.send(mensaje);
            log.info("Correo enviado a: {}", destinatario);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar correo a {}", destinatario, e);
            log.info("Contenido que no se pudo enviar a {} | Asunto: {} | Cuerpo:\n{}", destinatario, asunto, cuerpo);
            return false;
        }
    }

    public void enviarCorreoObligatorio(String destinatario, String asunto, String cuerpo) {
        boolean enviado = enviarCorreo(destinatario, asunto, cuerpo);
        if (!enviado) {
            throw new ServicioCorreoException(
                    "No fue posible enviar el correo de verificacion. Revise la configuracion SMTP del backend e intente de nuevo.");
        }
    }

    /**
     * Envía correo HTML usando una plantilla Thymeleaf.
     *
     * @param destinatario correo destino
     * @param asunto       asunto del mensaje
     * @param plantilla    nombre de la plantilla sin extensión, ej:
     *                     "correos/correo-verificacion"
     * @param variables    mapa de variables para el contexto Thymeleaf
     * @return true si fue enviado con éxito
     */
    public boolean enviarCorreoHtml(String destinatario, String asunto, String plantilla,
            Map<String, Object> variables) {
        try {
            Context contexto = new Context();
            if (variables != null) {
                variables.forEach(contexto::setVariable);
            }
            String contenidoHtml = templateEngine.process(plantilla, contexto);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setFrom(remitente);
            helper.setText(contenidoHtml, true);

            mailSender.send(mimeMessage);
            log.info("Correo HTML enviado a: {} (plantilla: {})", destinatario, plantilla);
            return true;
        } catch (Exception e) {
            log.error("Error al enviar correo HTML a {} con plantilla {}", destinatario, plantilla, e);
            return false;
        }
    }

    /** Envía correo HTML y lanza excepción si falla. */
    public void enviarCorreoHtmlObligatorio(String destinatario, String asunto, String plantilla,
            Map<String, Object> variables) {
        boolean enviado = enviarCorreoHtml(destinatario, asunto, plantilla, variables);
        if (!enviado) {
            throw new ServicioCorreoException(
                    "No fue posible enviar el correo. Revise la configuracion SMTP del backend e intente de nuevo.");
        }
    }
}
