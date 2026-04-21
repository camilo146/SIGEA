package co.edu.sena.sigea.usuario.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.EstadoAprobacion;
import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

        Optional<Usuario> findByCorreoElectronico(String correoElectronico);

        default Optional<Usuario> findByIdentificador(String identificador) {
                if (identificador == null || identificador.isBlank()) {
                        return Optional.empty();
                }
                String valor = identificador.trim();
                return findByCorreoElectronico(valor).or(() -> findByNumeroDocumento(valor));
        }

        Optional<Usuario> findByTipoDocumentoAndNumeroDocumento(
                        co.edu.sena.sigea.common.enums.TipoDocumento tipoDocumento,
                        String numeroDocumento);

        boolean existsByCorreoElectronico(String correoElectronico);

        boolean existsByNumeroDocumento(String numeroDocumento);

        Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

        List<Usuario> findByRolAndActivoTrue(Rol rol);

        List<Usuario> findByActivoTrue();

        Optional<Usuario> findFirstByEsSuperAdminTrueAndActivoTrue();

        Optional<Usuario> findByTokenVerificacion(String tokenVerificacion);

        List<Usuario> findByEstadoAprobacion(EstadoAprobacion estadoAprobacion);

        /**
         * Para la tarea programada: usuarios PENDIENTE cuya fechaCreacion <= limite.
         */
        List<Usuario> findByEstadoAprobacionAndFechaCreacionBefore(
                        EstadoAprobacion estadoAprobacion, LocalDateTime limite);
}