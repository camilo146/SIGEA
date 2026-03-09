package co.edu.sena.sigea.equipo.dto;


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

// DTO para enviar información de una foto de equipo al cliente
public class FotoEquipoRespuestaDTO {

    private Long id;

    private String nombreArchivo;

    private String rutaArchivo;

    private Long tamanoBytes;

    private LocalDateTime fechaSubida;
}
