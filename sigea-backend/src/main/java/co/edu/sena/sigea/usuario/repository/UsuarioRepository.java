package co.edu.sena.sigea.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.usuario.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoElectronico(String correoElectronico);

    Optional<Usuario> findByTipoDocumentoAndNumeroDocumento(
            co.edu.sena.sigea.common.enums.TipoDocumento tipoDocumento,
            String numeroDocumento
    );

    boolean existsByCorreoElectronico(String correoElectronico);

    boolean existsByNumeroDocumento(String numeroDocumento);

    Optional<Usuario> findByNumeroDocumento(String numeroDocumento);

    List<Usuario> findByRolAndActivoTrue(Rol rol);

    List<Usuario> findByActivoTrue();
}