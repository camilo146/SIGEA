package co.edu.sena.sigea.mantenimiento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.TipoMantenimiento;
import co.edu.sena.sigea.mantenimiento.entity.Mantenimiento;

@Repository
//Repositorio para gestionar los mantenimientos de los equipos (RF-MA-01).
//Proporciona metodos para consultar mantenimientos por equipo, por tipo y mantenimientos activos (sin fecha de fin).
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    List<Mantenimiento> findByEquipoId(Long equipoId);

    List<Mantenimiento> findByTipo(TipoMantenimiento tipo);

    List<Mantenimiento> findByFechaFinIsNull();

    long countByFechaFinIsNull();

    long countByEquipoIdAndFechaFinIsNull(Long equipoId);
}