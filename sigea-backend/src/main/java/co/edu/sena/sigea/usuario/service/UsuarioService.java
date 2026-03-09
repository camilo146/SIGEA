package co.edu.sena.sigea.usuario.service;


//Contiene la logica de gestion de usuarios 
/*
Crear Usuarios desde el panel del admin
Listar usuarios (activos, todos, por rol)
Buscar por ID
Obteneter un perfil propio (desde el token JWT )
Actualizar informacion 
Cambiar contraseña propia 
Cambiar rol (solo admin)
Activar/Desactivar usuarios 
Desbloquear cuentas por intentos fallidos 
*/

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.common.exception.OperacionNoPermitidaException;
import co.edu.sena.sigea.common.exception.RecursoDuplicadoException;
import co.edu.sena.sigea.common.exception.RecursoNoEncontradoException;
import co.edu.sena.sigea.usuario.dto.UsuarioActualizadoDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCambiarContrasenaDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCambiarRolDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCrearDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioRespuestaDTO;
import co.edu.sena.sigea.usuario.entity.Usuario;
import co.edu.sena.sigea.usuario.repository.UsuarioRepository;



@Service
public class UsuarioService {

    //Dependecias inyectadas por el constructor
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder  passwordEncoder;

    // Constructor para inyectar dependencias (inyección por constructor - patrón recomendado)
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

   //Metoddo para crear un nuevo usuario (solo admin)
   /*
   Fujo:
   verificar que el documento no este duplicado 
   verificar que el correo no este duplicado
   Encriptar  contraseña con BCrypt
    Guardar el nuevo usuario en la base de datos
    Construir y guiardar la entidad Usuario
    Retornar un UsuarioRespuestaDTO sin contraseña 
   */

   @Transactional
   public UsuarioRespuestaDTO crear(UsuarioCrearDTO dto){

    //verificar documento duplicado 
    if (usuarioRepository.existsByNumeroDocumento(dto.getNumeroDocumento())){
        throw new RecursoDuplicadoException(
            "El numero de documento ya esta registrado: " + dto.getNumeroDocumento()
        );
    }

    // Verificar correo duplicado
    if (dto.getCorreoElectronico() != null
            && usuarioRepository.existsByCorreoElectronico(dto.getCorreoElectronico())) {
        throw new RecursoDuplicadoException(
                "El correo electronico ya esta registrado: " + dto.getCorreoElectronico());
    }

    Usuario usuario = Usuario.builder()
            .nombreCompleto(dto.getNombreCompleto())
            .tipoDocumento(dto.getTipoDocumento())
            .numeroDocumento(dto.getNumeroDocumento())
            .correoElectronico(dto.getCorreoElectronico())
            .programaFormacion(dto.getProgramaFormacion())
            .telefono(dto.getTelefono())
            .ficha(dto.getNumeroFicha())
            .contrasenaHash(passwordEncoder.encode(dto.getContrasena()))
            .rol(dto.getRol())
            .esSuperAdmin(false)
            .activo(true)
            .intentosFallidos(0)
            .build();

    Usuario guardado = usuarioRepository.save(usuario);
    return convertirADTO(guardado);
}


   //Metodo para listar usuarios actiivos 

    //Retorna todoss los usuarios con activo = true
    @Transactional(readOnly = true)
    public List<UsuarioRespuestaDTO> listarActivos(){
        return usuarioRepository.findByActivoTrue().stream()
                .map(this::convertirADTO)
                .toList();
    }

    //Metodo para listar TODOS los usuarios (incluye inactivos, solo admin)
    @Transactional(readOnly = true)
    public List<UsuarioRespuestaDTO> listarTodos(){
        return usuarioRepository.findAll().stream()
                .map(this::convertirADTO)
                .toList();
    }

    //Metodo para listar usuarios por rol 

    @Transactional(readOnly = true)
    public List<UsuarioRespuestaDTO> listarPorRol(Rol rol){
        return usuarioRepository.findByRolAndActivoTrue(rol).stream()
                .map(this::convertirADTO)
                .toList();
    }   
    

