package co.edu.sena.sigea.auditoria.service;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.auditoria.dto.LogAuditoriaRespuestaDTO;
import co.edu.sena.sigea.auditoria.entity.LogAuditoria;
import co.edu.sena.sigea.auditoria.repository.LogAuditoriaRepository;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Service
// Servicio para gestionar los logs de auditoria (RF-AU-01).
// Proporciona metodos para registrar un nuevo log (llamado desde otros servicios)
// y para listar logs con diferentes filtros (todos, por usuario, por entidad, por rango de fechas). 
public class AuditoriaServicio {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public AuditoriaServicio(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
        // Inyección por constructor: Spring entrega el repositorio automáticamente
    }

    // =========================================================================
    // REGISTRAR — llamado desde otros servicios para guardar un log
    // =========================================================================
    // Este método NO devuelve nada: su único propósito es persistir el log.
    // "usuario" puede ser null cuando la acción la hace el sistema (cron job).
    @Transactional
    public void registrar(Usuario usuario, String accion,
                          String entidadAfectada, Long entidadId,
                          String detalles, String direccionIp) {
        LogAuditoria log = LogAuditoria.builder()
                .usuario(usuario)
                // usuario puede ser null → logs de tareas automáticas
                .accion(accion)
                // Ej: "CREAR_EQUIPO", "APROBAR_PRESTAMO"
                .entidadAfectada(entidadAfectada)
                // Ej: "Equipo", "Prestamo"
                .entidadId(entidadId)
                // ID del registro afectado en su tabla
                .detalles(detalles)
                // JSON opcional: {"campo":"estado","antes":"ACTIVO","despues":"BAJA"}
                .direccionIp(direccionIp)
                // IP del cliente HTTP
                .build();
        // @PrePersist en la entidad pondrá fechaHora = LocalDateTime.now() automáticamente
        logAuditoriaRepository.save(log);
    }

    // =========================================================================
    // LISTAR TODOS los logs — solo para administradores
    // =========================================================================
    @Transactional(readOnly = true)
    public List<LogAuditoriaRespuestaDTO> listarTodos() {
        return logAuditoriaRepository.findAll()
                .stream()
                // Convierte cada LogAuditoria a su DTO de respuesta
                .map(this::mapear)
                .toList();
    }

    // =========================================================================
    // LISTAR POR USUARIO — ver qué hizo un usuario específico
    // =========================================================================
    @Transactional(readOnly = true)
    public List<LogAuditoriaRespuestaDTO> listarPorUsuario(Long usuarioId) {
        return logAuditoriaRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapear)
                .toList();
    }

    // =========================================================================
    // LISTAR POR ENTIDAD — ver todo lo que le pasó a un registro específico
    // Ej: todos los logs del Equipo con ID 5
    // =========================================================================
    @Transactional(readOnly = true)
    public List<LogAuditoriaRespuestaDTO> listarPorEntidad(String entidad, Long entidadId) {
        return logAuditoriaRepository
                .findByEntidadAfectadaAndEntidadId(entidad, entidadId)
                .stream()
                .map(this::mapear)
                .toList();
    }

    // =========================================================================
    // LISTAR POR RANGO DE FECHAS — filtrar logs entre dos momentos
    // =========================================================================
    @Transactional(readOnly = true)
    public List<LogAuditoriaRespuestaDTO> listarPorRangoFechas(
            LocalDateTime desde, LocalDateTime hasta) {
        return logAuditoriaRepository.findByFechaHoraBetween(desde, hasta)
                .stream()
                .map(this::mapear)
                .toList();
    }

    // =========================================================================
    // MAPEAR — convierte entidad → DTO (método privado de apoyo)
    // =========================================================================
    private LogAuditoriaRespuestaDTO mapear(LogAuditoria log) {
        return LogAuditoriaRespuestaDTO.builder()
                .id(log.getId())
                .usuarioId(log.getUsuario() != null ? log.getUsuario().getId() : null)
                // Si el usuario es null (acción del sistema), el campo queda null
                .nombreUsuario(log.getUsuario() != null
                        ? log.getUsuario().getNombreCompleto() : null)
                // Igual para el nombre
                .accion(log.getAccion())
                .entidadAfectada(log.getEntidadAfectada())
                .entidadId(log.getEntidadId())
                .detalles(log.getDetalles())
                .direccionIp(log.getDireccionIp())
                .fechaHora(log.getFechaHora())
                .build();
    }
}