package co.edu.sena.sigea.transferencia.entity;

// =============================================================================
// ENTIDAD: Transferencia
// =============================================================================
// Registra el movimiento de equipos ENTRE ambientes de formación.
//
// TABLA EN BD: transferencia
//
// REQUERIMIENTOS:
//   RF-AMB-04: Transferir equipos entre ambientes con: origen, destino,
//              equipo, fecha, admin que autoriza, motivo.
//   RN-10: Las transferencias deben ser autorizadas por el admin del origen.
//
// NOTA SOBRE LAS DOS FK A AMBIENTE:
//   ambiente_origen_id → ¿De dónde sale el equipo?
//   ambiente_destino_id → ¿A dónde va el equipo?
//   Ambas apuntan a la MISMA tabla (ambiente). Esto se llama relación reflexiva.
// =============================================================================

import co.edu.sena.sigea.ambiente.entity.Ambiente;
import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.equipo.entity.Equipo;
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

import java.time.LocalDateTime;

@Entity
@Table(name = "transferencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transferencia extends EntidadBase {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    // Inventario (instructor) de donde SALE el equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventario_origen_instructor_id", nullable = false)
    private Usuario inventarioOrigenInstructor;

    // Inventario (instructor) a donde LLEGA el equipo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventario_destino_instructor_id", nullable = false)
    private Usuario inventarioDestinoInstructor;

    // Dueño original del equipo (se mantiene tras la transferencia).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_equipo_id", nullable = false)
    private Usuario propietarioEquipo;

    // Ubicación final opcional dentro del inventario destino.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ubicacion_destino_id")
    private Ambiente ubicacionDestino;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 1;

    // El admin del ambiente de origen que autoriza la transferencia (RN-10)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_autoriza_id", nullable = false)
    private Usuario administradorAutoriza;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "fecha_transferencia", nullable = false)
    private LocalDateTime fechaTransferencia;
}
