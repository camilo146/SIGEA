package co.edu.sena.sigea.seguridad.jwt;


import java.io.IOException;        // La "cadena" de filtros (pasa al siguiente)

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;    // Excepción de servlets
import org.springframework.security.core.userdetails.UserDetails;   // La petición HTTP entrante
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;  // La respuesta HTTP saliente

import co.edu.sena.sigea.seguridad.service.UsuarioDetallesServicio;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class JwtFiltroAutenticacion extends OncePerRequestFilter {
    
    private final JwtProveedor jwtProveedor;
    private final UsuarioDetallesServicio usuarioDetallesServicio;
     


    //constructor que inyecta las dependencias 
    public JwtFiltroAutenticacion(JwtProveedor jwtProveedor,
            UsuarioDetallesServicio usuarioDetallesServicio){
                this.jwtProveedor = jwtProveedor;
                this.usuarioDetallesServicio = usuarioDetallesServicio;
            }


    //doFilterInternal: este metodo se ejecuta en cada peticion HTTP
    //request la peticion que llega 
    //response la respuesta que se va a enviar al cliente
    //filterChain la cadena de filtros que se ejecuta despues de este filtro 
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException{
                                        // Extraemos el token del header "Authorization"
                                        String headerAutorizacion = request.getHeader("Authorization");
                                        String token = null;
                                        String correoElectronico = null;

                                        if (headerAutorizacion != null && headerAutorizacion.startsWith("Bearer")){
                                            token = headerAutorizacion.substring(7);// Eliminar "Bearer" del inicio
                                            correoElectronico = jwtProveedor.obtenerCorreoDelToken(token);//extraer correo del usuario que esta adentro del token

                                        }

                                        //validar token y autenticar usuario 

                                        if (correoElectronico != null && SecurityContextHolder.getContext().getAuthentication() == null){
                                            //validar que le token no este exopirado ni alterado
                                            if (jwtProveedor.validarToken(token)){
                                                 //cargar los datos del usuario desde la BD usando eo correo extraido del token 
                                                UserDetails userDetails = usuarioDetallesServicio.loadUserByUsername(correoElectronico);
                                                

                                                //crear un objeto de autenticacion de Spring Security con los detalles del usuario y sus roles
                                                UsernamePasswordAuthenticationToken authToken =
                                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                                                authToken.setDetails(
                                                        new WebAuthenticationDetailsSource().buildDetails(request)
                                                );

                                                //establecer el contexto de seguridad con el usuario autenticado
                                                SecurityContextHolder.getContext().setAuthentication(authToken);

                                        }

                                    }

                    //pasar la peticion al siguiente filtro de la cadena
                    filterChain.doFilter(request, response);

    }


}