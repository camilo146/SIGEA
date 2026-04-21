package co.edu.sena.sigea.categoria.entity;

// =============================================================================
// ENTIDAD: Categoria
// =============================================================================
// Representa una clasificación de equipos del inventario.
//
// TABLA EN BD: categoria
//
// REQUERIMIENTO QUE CUBRE:
//   RF-INV-05: El sistema debe clasificar equipos por categorías:
//     herramientas manuales, equipos de medición, dispositivos de red,
//     cables y conectores, equipos de protección, otros.
//
// ¿POR QUÉ ES UNA TABLA Y NO UN ENUM?
//   Principio SOLID - Open/Closed:
//   Si el SENA necesita agregar una nueva categoría (ej: "Fibra óptica"),
//   solo se inserta un registro en la BD. NO se modifica código Java,
//   NO se recompila el proyecto, NO se redespliegue.
//
//   Si fuera un Enum, cada nueva categoría requeriría:
//   1. Modificar el archivo Enum
//   2. Recompilar el backend
//   3. Redesplegar la aplicación
//   Eso es costoso y riesgoso en producción.
//
//   Regla general:
//   - ¿Los valores SON FIJOS y están en la LÓGICA del programa? → ENUM
//     (ej: EstadoPrestamo → SOLICITADO, APROBADO... el flujo depende de esto)
//   - ¿Los valores son DATOS que el usuario podría querer modificar? → TABLA
//     (ej: Categoría → el admin podría querer agregar categorías nuevas)
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// Entidad que representa una categoría de equipo en el inventario, 
// con campos para nombre, descripción y estado activo.
public class Categoria extends EntidadBase {

    // =========================================================================
    // CAMPO: nombre
    // =========================================================================
    // Nombre de la categoría. Ejemplo: "Herramientas manuales"
    //
    // unique = true → No pueden existir dos categorías con el mismo nombre.
    //   Esto previene duplicados como "Herramientas" y "herramientas".
    //   (La unicidad a nivel de BD es case-sensitive por defecto en MariaDB,
    //    a menos que se configure un collation case-insensitive)
    // =========================================================================
    @Column(name = "nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    // =========================================================================
    // CAMPO: descripcion
    // =========================================================================
    // Descripción opcional de la categoría.
    // Ejemplo: "Herramientas que no requieren energía eléctrica para funcionar"
    //
    // @Column(columnDefinition = "TEXT"):
    //   Usa el tipo TEXT de MariaDB en vez de VARCHAR.
    //   TEXT puede almacenar hasta 65,535 caracteres.
    //   Útil para descripciones largas donde no sabemos el tamaño máximo.
    // =========================================================================
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    // =========================================================================
    // CAMPO: activo
    // =========================================================================
    // Soft delete: si es false, la categoría no aparece en listas desplegables.
    // Pero NO se borra de la BD para no romper las referencias de equipos
    // que ya están asociados a esta categoría.
    //
    // ¿Qué pasaría si borráramos físicamente?
    //   Los equipos que tenían categoria_id = 5 (la categoría borrada) quedarían
    //   con una FK "huérfana" → error de integridad referencial.
    // =========================================================================
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
