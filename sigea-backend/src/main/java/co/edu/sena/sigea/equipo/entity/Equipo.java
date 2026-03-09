package co.edu.sena.sigea.equipo.entity;

// =============================================================================
// ENTIDAD: Equipo
// =============================================================================
// Representa un equipo o herramienta del inventario del SENA.
// Es la ENTIDAD CENTRAL del sistema: todo gira alrededor de los equipos.
//
// TABLA EN BD: equipo
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-INV-01: Registrar equipos con nombre, descripción, código, categoría,
//              estado, cantidad, ubicación (ambiente), fotografía
//   RF-INV-02: Actualizar información de equipos
//   RF-INV-03: Eliminar equipos (eliminación lógica)
//   RF-INV-04: Stock disponible en tiempo real
//   RF-INV-06: Código único e irrepetible
//   RF-INV-09: Filtrar por estado, categoría, ambiente
//   RF-NOT-04: Umbral mínimo para alertas de stock bajo
//   RF-PRE-10: Descontar/reincorporar stock al prestar/devolver
//
// RELACIONES:
//   - Equipo N ←→ 1 Categoria (cada equipo pertenece a una categoría)
//   - Equipo N ←→ 1 Ambiente (cada equipo está ubicado en un ambiente)
//   - Equipo 1 ←→ N FotoEquipo (un equipo puede tener hasta 3 fotos)
// =============================================================================

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.categoria.entity.Categoria;
import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoEquipo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "equipo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Equipo extends EntidadBase {

    // =========================================================================
    // CAMPO: nombre
    // =========================================================================
    // Nombre del equipo. Ejemplo: "Multímetro digital Fluke 117"
    // =========================================================================
    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    // =========================================================================
    // CAMPO: descripcion
    // =========================================================================
    // Descripción detallada del equipo: marca, modelo, especificaciones, etc.
    // =========================================================================
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // =========================================================================
    // CAMPO: codigoUnico
    // =========================================================================
    // Código identificador ÚNICO e irrepetible para cada equipo.
    //
    // RF-INV-06: "El sistema debe asignar un código único e irrepetible."
    // RN-11: "El código único es irrepetible y no se puede reasignar."
    //
    // Ejemplo: "EQ-TEL-001", "HER-MAN-015", "CAB-UTP-089"
    //
    // unique = true → La BD garantiza que NUNCA habrá dos equipos
    //   con el mismo código. Si alguien intenta insertar un duplicado,
    //   la BD lanza una excepción → el sistema muestra un error amigable.
    // =========================================================================
    @Column(name = "codigo_unico", nullable = false, unique = true, length = 50)
    private String codigoUnico;

    // =========================================================================
    // CAMPO: categoria
    // =========================================================================
    // La categoría a la que pertenece este equipo.
    //
    // @ManyToOne: "Muchos equipos pueden pertenecer a la misma categoría"
    //   Ejemplo: "Multímetro Fluke" y "Multímetro Uni-T" son ambos "Equipos de medición"
    //
    // nullable = false → Todo equipo DEBE tener una categoría (es obligatorio).
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    // =========================================================================
    // CAMPO: estado
    // =========================================================================
    // Estado operativo del equipo: ACTIVO o EN_MANTENIMIENTO
    //
    // ACTIVO = funciona y puede ser prestado.
    // EN_MANTENIMIENTO = está en reparación, NO se puede prestar.
    //
    // NOTA: Esto es DIFERENTE a "activo" (boolean de soft delete).
    //   activo=true, estado=EN_MANTENIMIENTO → "Existe pero está en reparación"
    //   activo=false → "Dado de baja, ya no existe en el inventario"
    // =========================================================================
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEquipo estado = EstadoEquipo.ACTIVO;

    // =========================================================================
    // CAMPO: cantidadTotal
    // =========================================================================
    // Cantidad TOTAL de unidades de este equipo en el inventario.
    // Incluye las prestadas + las disponibles + las en mantenimiento.
    //
    // Ejemplo: Tenemos 10 cables UTP Cat6.
    //   cantidadTotal = 10 (siempre)
    //   cantidadDisponible = 7 (3 están prestados)
    // =========================================================================
    @Column(name = "cantidad_total", nullable = false)
    private Integer cantidadTotal = 1;

    // =========================================================================
    // CAMPO: cantidadDisponible
    // =========================================================================
    // Cantidad de unidades DISPONIBLES para prestar en este momento.
    //
    // RF-INV-04: "El sistema debe mostrar el stock disponible en tiempo real."
    // RF-PRE-10: "Descontar del stock al prestar, reincorporar al devolver."
    //
    // Fórmula: cantidadDisponible <= cantidadTotal (siempre)
    //
    // Cuando se presta: cantidadDisponible -= cantidadPrestada
    // Cuando se devuelve: cantidadDisponible += cantidadDevuelta
    //
    // Si cantidadDisponible <= umbralMinimo → se genera alerta de stock bajo.
    // =========================================================================
    @Column(name = "cantidad_disponible", nullable = false)
    private Integer cantidadDisponible = 1;

    // =========================================================================
    // CAMPO: ambiente
    // =========================================================================
    // El ambiente de formación donde está ubicado físicamente este equipo.
    //
    // @ManyToOne: "Muchos equipos pueden estar en el mismo ambiente"
    //   RF-AMB-03: "Cada ambiente tiene su inventario independiente."
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ambiente_id", nullable = false)
    private Ambiente ambiente;

    // =========================================================================
    // CAMPO: umbralMinimo
    // =========================================================================
    // Cantidad mínima aceptable de unidades disponibles.
    //
    // RF-NOT-04: "El sistema debe generar alertas de stock bajo cuando las
    // unidades disponibles lleguen al umbral mínimo configurado."
    //
    // Si cantidadDisponible <= umbralMinimo → se envía notificación STOCK_BAJO
    //   al administrador del ambiente.
    //
    // Ejemplo: umbralMinimo = 2, cantidadDisponible = 1
    //   → ¡Alerta! Solo queda 1 unidad de este equipo.
    // =========================================================================
    @Column(name = "umbral_minimo", nullable = false)
    private Integer umbralMinimo = 0;

    // =========================================================================
    // CAMPO: activo
    // =========================================================================
    // Soft delete para equipos.
    //
    // RF-INV-03: "Eliminar equipos del inventario (eliminación lógica)."
    // RN-09: "Los equipos eliminados se marcan como dados de baja."
    //
    // activo = false → El equipo fue dado de baja. Ya no aparece en búsquedas
    //   ni se puede prestar, pero su historial de préstamos se conserva.
    // =========================================================================
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
