package co.edu.sena.sigea.ambiente.entity;

// =============================================================================
// ENTIDAD: Ambiente
// =============================================================================
// Representa un espacio físico de formación del SENA donde se ubican equipos.
// Ejemplos: "Laboratorio de Telecomunicaciones", "Taller de Redes", etc.
//
// TABLA EN BD: ambiente
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-AMB-01: Crear ambientes con nombre, ubicación, descripción, instructor
//   RF-AMB-02: Actualizar y desactivar ambientes
//   RF-AMB-03: Cada ambiente tiene su inventario independiente
//   RF-AMB-05: Admin gestiona solo SUS ambientes, superadmin gestiona todos
//
// RELACIONES:
//   - Un Ambiente tiene UN instructor responsable (ManyToOne → Usuario)
//   - Un Ambiente tiene MUCHOS equipos (OneToMany → Equipo)
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ambiente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ambiente extends EntidadBase {

    // =========================================================================
    // CAMPO: nombre
    // =========================================================================
    // Nombre del ambiente de formación.
    // Ejemplo: "Laboratorio de Telecomunicaciones - Piso 2"
    // =========================================================================
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    // =========================================================================
    // CAMPO: ubicacion
    // =========================================================================
    // Descripción de la ubicación física del ambiente dentro del centro.
    // Ejemplo: "Edificio A, Piso 2, Salón 205"
    // =========================================================================
    @Column(name = "ubicacion", length = 200)
    private String ubicacion;

    // =========================================================================
    // CAMPO: descripcion
    // =========================================================================
    // Descripción adicional del ambiente (qué tipo de formación se imparte, etc.)
    // =========================================================================
    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "direccion", length = 250)
    private String direccion;

    // =========================================================================
    // CAMPO: instructorResponsable
    // =========================================================================
    // El instructor (usuario con rol ADMINISTRADOR) encargado de este ambiente.
    //
    // @ManyToOne:
    // "Muchos ambientes pueden tener al mismo instructor como responsable"
    // (un instructor puede ser responsable de varios ambientes)
    //
    // Relación: Ambiente N ←→ 1 Usuario
    //
    // @JoinColumn(name = "instructor_responsable_id"):
    // Le dice a JPA: "En la tabla 'ambiente', crea una columna llamada
    // 'instructor_responsable_id' que es FK hacia la tabla 'usuario'."
    //
    // FetchType.LAZY:
    // "No cargues los datos del instructor inmediatamente."
    // Solo cárgalos cuando ALGUIEN acceda a getInstructorResponsable().
    //
    // ¿Por qué LAZY?
    // Si cargáramos EAGER (inmediato), cada vez que consultes un ambiente,
    // Hibernate haría un JOIN con la tabla usuario. Si consultas 100 ambientes,
    // son 100 JOINs innecesarios. Con LAZY, solo hace el JOIN cuando lo necesitas.
    // Esto es PERFORMANCE OPTIMIZATION.
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_responsable_id")
    private Usuario instructorResponsable;

    // =========================================================================
    // CAMPO: activo
    // =========================================================================
    // Soft delete para ambientes (RF-AMB-02: "desactivar ambientes").
    // Un ambiente inactivo no aparece en las listas pero sus datos se conservan.
    // =========================================================================
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Ruta opcional de la foto del ambiente (ej. /uploads/ambientes/uuid.jpg). */
    @Column(name = "ruta_foto", length = 500)
    private String rutaFoto;
}
