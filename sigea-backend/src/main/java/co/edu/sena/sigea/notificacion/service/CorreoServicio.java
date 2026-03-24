package co.edu.sena.sigea.notificacion.service;

import co.edu.sena.sigea.common.exception.ServicioCorreoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoServicio {

    private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:sigea@sena.edu.co}")
    private String remitente;

    public CorreoServicio(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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
            // En desarrollo sin SMTP real, mostrar cuerpo en log para ver
            // códigos/notificaciones
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

}
