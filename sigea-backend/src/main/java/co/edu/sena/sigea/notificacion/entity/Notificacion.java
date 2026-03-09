package co.edu.sena.sigea.notificacion.entity;

// =============================================================================
// ENTIDAD: Notificacion
// =============================================================================
// Almacena todas las notificaciones enviadas a los usuarios del sistema.
//
// TABLA EN BD: notificacion
//
// REQUERIMIENTOS:
//   RF-NOT-01: Enviar notificaciones por correo y/o sistema interno.
//   RF-NOT-02: Estados de préstamo, aprobaciones, rechazos, recordatorios.
//   RN-12: Toda notificación debe quedar registrada con: destinatario,
//           tipo, fecha, medio de envío, y estado de envío.
//
// DISEÑO DE NOTIFICACIONES:
//   Esta entidad NO envía la notificación; solo la REGISTRA.
//   El Service será quien:
//     1) Cree el registro en BD (estado = PENDIENTE)
//     2) Intente enviarla (email o sistema)
//     3) Actualice el estado (ENVIADO o FALLIDO)
//
//   Esto sigue el principio SRP (Single Responsibility):
//   - La ENTIDAD solo modela los datos.
//   - El SERVICIO se encarga de la lógica de envío.
//
// ¿POR QUÉ 3 ENUMS DIFERENTES?
//   - TipoNotificacion: ¿Qué se notifica? (PRESTAMO_APROBADO, etc.)
//   - MedioEnvio: ¿Por dónde se envía? (CORREO, SISTEMA, AMBOS)
//   - EstadoEnvio: ¿Se logró enviar? (PENDIENTE, ENVIADO, FALLIDO)
//   Cada enum tiene una RESPONSABILIDAD diferente → SRP aplicado a enums.
// =============================================================================

import co.edu.sena.sigea.common.entity.EntidadBase;
import co.edu.sena.sigea.common.enums.EstadoEnvio;
import co.edu.sena.sigea.common.enums.MedioEnvio;
import co.edu.sena.sigea.common.enums.TipoNotificacion;
import co.edu.sena.sigea.usuario.entity.Usuario;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notificacion extends EntidadBase {

    // ¿A quién va dirigida la notificación?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_destino_id", nullable = false)
    private Usuario usuarioDestino;

    // ¿Qué tipo de evento se notifica? (préstamo aprobado, reserva cancelada, etc.)
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 40)
    private TipoNotificacion tipo;

    // Título corto del mensaje (aparece en la bandeja del usuario)
    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    // Cuerpo completo del mensaje
    @Column(name = "mensaje", columnDefinition = "TEXT", nullable = false)
    private String mensaje;

    // ¿Por qué medio se envió? CORREO, SISTEMA o AMBOS
    @Enumerated(EnumType.STRING)
    @Column(name = "medio_envio", nullable = false, length = 20)
    private MedioEnvio medioEnvio;

    // ¿Se envió correctamente? PENDIENTE → ENVIADO o FALLIDO
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_envio", nullable = false, length = 20)
    private EstadoEnvio estadoEnvio = EstadoEnvio.PENDIENTE;

    // ¿El usuario ya la leyó? (solo aplica para notificaciones internas del sistema)
    @Column(name = "leida", nullable = false)
    private Boolean leida = false;

    // Momento exacto en que se envió (o se intentó enviar)
    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;
}
