package co.edu.sena.sigea.observacion.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.observacion.entity.ObservacionEquipo;

@Repository
public interface ObservacionEquipoRepository extends JpaRepository<ObservacionEquipo, Long> {

    List<ObservacionEquipo> findByEquipoIdOrderByFechaRegistroDesc(Long equipoId);

    List<ObservacionEquipo> findByPrestamoId(Long prestamoId);

    @Query("SELECT o FROM ObservacionEquipo o WHERE o.equipo.id = :equipoId " +
            "AND o.fechaRegistro BETWEEN :desde AND :hasta")
    List<ObservacionEquipo> findByEquipoIdAndFechaRegistroBetween(
            @Param("equipoId") Long equipoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT AVG(o.estadoDevolucion) FROM ObservacionEquipo o WHERE o.equipo.id = :equipoId " +
            "AND o.fechaRegistro BETWEEN :desde AND :hasta")
    Double promedioEstadoDevolucionEnPeriodo(
            @Param("equipoId") Long equipoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
