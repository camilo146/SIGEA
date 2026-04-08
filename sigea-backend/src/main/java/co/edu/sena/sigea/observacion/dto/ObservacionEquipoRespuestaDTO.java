package co.edu.sena.sigea.observacion.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservacionEquipoRespuestaDTO {

    private Long id;
    private Long prestamoId;
    private Long equipoId;
    private String equipoNombre;
    private String equipoPlaca;
    private Long usuarioDuenioId;
    private String usuarioDuenioNombre;
    private Long usuarioPrestatarioId;
    private String usuarioPrestatarioNombre;
    private String observaciones;
    private Integer estadoDevolucion;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaCreacion;
}
