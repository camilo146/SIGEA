package co.edu.sena.sigea.equipo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoEquipo;
import co.edu.sena.sigea.equipo.entity.Equipo;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    Optional<Equipo> findByCodigoUnico(String codigoUnico);

    boolean existsByCodigoUnico(String codigoUnico);

    List<Equipo> findByActivoTrue();

    List<Equipo> findByCategoriaIdAndActivoTrue(Long categoriaId);

    List<Equipo> findByAmbienteIdAndActivoTrue(Long ambienteId);

    List<Equipo> findByEstadoAndActivoTrue(EstadoEquipo estado);

    List<Equipo> findByCantidadDisponibleLessThanEqualAndActivoTrue(Integer umbral);
}