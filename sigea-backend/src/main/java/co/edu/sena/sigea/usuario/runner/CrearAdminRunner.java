package co.edu.sena.sigea.usuario.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.enums.TipoDocumento;
import co.edu.sena.sigea.usuario.dto.UsuarioCrearDTO;
import co.edu.sena.sigea.usuario.service.UsuarioService;

/**
 * Crea un usuario administrador por defecto (999999999 / password)
 * cuando se arranca la app con el perfil "crear-admin".
 * Uso: mvn spring-boot:run
 * "-Dspring-boot.run.arguments=--spring.profiles.active=crear-admin"
 */
@Component
@Profile("crear-admin")
@Order(Integer.MIN_VALUE)
public class CrearAdminRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CrearAdminRunner.class);
    private static final String CORREO_ADMIN = "admin2@sigea.local";
    private static final String PASSWORD_TEMPORAL = "password";

    private final UsuarioService usuarioService;

    public CrearAdminRunner(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void run(String... args) {
        UsuarioCrearDTO dto = new UsuarioCrearDTO();
        dto.setNombreCompleto("Administrador SIGEA");
        dto.setTipoDocumento(TipoDocumento.CC);
        dto.setNumeroDocumento("999999999");
        dto.setCorreoElectronico(CORREO_ADMIN);
        dto.setProgramaFormacion(null);
        dto.setNumeroFicha(null);
        dto.setTelefono(null);
        dto.setContrasena(PASSWORD_TEMPORAL);
        dto.setRol(Rol.ADMINISTRADOR);

        try {
            usuarioService.crear(dto);
            log.info("[SIGEA] Usuario administrador creado: {}", CORREO_ADMIN);
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("correo") || e.getMessage().contains("documento"))) {
                log.info("[SIGEA] El usuario {} ya existe. Nada que hacer.", CORREO_ADMIN);
            } else {
                throw e;
            }
        }
        System.exit(0);
    }
}
