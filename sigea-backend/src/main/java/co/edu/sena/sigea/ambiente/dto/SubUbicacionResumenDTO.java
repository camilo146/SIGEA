package co.edu.sena.sigea.ambiente.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resumen de una sub-ubicación (sin incluir sus propias sub-ubicaciones para
 * evitar recursión infinita en la serialización JSON).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubUbicacionResumenDTO {

    private Long id;

    private String nombre;

    private String ubicacion;

    private String descripcion;

    private Boolean activo;
}
