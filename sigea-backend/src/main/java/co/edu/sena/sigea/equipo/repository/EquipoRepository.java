package co.edu.sena.sigea.equipo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.equipo.entity.Equipo;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByCodigoUnico(String codigoUnico);

    boolean existsByCodigoUnico(String codigoUnico);

    List<Equipo> findByActivoTrue();

    long countByActivoTrue();

    List<Equipo> findByCategoriaIdAndActivoTrue(Long categoriaId);

    long countByCategoriaId(Long categoriaId);

    List<Equipo> findByAmbienteIdAndActivoTrue(Long ambienteId);

    List<Equipo> findByPropietarioIdAndActivoTrue(Long propietarioId);

    List<Equipo> findByInventarioActualInstructorIdAndActivoTrue(Long inventarioActualInstructorId);

    List<Equipo> findByEstadoAndActivoTrue(EstadoEquipo estado);

    List<Equipo> findByCantidadDisponibleLessThanEqualAndActivoTrue(Integer umbral);

    @Query("SELECT COUNT(e) FROM Equipo e WHERE e.activo = true AND (e.cantidadDisponible <= e.umbralMinimo OR (e.umbralMinimo = 0 AND e.cantidadDisponible <= 1))")
    long countEquiposConStockBajo();

    Optional<Equipo> findByPlaca(String placa);

    List<Equipo> findByActivoTrueAndEstado(EstadoEquipo estado);
}