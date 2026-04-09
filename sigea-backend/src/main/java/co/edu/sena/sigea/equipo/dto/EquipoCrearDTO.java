package co.edu.sena.sigea.equipo.dto;

import co.edu.sena.sigea.common.enums.TipoUsoEquipo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
     * Placa de registro SENA — máximo 20 dígitos numéricos. Único por equipo.
     */
    @NotBlank(message = "La placa de registro SENA es obligatoria")
    @Pattern(regexp = "\\d{1,20}", message = "La placa debe contener solo dígitos numéricos (máximo 20)")
    private String placa;

    /**
     * Número de serie del fabricante — obligatorio.
     */
    @NotBlank(message = "El serial es obligatorio")
    @Size(max = 50, message = "El serial no puede superar los 50 caracteres")
    private String serial;

    /**
     * Modelo del equipo — obligatorio.
     */
    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 100, message = "El modelo no puede superar los 100 caracteres")
    private String modelo;

    /**
     * ID de la marca (FK) — obligatorio.
     */
    @NotNull(message = "La marca del equipo es obligatoria")
    private Long marcaId;

    /**
     * Estado del equipo en escala 1-10.
     */
    @Min(value = 1, message = "El estado del equipo debe ser al menos 1")
    @Max(value = 10, message = "El estado del equipo no puede superar 10")
    private Integer estadoEquipoEscala;

    /**
     * Opcional: si viene vacío o null, el sistema genera uno automáticamente.
     */
    @Size(max = 50, message = "El codigo unico no puede superar los 50 caracteres")
    private String codigoUnico;

    @NotNull(message = "La categoria del equipo es obligatoria")
    private Long categoriaId;

    @NotNull(message = "La ubicacion del equipo es obligatoria")
    private Long ambienteId;

    /**
     * Sub-ubicación dentro del ambiente (opcional).
     */
    private Long subUbicacionId;

    /**
     * Solo para ALIMENTADOR_EQUIPOS: asigna el instructor dueño original del
     * equipo.
     */
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
