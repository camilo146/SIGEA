package co.edu.sena.sigea.equipo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.equipo.entity.FotoEquipo;

@Repository
public interface FotoEquipoRepository extends JpaRepository<FotoEquipo, Long> {

    List<FotoEquipo> findByEquipoId(Long equipoId);

    long countByEquipoId(Long equipoId);
}