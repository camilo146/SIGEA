package co.edu.sena.sigea.reserva.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoReserva;
import co.edu.sena.sigea.reserva.entity.Reserva;

@Repository
// Repositorio para gestionar las reservas de equipos (RF-RES-01 a RF-RES-04).
// Proporciona métodos para consultar reservas por usuario, equipo, estado,
// fechas y reservas solapadas (para validar disponibilidad).
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByEquipoIdAndEstado(Long equipoId, EstadoReserva estado);

    long countByEquipoId(Long equipoId);

    List<Reserva> findByEstado(EstadoReserva estado);

    long countByEstado(EstadoReserva estado);

    // RF-RES-02: Reservas ACTIVAS cuya ventana ya terminó (para expirar automáticamente).
    List<Reserva> findByEstadoAndFechaHoraFinBefore(EstadoReserva estado, LocalDateTime fecha);

    // RF-RES-04: Reservas ACTIVAS que se solapan con [inicio, fin] para un equipo.
    // Se usa para calcular cantidad ya reservada en ese periodo y validar disponibilidad.
    @Query("SELECT r FROM Reserva r WHERE r.equipo.id = :equipoId AND r.estado = :estado "
            + "AND r.fechaHoraInicio < :fin AND r.fechaHoraFin > :inicio")
    List<Reserva> findReservasSolapadas(@Param("equipoId") Long equipoId,
                                       @Param("estado") EstadoReserva estado,
                                       @Param("inicio") LocalDateTime inicio,
                                       @Param("fin") LocalDateTime fin);
}