    //Metodo para buscar un usuario por ID 
    @Transactional(readOnly = true)
    public UsuarioRespuestaDTO buscarPorId(Long id){
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Usuario no encontrado con ID: " + id
                ));
        return convertirADTO(usuario);
    }

    //Metodo obtener perfil propio 
    //el usuario autenticado obtiene su propia informacion 
    @Transactional(readOnly = true)
    public UsuarioRespuestaDTO obtenerPerfil(String correoElectronico){
        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "Usuario no encontrado con correo: " + correoElectronico
                ));
        return convertirADTO(usuario);
    }


    //Metodo para actualiza usuarios (solo actualiza los datos basicos del usuario
    /* 
    Buscar el usuario por ID
    verificar que el  num de documento no este duplicado 
    verificar que el coreo no este dupliicado 
    Actualizar los campos 
    Guardar los cambios en la base de datos
    */

    @Transactional 
    public UsuarioRespuestaDTO actualizar (long  id, UsuarioActualizadoDTO dto ){

        //Buscar usuario existente 
        Usuario usuario = usuarioRepository.findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException(
            "Usuario no encontrado con ID: " + id
        ));

        //verificar documento duplicado
        usuarioRepository.findByNumeroDocumento(dto.getNumeroDocumento())
        .ifPresent(otroUsuario -> {
            if (!otroUsuario.getId().equals(id)){
                throw new RecursoDuplicadoException(
                    "El numero de documento ya esta registrado: " + dto.getNumeroDocumento()
                );
            }
        });


        // Verificar correo duplicado (si cambió)
        if (dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
            usuarioRepository.findByCorreoElectronico(dto.getCorreoElectronico())
                    .ifPresent(existente -> {
                        if (!existente.getId().equals(id)) {
                            throw new RecursoDuplicadoException(
                                    "Ya existe otro usuario con el correo: " + dto.getCorreoElectronico());
                        }
                    });
        }

        //Actualizar campos 
        usuario.setNombreCompleto(dto.getNombreCompleto());
        usuario.setTipoDocumento(dto.getTipoDocumento());
        usuario.setNumeroDocumento(dto.getNumeroDocumento());
        usuario.setCorreoElectronico(dto.getCorreoElectronico());
        usuario.setTelefono(dto.getNumeroTelefono());
        usuario.setProgramaFormacion(dto.getProgramaFormacion());
        usuario.setFicha(dto.getNumeroFicha());

        Usuario actualizado = usuarioRepository.save(usuario);


        return convertirADTO(actualizado);
    }

    // Metodo para cambiar la contraseña propia
    // Flujo: buscar por correo → verificar actual → encriptar nueva → guardar
    @Transactional
    public void cambiarContrasena(String correoElectronico,
                                  UsuarioCambiarContrasenaDTO dto) {

        Usuario usuario = usuarioRepository.findByCorreoElectronico(correoElectronico)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con correo: " + correoElectronico));

        if (!passwordEncoder.matches(dto.getContrasenaActual(), usuario.getContrasenaHash())) {
            throw new OperacionNoPermitidaException(
                    "La contraseña actual es incorrecta");
        }

        usuario.setContrasenaHash(passwordEncoder.encode(dto.getNuevaContrasena()));
        usuarioRepository.save(usuario);
    }

    // Metodo para cambiar el rol de un usuario (solo admin)
    @Transactional
    public UsuarioRespuestaDTO cambiarRol(long id, UsuarioCambiarRolDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id));

        if (Boolean.TRUE.equals(usuario.getEsSuperAdmin())) {
            throw new OperacionNoPermitidaException(
                    "No se puede cambiar el rol de un super admin");
        }

        usuario.setRol(dto.getNuevoRol());
        Usuario actualizado = usuarioRepository.save(usuario);
        return convertirADTO(actualizado);
    }

    // Metodo para desactivar usuario
    @Transactional
    public void desactivar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id));

        if (Boolean.TRUE.equals(usuario.getEsSuperAdmin())) {
            throw new OperacionNoPermitidaException(
                    "No se puede desactivar un super admin");
        }

        if (!usuario.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "El usuario ya esta desactivado");
        }

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    // Metodo para activar usuario
    @Transactional
    public void activar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id));

        if (Boolean.TRUE.equals(usuario.getEsSuperAdmin())) {
            throw new OperacionNoPermitidaException(
                    "No se puede activar un super admin");
        }

        if (usuario.getActivo()) {
            throw new OperacionNoPermitidaException(
                    "El usuario ya esta activado");
        }

        usuario.setActivo(true);
        usuarioRepository.save(usuario);
    }

    // Metodo para desbloquear cuenta (tras intentos fallidos de login)
    @Transactional
    public void desbloquearCuenta(long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario no encontrado con ID: " + id));

        if (usuario.getCuentaBloqueadaHasta() == null) {
            throw new OperacionNoPermitidaException(
                    "La cuenta del usuario no esta bloqueada");
        }

        usuario.setIntentosFallidos(0);
        usuario.setCuentaBloqueadaHasta(null);
        usuarioRepository.save(usuario);
    }


    //Metodo para convertir entidad a DTO (sin contraseña)
    private UsuarioRespuestaDTO convertirADTO(Usuario usuario){
        return UsuarioRespuestaDTO.builder()
                .id(usuario.getId())
                .nombreCompleto(usuario.getNombreCompleto())
                .tipoDocumento(usuario.getTipoDocumento().name())
                .numeroDocumento(usuario.getNumeroDocumento())
                .correoElectronico(usuario.getCorreoElectronico())
                .telefono(usuario.getTelefono())
                .programaFormacion(usuario.getProgramaFormacion()) 
                .ficha(usuario.getFicha())
                .rol(usuario.getRol().name())
                .esSuperAdmin(usuario.getEsSuperAdmin())
                .activo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }

    
}
