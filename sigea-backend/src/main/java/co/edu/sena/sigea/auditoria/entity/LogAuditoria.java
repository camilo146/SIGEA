package co.edu.sena.sigea.auditoria.entity;

// =============================================================================
// ENTIDAD: LogAuditoria
// =============================================================================
// Registra TODA acción relevante realizada en el sistema para trazabilidad.
//
// TABLA EN BD: log_auditoria
//
// REQUERIMIENTOS:
//   RF-AUD-01: Registrar acciones de creación, actualización y eliminación.
//   RF-AUD-02: Registrar quién, cuándo, qué entidad, qué cambió y desde dónde.
//   RN-13: Los logs de auditoría NO se pueden modificar ni eliminar.
//
// DISEÑO CLAVE:
//   1) Esta entidad NO extiende EntidadBase.
//      ¿Por qué? Porque EntidadBase tiene fechaActualizacion y eso implica
//      que el registro se puede modificar. Un log de auditoría es INMUTABLE.
//
//   2) El campo "usuario" es @ManyToOne NULLABLE.
//      ¿Por qué? Porque algunas acciones ocurren sin usuario autenticado:
//      - Tareas programadas (cron jobs)
//      - Eventos del sistema (startup, shutdown)
//      - Acciones de login fallido (el usuario aún no está autenticado)
//
//   3) Usamos VARCHAR para "accion" en vez de un enum.
//      ¿Por qué? Porque las acciones son muy variadas:
//      "CREAR_EQUIPO", "APROBAR_PRESTAMO", "LOGIN_EXITOSO", etc.
//      Un enum obligaría a recompilar cada vez que se agrega una acción.
//      Esto sigue el principio OCP (Open/Closed): la clase está abierta
//      a nuevos tipos de acción sin modificar su código.
//
//   4) "detalles" es TEXT para almacenar JSON con el antes/después.
//      Ejemplo: {"campo":"estado","antes":"DISPONIBLE","despues":"PRESTADO"}
// =============================================================================

import java.time.LocalDateTime;

import co.edu.sena.sigea.usuario.entity.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Entidad que representa un log de auditoría, registra acciones relevantes del sistema para trazabilidad, 
// incluyendo quién hizo qué, cuándo, desde dónde, y qué cambió.
public class LogAuditoria {

    // ID propio porque NO extiende EntidadBase
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Usuario que realizó la acción (puede ser null para acciones del sistema)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    // ¿Qué acción se realizó? Ej: "CREAR_EQUIPO", "APROBAR_PRESTAMO"
    @Column(name = "accion", nullable = false, length = 100)
    private String accion;

    // ¿Qué entidad fue afectada? Ej: "Equipo", "Prestamo", "Usuario"
    @Column(name = "entidad_afectada", nullable = false, length = 100)
    private String entidadAfectada;

    // ¿Cuál fue el ID de la entidad afectada?
    @Column(name = "entidad_id")
    private Long entidadId;

    // JSON con el detalle del cambio (antes/después)
    @Column(name = "detalles", columnDefinition = "TEXT")
    private String detalles;

    // Dirección IP desde donde se realizó la acción
    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    // Momento exacto en que ocurrió la acción
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    // Establecer fecha automáticamente antes de persistir (similar a EntidadBase)
    @jakarta.persistence.PrePersist
    protected void alCrear() {
        this.fechaHora = LocalDateTime.now();
    }
}
