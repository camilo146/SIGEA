package co.edu.sena.sigea.notificacion.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoEnvio;
import co.edu.sena.sigea.notificacion.entity.Notificacion;

@Repository
// Repositorio para gestionar las notificaciones del sistema (RF-NOT-01).
// proporciona metodos para consultar notificaciones por usuario destino,
// estado de lectura y estado de envio 
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioDestinoId(Long usuarioId);

    List<Notificacion> findByUsuarioDestinoIdAndLeidaFalse(Long usuarioId);

    List<Notificacion> findByEstadoEnvio(EstadoEnvio estadoEnvio);

    long countByUsuarioDestinoIdAndLeidaFalse(Long usuarioId);
}
