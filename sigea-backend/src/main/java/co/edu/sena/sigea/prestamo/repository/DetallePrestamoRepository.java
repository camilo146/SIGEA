package co.edu.sena.sigea.prestamo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.prestamo.entity.DetallePrestamo;

@Repository
// Repositorio para gestionar los detalles de los préstamos de equipos (RF-PRE-01).
// Proporciona métodos para consultar detalles por préstamo, equipo y estado de devolución.
public interface DetallePrestamoRepository extends JpaRepository<DetallePrestamo, Long> {

    List<DetallePrestamo> findByPrestamoId(Long prestamoId);

    List<DetallePrestamo> findByEquipoId(Long equipoId);

    List<DetallePrestamo> findByPrestamoIdAndDevueltoFalse(Long prestamoId);

    /** RF-REP-03: Ranking de equipos más solicitados. Retorna [equipoId, cantidad] ordenado por cantidad DESC. */
    @Query("SELECT d.equipo.id, COUNT(d) FROM DetallePrestamo d GROUP BY d.equipo.id ORDER BY COUNT(d) DESC")
    List<Object[]> countPrestamosByEquipoId();
}