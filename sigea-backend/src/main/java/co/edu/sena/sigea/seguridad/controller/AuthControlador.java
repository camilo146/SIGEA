package co.edu.sena.sigea.seguridad.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.seguridad.dto.LoginDTO;
import co.edu.sena.sigea.seguridad.dto.LoginRespuestaDTO;
import co.edu.sena.sigea.seguridad.dto.RegistroDTO;
import co.edu.sena.sigea.seguridad.dto.VerificarCodigoDTO;
import co.edu.sena.sigea.seguridad.service.AutenticacionServicio;
import co.edu.sena.sigea.seguridad.service.VerificacionEmailServicio;
import jakarta.validation.Valid;


@RestController// esta clase maneja peticiones HTTP y devuelve respuestas HTTP
@RequestMapping("/auth") //prefijo  todos loa endpoints empiezan con /auth

public class AuthControlador{

    //dependecia del servicio de autenticacio
    /*El  controlador no tiene logica solo recibe la peticion 
    HTTP y le pasa al servicio y devuelve la respuesta */

    private final AutenticacionServicio autenticacionServicio;
    private final VerificacionEmailServicio verificacionEmailServicio;

    public AuthControlador(AutenticacionServicio autenticacionServicio,
                           VerificacionEmailServicio verificacionEmailServicio) {
        this.autenticacionServicio = autenticacionServicio;
        this.verificacionEmailServicio = verificacionEmailServicio;
    }

    //endpoint 
    /*
    inicia sesion y devuleve un token JWT 
    funciona as:
    El usuario envia un JSON con coreo y contraseña 
    Spriing lo conviierte a un LoginDTO y lo pasa al servicio de autenticacion
    @valid ejecuta las validaciones (@NotBlank, @Email) definidas en LoginDTO
    si falla ManejadorGlobalExcepciones y genera JWT
    se llama al servicio que verifica credenciales y genera JWT
    se devuelve 200 OK con el token 
    */

    @PostMapping("/login")
    public ResponseEntity<LoginRespuestaDTO> login(@Valid @RequestBody LoginDTO loginDTO){

        //Delega la logica a servicio de autenticacion 
        LoginRespuestaDTO respuesta = autenticacionServicio.login(loginDTO);

        //Rtorna 200 oK con el token y datos del usuario
        return ResponseEntity.ok(respuesta);
    }

    //endpoint para registrar un nuevo usuario
    /*funciona asi:
    El  admin envia  un JSON con los datos del nuevo usuario
    Spriing los convierte a registroDTO
    @valid ejecuta las validaciones definidas en RegistroDTO
    El servicio veriifica duplicados, encripta contraseña y guarda 
    se devuelve 201 Created si todo sale bien o errores de validacion si falla
    */
    @PostMapping("/registro")
         public ResponseEntity<Void> registro(
            @Valid @RequestBody RegistroDTO registroDTO) {

        // Delegar toda la lógica al servicio
        autenticacionServicio.registrar(registroDTO);

        // Retornar 201 CREATED sin cuerpo
        // HttpStatus.CREATED = código 201 (recurso creado exitosamente)
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Verificación de email por código de 6 dígitos enviado al correo.
     * POST /api/v1/auth/verificar-email con body { "correo": "...", "codigo": "123456" }
     */
    @PostMapping("/verificar-email")
    public ResponseEntity<java.util.Map<String, String>> verificarEmailPorCodigo(
            @Valid @RequestBody VerificarCodigoDTO dto) {
        String mensaje = verificacionEmailServicio.verificarCodigo(dto.getCorreo(), dto.getCodigo());
        return ResponseEntity.ok(java.util.Map.of("mensaje", mensaje));
    }
}