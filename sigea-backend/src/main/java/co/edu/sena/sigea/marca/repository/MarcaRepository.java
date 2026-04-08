package co.edu.sena.sigea.marca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.marca.entity.Marca;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long> {

    List<Marca> findByActivoTrue();

    boolean existsByNombre(String nombre);

    Optional<Marca> findByNombre(String nombre);
}
