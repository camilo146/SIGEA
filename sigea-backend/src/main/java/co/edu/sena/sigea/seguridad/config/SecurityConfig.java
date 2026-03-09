package co.edu.sena.sigea.seguridad.config;


import java.util.Arrays;
import java.util.List;

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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;          // Marca métodos como creadores de beans

import co.edu.sena.sigea.seguridad.jwt.JwtFiltroAutenticacion; 

@Configuration //contiene el beans de configuracion de
@EnableWebSecurity //habilita la seguridad web de Spring Security
@EnableMethodSecurity //habilita la seguridad a nivel de metodos (con @PreAuthorize) 


public class SecurityConfig {
    //Inyectamos el filtro de autenticacion JWT para que se ejecute en cada peticion HTTP
    private final JwtFiltroAutenticacion jwtFiltroAutenticacion;

    // Constructor para inyectar el filtro de autenticacion JWT
    public SecurityConfig(JwtFiltroAutenticacion jwtFiltroAutenticacion){
        this.jwtFiltroAutenticacion = jwtFiltroAutenticacion;
    }

    // Configuración de seguridad HTTP
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http 
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(auth -> auth 
        .requestMatchers("/auth/**").permitAll()
        .requestMatchers("/actuator/**").permitAll()
        .anyRequest().authenticated()

        )

        .sessionManagement(session -> session 
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

          .addFilterBefore(jwtFiltroAutenticacion, UsernamePasswordAuthenticationFilter.class);

          return http.build();


    }

        @Bean
        // Configuración del PasswordEncoder para encriptar las contraseñas de los usuarios 
        public PasswordEncoder passwordEncoder(){
            return new BCryptPasswordEncoder();
        }  

        @Bean
        // Exponer el AuthenticationManager para que pueda ser utilizado en el controlador de autenticación
        public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
        ) throws Exception {
            return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean 
        // Configuración de CORS para permitir solicitudes desde el frontend
        public CorsConfigurationSource corsConfigurationSource(){
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOriginPatterns(List.of("*")); // Permitir todas las fuentes (en producción, especificar dominios)
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            configuration.setAllowedHeaders(List.of("*")); // Permitir todos los encabezados
            configuration.setAllowCredentials(true); // Permitir envío de cookies y credenciales

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration); // Aplicar esta configuración a todas las rutas
            return source;
        }


}