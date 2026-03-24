package co.edu.sena.sigea.transferencia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.transferencia.entity.Transferencia;

@Repository
// Repositorio para gestionar las transferencias de equipos entre inventarios de
// instructores.
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    List<Transferencia> findByEquipoId(Long equipoId);

    List<Transferencia> findByInventarioOrigenInstructorId(Long instructorId);

    List<Transferencia> findByInventarioDestinoInstructorId(Long instructorId);

    List<Transferencia> findByPropietarioEquipoId(Long instructorId);
}
