package co.edu.sena.sigea.prestamo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.prestamo.entity.Prestamo;

@Repository
// Repositorio para gestionar las operaciones de préstamo de equipos (RF-PRE-01).
// Proporciona métodos para consultar préstamos por usuario, estado y fechas.
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioSolicitanteId(Long usuarioId);

    List<Prestamo> findByEstado(EstadoPrestamo estado);

    List<Prestamo> findByUsuarioSolicitanteIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    List<Prestamo> findByFechaHoraDevolucionEstimadaBeforeAndEstado(
            LocalDateTime fecha, EstadoPrestamo estado
    );

    long countByUsuarioSolicitanteIdAndEstado(Long usuarioId, EstadoPrestamo estado);

    long countByEstado(EstadoPrestamo estado);

    /** RF-REP-02: Historial de préstamos con filtro por rango de fechas. */
    List<Prestamo> findByFechaHoraSolicitudBetween(LocalDateTime desde, LocalDateTime hasta);

    /** Préstamos que incluyen un equipo dado (vía detalle). RF-REP-02 filtro por equipo. */
    @Query("SELECT DISTINCT d.prestamo FROM DetallePrestamo d WHERE d.equipo.id = :equipoId")
    List<Prestamo> findByDetallesEquipoId(Long equipoId);
}