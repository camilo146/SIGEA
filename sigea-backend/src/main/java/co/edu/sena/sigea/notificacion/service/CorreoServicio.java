package co.edu.sena.sigea.notificacion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;




@Service
public class CorreoServicio {

        //esto es para enviar los correos, se implementa el metodo enviarCorreo()
        // que recibe el correo del destinatario, el asunto y el mensaje.
        private static final Logger log = LoggerFactory.getLogger(CorreoServicio.class);

        private final JavaMailSender mailSender;

        public CorreoServicio(JavaMailSender mailSender) {
            this.mailSender = mailSender;
        }

        public boolean enviarCorreo(String destinatario, String asunto, String cuerpo) {
            try {
                SimpleMailMessage mensaje  = new SimpleMailMessage();
                mensaje.setTo(destinatario);
                mensaje.setSubject(asunto);
                mensaje.setText(cuerpo);
                mensaje.setFrom("tu_correo@dominio.com");
                mailSender.send(mensaje);
                log.info("Correo enviado a: {}", destinatario);
                return true;
            } catch (Exception e) {
                log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage());
                return false;
            }
        }
    
}
