package co.edu.sena.sigea.seguridad.service;


// Spring Security: interfaces que debemos implementar
import java.util.Collections;  // Contrato: "cómo cargar un usuario"

import org.springframework.security.core.authority.SimpleGrantedAuthority;         // Objeto que Spring Security entiende
import org.springframework.security.core.userdetails.UserDetails; // Excepción si el usuario no existe
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;

@Service 
public class UsuarioDetallesServicio implements UserDetailsService{

    private final UsuarioRepository usuarioRepository;
    
    //constructor para inyectar el repositorio de usuarios
    public UsuarioDetallesServicio(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identificador) throws UsernameNotFoundException {
        // buscar el usuario por correo o por número de documento en la base de datos
        Usuario usuario = usuarioRepository.findByIdentificador(identificador)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Usuario no encontrado: " + identificador));

                    boolean estaCuentaActiva = usuario.getActivo();

                    // Verificar si la cuenta está bloqueada (por ejemplo, por intentos fallidos)
                    boolean estaCuentaBloqueada = false;
                    if (usuario.getCuentaBloqueadaHasta()!= null){
                        estaCuentaBloqueada = usuario.getCuentaBloqueadaHasta()
                        .isAfter  (java.time.LocalDateTime.now());
                    } 
                    
                    
                    return org.springframework.security.core.userdetails.User.builder()
                .username(usuario.getCorreoElectronico() != null && !usuario.getCorreoElectronico().isBlank()
                        ? usuario.getCorreoElectronico()
                        : usuario.getNumeroDocumento())
                .password(usuario.getContrasenaHash())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())
                ))
                .disabled(!estaCuentaActiva)          // true si la cuenta está desactivada
                .accountLocked(estaCuentaBloqueada) 
                .build();
    }
    
}