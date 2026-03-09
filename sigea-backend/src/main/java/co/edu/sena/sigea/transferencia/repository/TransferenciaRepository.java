package co.edu.sena.sigea.transferencia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.transferencia.entity.Transferencia;

@Repository
//  Repositorio para gestionar las transferencias de equipos entre ambientes (RF-TR-01).
//  Proporciona metodos para consultar transferencias por equipo, ambiente de origen y ambiente de destino.
public interface TransferenciaRepository extends JpaRepository<Transferencia, Long> {

    List<Transferencia> findByEquipoId(Long equipoId);

    List<Transferencia> findByAmbienteOrigenId(Long ambienteId);

    List<Transferencia> findByAmbienteDestinoId(Long ambienteId);
}
