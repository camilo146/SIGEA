package co.edu.sena.sigea.equipo.dto;

import co.edu.sena.sigea.common.enums.TipoUsoEquipo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipoCrearDTO {

    @NotBlank(message = "El nombre del equipo es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar los 200 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripcion no puede superar los 2000 caracteres")
    private String descripcion;

    /**
     * Opcional: si viene vacío o null, el sistema genera uno automáticamente (ej:
     * SIGEA-EQ-1739123456789).
     */
    @Size(max = 50, message = "El codigo unico no puede superar los 50 caracteres")
    private String codigoUnico;

    @NotNull(message = "La categoria del equipo es obligatoria")
    private Long categoriaId;

    @NotNull(message = "La ubicacion del equipo es obligatoria")
    private Long ambienteId;

    /**
     * Sub-ubicación dentro del ambiente (opcional). Debe pertenecer al ambiente
     * indicado en ambienteId.
     */
    private Long subUbicacionId;

    /** Solo para ADMINISTRADOR: asigna el instructor dueño original del equipo. */
    private Long propietarioId;

    @NotNull(message = "La cantidad total es obligatoria")
    @Min(value = 1, message = "La cantidad total debe ser al menos 1")
    private Integer cantidadTotal;

    @NotNull(message = "Debes indicar si el equipo es consumible o no consumible")
    private TipoUsoEquipo tipoUso;

    @NotNull(message = "El umbral minimo es obligatorio")
    @Min(value = 0, message = "El umbral minimo no puede ser negativo")
    private Integer umbralMinimo;
}
