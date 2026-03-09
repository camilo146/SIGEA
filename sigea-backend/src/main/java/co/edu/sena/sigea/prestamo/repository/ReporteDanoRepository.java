package co.edu.sena.sigea.prestamo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.prestamo.entity.ReporteDano;

@Repository
// Repositorio para gestionar los reportes de daño asociados a los detalles de prestamo (RF-PR-01).
// Proporciona metodos para consultar reportes por detalle de prestamo y verificar su existencia.
public interface ReporteDanoRepository extends JpaRepository<ReporteDano, Long> {

    Optional<ReporteDano> findByDetallePrestamoId(Long detallePrestamoId);

    boolean existsByDetallePrestamoId(Long detallePrestamoId);
}