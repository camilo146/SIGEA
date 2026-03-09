
package co.edu.sena.sigea.configuracion.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.configuracion.entity.Configuracion;

@Repository
// Repositorio para gestionar las configuraciones del sistema (RF-CF-01).
// Proporciona metodos para consultar configuraciones por clave y verificar la existencia de una clave.
public interface ConfiguracionRepository extends JpaRepository<Configuracion, Long> {

    Optional<Configuracion> findByClave(String clave);

    boolean existsByClave(String clave);
}