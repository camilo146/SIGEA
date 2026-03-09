package co.edu.sena.sigea.auditoria.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.auditoria.entity.LogAuditoria;

@Repository
// Repositorio para gestionar los logs de auditoria (RF-AU-01).
// Proporciona metodos para consultar logs por usuario, entidad afectada y rango de fechas
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findByUsuarioId(Long usuarioId);

    List<LogAuditoria> findByEntidadAfectadaAndEntidadId(String entidad, Long entidadId);

    List<LogAuditoria> findByFechaHoraBetween(LocalDateTime desde, LocalDateTime hasta);
}
