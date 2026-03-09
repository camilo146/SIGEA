package co.edu.sena.sigea.prestamo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoExtension;
import co.edu.sena.sigea.prestamo.entity.ExtensionPrestamo;

@Repository
// Repositorio para gestionar las extensiones de los pretamos de equipos (RF-PR-01).
// Propociona metodos para consultar las extenciones por prestamo, estado y contar el numero de estensiones por prestamo.
public interface ExtensionPrestamoRepository extends JpaRepository<ExtensionPrestamo, Long> {

    List<ExtensionPrestamo> findByPrestamoId(Long prestamoId);

    List<ExtensionPrestamo> findByEstado(EstadoExtension estado);

    long countByPrestamoId(Long prestamoId);
}