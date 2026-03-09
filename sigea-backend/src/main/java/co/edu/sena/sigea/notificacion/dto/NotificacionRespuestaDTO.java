package co.edu.sena.sigea.notificacion.dto;

import java.time.LocalDateTime;

import co.edu.sena.sigea.common.enums.EstadoEnvio;
import co.edu.sena.sigea.common.enums.MedioEnvio;
import co.edu.sena.sigea.common.enums.TipoNotificacion;
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

// Esto es para Mostrar las notificaciones en el front
public class NotificacionRespuestaDTO {

    private long id;
    //Id de la notificacion 

    private long usuarioDestinoId;
    //El usuario al que va dirijida la notificacion 

    private String nombreUsuarioDestino;
    //nombre del usuario al que se envia la notificacion

    private TipoNotificacion tipoNotificacion;
    //Tipo de notificacion (los tipos de notificaciones esta en enum)

    private String mensaje;
    //Mensaje de la notificacion

    private String titulo;
    //Titulo de la notificacion

    private MedioEnvio medioEnvio;
    //Medio por el cual se envio la notificacion (EMAIL, SMS, PUSH)

    private EstadoEnvio estadoEnvio;
    //Estado del envio (ENVIADO, FALLIDO, PENDIENTE)

    private Boolean leida;
    //Indica si el usuario ha leído la notificación (true/false)
    
    private LocalDateTime fechaEnvio;
    //Fecha y hora en que se envió la notificación

    private LocalDateTime fechaCreacion;
    //Fecha y hora en que se creó el registro de la notificación en la base de datos

    
}
