package co.edu.sena.sigea.seguridad.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder

public class LoginRespuestaDTO {
    // El token JWT generado para JWTProveedor
    // El frontend lo alamacena y lo envia en cada peticion
    private String token;

    // Tipo de token, quien porte este token esta autenticado
    private String tipo;

    // Nombre completo del usuario autenticado
    // El front lo usara para mostrar un saludo con el nombre del usuario
    private String nombreCompleto;

    // Rol del usuario autenticado3 6
    // El front lo usara para mostrar opciones basadas en el rol del usuario
    private String rol;

    private Boolean esSuperAdmin;
}
