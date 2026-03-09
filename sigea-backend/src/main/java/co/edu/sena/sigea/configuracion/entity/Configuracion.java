package co.edu.sena.sigea.configuracion.entity;

// =============================================================================
// ENTIDAD: Configuracion
// =============================================================================
// Almacena parámetros de configuración del sistema en formato clave-valor.
//
// TABLA EN BD: configuracion
//
// REQUERIMIENTOS:
//   RF-CFG-01: El super-admin puede ajustar días máximos de préstamo,
//              extensiones permitidas, umbrales de stock, horarios, etc.
//
// ¿POR QUÉ UNA TABLA DE CONFIGURACIÓN?
//   Porque así NO necesitamos tocar el código fuente ni el
//   application.properties cada vez que cambia una regla de negocio.
//
//   Ejemplo de registros:
//     clave: "prestamo.dias.maximo"         valor: "5"    tipo: "INTEGER"
//     clave: "prestamo.extensiones.maximo"   valor: "2"    tipo: "INTEGER"
//     clave: "correo.notificacion.activo"    valor: "true" tipo: "BOOLEAN"
//     clave: "horario.inicio"                valor: "07:00" tipo: "TIME"
//
// PRINCIPIO OCP (Open/Closed):
//   Esta tabla permite AGREGAR nuevas configuraciones (abierta a extensión)
//   sin modificar el código del sistema (cerrada a modificación).
//   Solo se agrega un nuevo registro INSERT y el Service lo lee dinámicamente.
//
// ¿POR QUÉ "tipo" ES VARCHAR Y NO ENUM?
//   Porque los tipos de datos pueden crecer: STRING, INTEGER, BOOLEAN,
//   DOUBLE, DATE, TIME, JSON, etc. Un enum nos obligaría a recompilar.
//   Mantenemos la flexibilidad y el Service se encarga de parsear el valor
//   según el tipo indicado.
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
@Table(name = "configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Configuracion extends EntidadBase {

    // Identificador único de la configuración, ej: "prestamo.dias.maximo"
    // Es UNIQUE porque no puede haber dos configuraciones con la misma clave
    @Column(name = "clave", nullable = false, unique = true, length = 100)
    private String clave;

    // El valor almacenado como String. El Service lo parsea según el "tipo".
    @Column(name = "valor", nullable = false, length = 500)
    private String valor;

    // Tipo de dato: "STRING", "INTEGER", "BOOLEAN", "DOUBLE", etc.
    // Esto le dice al Service cómo interpretar el campo "valor"
    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    // Descripción legible para el admin: "Cantidad máxima de días para un préstamo"
    @Column(name = "descripcion", length = 500)
    private String descripcion;
}
