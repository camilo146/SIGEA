package co.edu.sena.sigea.seguridad.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // Marca métodos como creadores de beans

import co.edu.sena.sigea.seguridad.jwt.JwtFiltroAutenticacion;
import jakarta.servlet.http.HttpServletRequest;

@Configuration // contiene el beans de configuracion de
@EnableWebSecurity // habilita la seguridad web de Spring Security
@EnableMethodSecurity // habilita la seguridad a nivel de metodos (con @PreAuthorize)

public class SecurityConfig {
    // Inyectamos el filtro de autenticacion JWT para que se ejecute en cada
    // peticion HTTP
    private final JwtFiltroAutenticacion jwtFiltroAutenticacion;

    /**
     * URL(s) del frontend/app en producción. Soporta múltiples URLs separadas por
     * coma (ej: http://192.168.4.250:4043,https://sigea.web-virtual.com:4043).
     */
    @Value("${sigea.app.url:http://localhost:8082}")
    private String appUrl;

    // Constructor para inyectar el filtro de autenticacion JWT
    public SecurityConfig(JwtFiltroAutenticacion jwtFiltroAutenticacion) {
        this.jwtFiltroAutenticacion = jwtFiltroAutenticacion;
    }

    // Configuración de seguridad HTTP
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitAuthPath()).permitAll()
                        .requestMatchers("/actuator/**", "/api/v1/actuator/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .anyRequest().authenticated()

                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .addFilterBefore(jwtFiltroAutenticacion, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    /**
     * Permite cualquier ruta de autenticación (login/registro) sin importar
     * context-path o proxy.
     */
    private static RequestMatcher permitAuthPath() {
        return (HttpServletRequest request) -> {
            String uri = request.getRequestURI();
            String path = request.getServletPath();
            return (uri != null && uri.contains("/auth")) || (path != null && path.contains("/auth"));
        };
    }

    @Bean
    // Configuración del PasswordEncoder para encriptar las contraseñas de los
    // usuarios
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    // Exponer el AuthenticationManager para que pueda ser utilizado en el
    // controlador de autenticación
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    // Configuración de CORS. appUrl soporta múltiples orígenes separados por coma
    // para producción (ej: http://192.168.4.250:4043,https://sigea.web-virtual.com:4043).
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Construir lista de orígenes: localhost (dev) + URLs de producción
        List<String> allowedOrigins = new ArrayList<>(
                List.of("http://localhost:*", "http://127.0.0.1:*",
                        "http://localhost:4200", "http://localhost:58648"));
        Arrays.stream(appUrl.split(","))
                .map(String::trim)
                .filter(u -> !u.isEmpty())
                .forEach(allowedOrigins::add);

        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}