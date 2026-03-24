package co.edu.sena.sigea.usuario.dto;

//Data Transfer Object de SALIDA para la informacion del usuario 

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UsuarioRespuestaDTO {

    private Long id; // ID de usuario

    private String nombreCompleto;// nombre completo de usuario

    private String tipoDocumento;// Se retorna como string ("CC", "TI", etc.)
    // para que el frontend no dependa de enums de java

    private String numeroDocumento;

    private String correoElectronico;

    private String telefono;

    private String programaFormacion;

    private String ficha;

    private String rol;// tambien como String para desacoplar el enum de java

    private Boolean esSuperAdmin;// si es super admin

    private Boolean activo;// si esta activo

    private String estadoAprobacion;

    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaActualizacion;

}
