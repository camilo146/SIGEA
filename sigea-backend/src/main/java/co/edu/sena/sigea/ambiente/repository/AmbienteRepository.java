package co.edu.sena.sigea.ambiente.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.ambiente.entity.Ambiente;

@Repository
public interface AmbienteRepository extends JpaRepository<Ambiente, Long> {

    Optional<Ambiente> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    List<Ambiente> findByActivoTrue();

    List<Ambiente> findByInstructorResponsableId(Long instructorId);

    /** Devuelve todas las sub-ubicaciones (hijas) del ambiente padre dado. */
    List<Ambiente> findByPadreId(Long padreId);

    /** Verifica si existen sub-ubicaciones activas para un padre dado. */
    boolean existsByPadreIdAndActivoTrue(Long padreId);
}