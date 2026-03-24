package co.edu.sena.sigea.notificacion.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import co.edu.sena.sigea.common.exception.ServicioWhatsappException;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Service
public class WhatsAppServicio {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppServicio.class);

    private final RestTemplate restTemplate = new RestTemplate();

    private final boolean enabled;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String defaultCountryCode;

    public WhatsAppServicio(
            @Value("${sigea.whatsapp.enabled:false}") boolean enabled,
            @Value("${sigea.whatsapp.twilio.account-sid:}") String accountSid,
            @Value("${sigea.whatsapp.twilio.auth-token:}") String authToken,
            @Value("${sigea.whatsapp.twilio.from:whatsapp:+14155238886}") String fromNumber,
            @Value("${sigea.whatsapp.default-country-code:+57}") String defaultCountryCode) {
        this.enabled = enabled;
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.fromNumber = fromNumber;
        this.defaultCountryCode = defaultCountryCode;
    }

    public boolean estaHabilitado() {
        return enabled;
    }

    public void enviarCodigoVerificacionObligatorio(Usuario usuario, String codigo) {
        if (!enabled) {
            throw new ServicioWhatsappException(
                    "WhatsApp no esta habilitado en el backend. Configure SIGEA_WHATSAPP_ENABLED=true.");
        }
        if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
            throw new ServicioWhatsappException("El usuario no tiene numero de telefono para WhatsApp.");
        }
        validarCredenciales();

        String telefonoDestino = normalizarTelefono(usuario.getTelefono());
        String mensaje = "SIGEA: tu codigo de verificacion es " + codigo + ". Vigencia 24 horas.";
        String endpoint = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", "whatsapp:" + telefonoDestino);
        body.add("From", fromNumber);
        body.add("Body", mensaje);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + codificarBasicAuth(accountSid, authToken));

        try {
            ResponseEntity<String> respuesta = restTemplate.postForEntity(endpoint, new HttpEntity<>(body, headers),
                    String.class);
            HttpStatusCode status = respuesta.getStatusCode();
            if (!status.is2xxSuccessful()) {
                throw new ServicioWhatsappException(
                        "No fue posible enviar el codigo por WhatsApp. Estado proveedor: " + status.value());
            }
            log.info("Codigo de verificacion enviado por WhatsApp a {}", telefonoDestino);
        } catch (RestClientException ex) {
            log.error("Error enviando WhatsApp a {}", telefonoDestino, ex);
            throw new ServicioWhatsappException(
                    "No fue posible enviar el codigo por WhatsApp. Verifique credenciales y numero destino.");
        }
    }

    private void validarCredenciales() {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new ServicioWhatsappException(
                    "Faltan credenciales de Twilio. Configure SIGEA_TWILIO_ACCOUNT_SID y SIGEA_TWILIO_AUTH_TOKEN.");
        }
    }

    private String codificarBasicAuth(String user, String pass) {
        return Base64.getEncoder()
                .encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }

    private String normalizarTelefono(String telefono) {
        String limpio = telefono.replaceAll("[^\\d+]", "").trim();
        if (limpio.startsWith("00")) {
            limpio = "+" + limpio.substring(2);
        }
        if (!limpio.startsWith("+")) {
            limpio = defaultCountryCode + limpio;
        }
        return limpio;
    }
}