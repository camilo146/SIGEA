package co.edu.sena.sigea.auditoria.dto;

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
public class LogAuditoriaRespuestaDTO {

    private Long id;
    // ID único del log

    private Long usuarioId;
    // ID del usuario que ejecutó la acción (null si fue el sistema)

    private String nombreUsuario;
    // Nombre del usuario (null si fue el sistema o tarea programada)

    private String accion;
    // Texto de la acción: "CREAR_EQUIPO", "APROBAR_PRESTAMO", etc.

    private String entidadAfectada;
    // Nombre de la entidad: "Equipo", "Prestamo", "Usuario"

    private Long entidadId;
    // ID del registro afectado en esa tabla

    private String detalles;
    // JSON con el antes/después del cambio

    private String direccionIp;
    // IP desde donde se realizó la acción

    private LocalDateTime fechaHora;
    // Fecha y hora exacta del evento
}