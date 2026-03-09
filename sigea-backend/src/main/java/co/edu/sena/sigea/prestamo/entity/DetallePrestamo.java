package co.edu.sena.sigea.prestamo.entity;

// =============================================================================
// ENTIDAD: DetallePrestamo (DETALLE / LÍNEA DEL PRÉSTAMO)
// =============================================================================
// Representa UN equipo específico incluido dentro de un préstamo.
// Es la "línea de la factura": qué equipo, cuántas unidades, en qué estado.
//
// TABLA EN BD: detalle_prestamo
//
// REQUERIMIENTOS QUE CUBRE:
//   RF-PRE-03: Registrar equipo(s) prestado(s), observaciones sobre el estado
//   RF-PRE-04: Estado del equipo al devolver
//   RF-PRE-10: Descontar del stock al prestar, reincorporar al devolver
//   RN-03: Documentar estado del equipo al prestar
//   RN-04: Documentar estado del equipo al devolver
//
// RESUELVE LA RELACIÓN MUCHOS A MUCHOS:
//   Un préstamo puede incluir MUCHOS equipos.
//   Un equipo puede aparecer en MUCHOS préstamos (a lo largo del tiempo).
//   Préstamo N ←→ N Equipo → se resuelve con esta tabla intermedia enriquecida.
//
//   ¿Por qué "enriquecida"?
//   Una tabla intermedia simple solo tendría: prestamo_id + equipo_id.
//   Pero nosotros necesitamos datos ADICIONALES por cada línea: cantidad,
//   estado del equipo al entregar/devolver, observaciones, si fue devuelto.
//   Por eso es una ENTIDAD completa, no solo una tabla join.
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoCondicion;
import co.edu.sena.sigea.equipo.entity.Equipo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_prestamo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetallePrestamo extends EntidadBase {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;


    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;


    @Enumerated(EnumType.STRING)
    @Column(name = "estado_equipo_entrega", length = 15)
    private EstadoCondicion estadoEquipoEntrega;


    @Column(name = "observaciones_entrega", columnDefinition = "TEXT")
    private String observacionesEntrega;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_equipo_devolucion", length = 15)
    private EstadoCondicion estadoEquipoDevolucion;

  
    @Column(name = "observaciones_devolucion", columnDefinition = "TEXT")
    private String observacionesDevolucion;

 
    @Column(name = "devuelto", nullable = false)
    private Boolean devuelto = false;


    @OneToOne(mappedBy = "detallePrestamo", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReporteDano reporteDano;
}
