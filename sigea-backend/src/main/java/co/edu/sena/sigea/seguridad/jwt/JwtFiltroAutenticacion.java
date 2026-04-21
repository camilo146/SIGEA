package co.edu.sena.sigea.seguridad.jwt;

import java.io.IOException; // La "cadena" de filtros (pasa al siguiente)

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder; // Excepción de servlets
import org.springframework.security.core.userdetails.UserDetails; // La petición HTTP entrante
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // La respuesta HTTP saliente

import co.edu.sena.sigea.seguridad.service.UsuarioDetallesServicio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFiltroAutenticacion extends OncePerRequestFilter {

    private final JwtProveedor jwtProveedor;
    private final UsuarioDetallesServicio usuarioDetallesServicio;

    // constructor que inyecta las dependencias
    public JwtFiltroAutenticacion(JwtProveedor jwtProveedor,
            UsuarioDetallesServicio usuarioDetallesServicio) {
        this.jwtProveedor = jwtProveedor;
        this.usuarioDetallesServicio = usuarioDetallesServicio;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || (uri != null && uri.contains("/auth"));
    }

    // doFilterInternal: este metodo se ejecuta en cada peticion HTTP
    // request la peticion que llega
    // response la respuesta que se va a enviar al cliente
    // filterChain la cadena de filtros que se ejecuta despues de este filtro
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String headerAutorizacion = request.getHeader("Authorization");

        if (headerAutorizacion != null && headerAutorizacion.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = headerAutorizacion.substring(7).trim();
                String correoElectronico = jwtProveedor.obtenerCorreoDelToken(token);

                if (correoElectronico != null && jwtProveedor.validarToken(token)) {
                    UserDetails userDetails = usuarioDetallesServicio.loadUserByUsername(correoElectronico);

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
                // Si el token es inválido/expirado, se ignora para no bloquear
                // endpoints públicos como el login. La autorización real se evalúa
                // más adelante en la cadena de seguridad.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

}