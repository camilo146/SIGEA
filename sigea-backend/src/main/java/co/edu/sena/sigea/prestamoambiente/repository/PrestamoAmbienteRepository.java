package co.edu.sena.sigea.prestamoambiente.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.prestamoambiente.entity.PrestamoAmbiente;
import co.edu.sena.sigea.prestamoambiente.enums.EstadoPrestamoAmbiente;

@Repository
public interface PrestamoAmbienteRepository extends JpaRepository<PrestamoAmbiente, Long> {

    List<PrestamoAmbiente> findBySolicitanteIdOrderByFechaSolicitudDesc(Long solicitanteId);

    List<PrestamoAmbiente> findByPropietarioAmbienteIdOrderByFechaSolicitudDesc(Long propietarioId);

    List<PrestamoAmbiente> findByAmbienteIdOrderByFechaInicioDesc(Long ambienteId);

    List<PrestamoAmbiente> findByEstadoOrderByFechaSolicitudDesc(EstadoPrestamoAmbiente estado);

    /**
     * Verifica si existe un préstamo de ambiente que se solape con el horario
     * solicitado, para los estados activos (APROBADO o ACTIVO).
     */
    @Query("SELECT COUNT(p) > 0 FROM PrestamoAmbiente p " +
            "WHERE p.ambiente.id = :ambienteId " +
            "AND p.estado IN ('APROBADO', 'ACTIVO') " +
            "AND p.fechaInicio <= :fechaFin AND p.fechaFin >= :fechaInicio " +
            "AND p.horaInicio < :horaFin AND p.horaFin > :horaInicio " +
            "AND (:excludeId IS NULL OR p.id <> :excludeId)")
    boolean existeSolapamiento(
            @Param("ambienteId") Long ambienteId,
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("excludeId") Long excludeId);
}
