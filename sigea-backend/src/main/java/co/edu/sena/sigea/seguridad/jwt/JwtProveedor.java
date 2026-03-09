
     
package co.edu.sena.sigea.seguridad.jwt;


import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;           

import io.jsonwebtoken.Claims;  // Para generar la clave de firma
import io.jsonwebtoken.Jwts;  // Para convertir String a bytes (UTF-8)
import io.jsonwebtoken.security.Keys;                     // Para manejar fechas de expiración


@Component  // Spring lo registra como bean para poder inyectarlo con @Autowired
public class JwtProveedor {

   
    private final SecretKey claveSecreta;

    
    private final long tiempoExpiracionMs;

    
    public JwtProveedor(
            @Value("${sigea.jwt.secret}") String secreto,
            @Value("${sigea.jwt.expiration-ms}") long tiempoExpiracionMs) {
        // Convertir la cadena del properties a una clave criptográfica
        this.claveSecreta = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.tiempoExpiracionMs = tiempoExpiracionMs;
    }
    public String generarToken(String correo, String rol) {
        Date ahora = new Date();
        // La fecha de expiración = ahora + 30 minutos 
        Date expiracion = new Date(ahora.getTime() + tiempoExpiracionMs);

        return Jwts.builder()
                // subject: identifica al dueño del token (usamos el correo)
                .subject(correo)
                // claim personalizado: guardamos el rol dentro del token
                // Así no necesitamos ir a la BD cada vez para saber el rol
                .claim("rol", rol)
                // issuedAt: cuándo se creó el token
                .issuedAt(ahora)
                // expiration: cuándo expira (después de esto, es inválido)
                .expiration(expiracion)
                // signWith: firma el token con nuestra clave secreta
                // Si alguien cambia el contenido, la firma no coincidirá
                .signWith(claveSecreta)
                // compact: construye el String final del token
                .compact();
    }

    
    public boolean validarToken(String token) {
        try {
            
            Jwts.parser()
                    .verifyWith(claveSecreta)
                    .build()
                    .parseSignedClaims(token);
            return true;  // Si llega aquí, el token es válido
        } catch (Exception e) {
            
            return false;
        }
    }

    
    public String obtenerCorreoDelToken(String token) {
        return extraerClaims(token).getSubject();
    }

    
    public String obtenerRolDelToken(String token) {
        return extraerClaims(token).get("rol", String.class);
    }


    private Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(claveSecreta)
                .build()
                .parseSignedClaims(token)
                .getPayload();  // getPayload() devuelve los Claims (datos)
    }
}