package co.edu.sena.sigea.prestamo.controller;

// =============================================================================
// CONTROLADOR: PrestamoControlador
// =============================================================================
// Expone los endpoints REST del módulo de préstamos.
// Recibe peticiones HTTP → delega al PrestamoServicio → retorna la respuesta.
// NO contiene lógica de negocio: eso es responsabilidad del servicio.
//
// BASE URL: /api/v1/prestamos
//   (el prefijo /api/v1 viene de server.servlet.context-path en application.properties)
//
// CICLO DE VIDA DEL PRÉSTAMO (endpoints en orden):
//   1. POST /prestamos              → usuario solicita
//   2. PATCH /{id}/aprobar          → admin aprueba
//   2. PATCH /{id}/rechazar         → admin rechaza (alternativa al paso 2)
//   3. PATCH /{id}/registrar-salida → admin confirma entrega física + descuenta stock
//   4. PATCH /{id}/registrar-devolucion → admin registra devolución + repone stock
//
// SEGURIDAD:
//   - POST /prestamos → cualquier usuario autenticado puede solicitar
//   - GET /prestamos → solo ADMINISTRADOR (ver todos)
//   - GET /prestamos/mis-prestamos → cualquier autenticado (solo ve los suyos)
//   - PATCH de administración → solo ADMINISTRADOR
//
// ¿QUÉ ES @AuthenticationPrincipal?
//   Es una anotación de Spring Security que inyecta el usuario autenticado
//   directamente como parámetro del método.
//   Spring Security guarda el usuario en el SecurityContext (memoria).
//   @AuthenticationPrincipal lo extrae de ahí y lo pasa al método.
//
//   UserDetails ud → ud.getUsername() devuelve el CORREO ELECTRÓNICO.
//   ¿Por qué el correo? Porque en UsuarioDetallesServicio, el método
//   loadUserByUsername(correo) usó el correo como "username".
// =============================================================================

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.common.enums.EstadoPrestamo;
import co.edu.sena.sigea.prestamo.dto.PrestamoCrearDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoDevolucionDTO;
import co.edu.sena.sigea.prestamo.dto.PrestamoRespuestaDTO;
import co.edu.sena.sigea.prestamo.service.PrestamoServicio;
import jakarta.validation.Valid;

@RestController
// @RestController = @Controller + @ResponseBody
// Indica que esta clase maneja peticiones HTTP y que los métodos
// retornan datos (JSON) directamente, no vistas HTML.

@RequestMapping("/prestamos")
// Todas las rutas de este controlador empiezan con /prestamos.
// Ruta completa: /api/v1/prestamos (porque /api/v1 está en application.properties).
public class PrestamoControlador {

    // Inyección por constructor (forma recomendada).
    private final PrestamoServicio prestamoServicio;

    public PrestamoControlador(PrestamoServicio prestamoServicio) {
        this.prestamoServicio = prestamoServicio;
    }

    // =========================================================================
    // ENDPOINT 1: POST /prestamos → Solicitar préstamo
    // =========================================================================
    // Cualquier usuario autenticado puede crear una solicitud de préstamo.
    //
    // @RequestBody → Spring convierte el JSON del body a PrestamoCrearDTO.
    // @Valid → activa las validaciones del DTO (@NotEmpty, @Future, etc.).
    // @AuthenticationPrincipal → Spring inyecta el usuario del token JWT.
    //
    // Respuesta HTTP 201 Created → significa "recurso creado exitosamente".
    // Se diferencia del 200 OK: 201 indica que se creó algo nuevo.
    // =========================================================================
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    // isAuthenticated() → cualquier usuario con sesión válida (con token JWT)
    public ResponseEntity<PrestamoRespuestaDTO> solicitar(
            @Valid @RequestBody PrestamoCrearDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        // userDetails.getUsername() retorna el correo del usuario autenticado.
        // El servicio usa ese correo para buscar al usuario en la BD.
        String correo = userDetails.getUsername();
        PrestamoRespuestaDTO respuesta = prestamoServicio.solicitar(dto, correo);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    // =========================================================================
    // ENDPOINT 2: GET /prestamos → Listar préstamos (admin: todos; otro: los suyos)
    // =========================================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoRespuestaDTO>> listarTodos(
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin = userDetails != null && userDetails.getAuthorities() != null
                && userDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a) || "ROLE_INSTRUCTOR".equals(a));
        if (isAdmin) {
            return ResponseEntity.ok(prestamoServicio.listarTodos());
        }
        String correo = userDetails != null ? userDetails.getUsername() : null;
        return ResponseEntity.ok(prestamoServicio.listarMisPrestamos(correo));
    }

    // =========================================================================
    // ENDPOINT 3: GET /prestamos/mis-prestamos → Listar mis préstamos
    // =========================================================================
    // Cualquier usuario autenticado ve ÚNICAMENTE sus propios préstamos.
    // El servicio usa el correo del token para filtrar por usuario.
    //
    // ¿Por qué no /prestamos/{usuarioId} para esto?
    //   Porque no debes confiar en que el usuario envíe su propio ID.
    //   Alguien podría enviar el ID de otra persona y ver sus préstamos.
    //   Al usar el correo del token, el servidor determina quién eres, no tú.
    // =========================================================================
    @GetMapping("/mis-prestamos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoRespuestaDTO>> listarMisPrestamos(
            @AuthenticationPrincipal UserDetails userDetails) {

        String correo = userDetails.getUsername();
        return ResponseEntity.ok(prestamoServicio.listarMisPrestamos(correo));
    }

    // =========================================================================
    // ENDPOINT 4: GET /prestamos/estado/{estado} → Filtrar por estado
    // =========================================================================
    // El admin puede filtrar: SOLICITADO, APROBADO, ACTIVO, DEVUELTO, etc.
    //
    // @PathVariable → extrae el valor de {estado} de la URL.
    //   URL: GET /prestamos/estado/ACTIVO → estado = EstadoPrestamo.ACTIVO
    //
    // EstadoPrestamo es un enum. Spring lo convierte automáticamente del String "ACTIVO"
    // al valor del enum EstadoPrestamo.ACTIVO gracias a los convertidores por defecto.
    // =========================================================================
    @GetMapping("/estado/{estado}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoRespuestaDTO>> listarPorEstado(
            @PathVariable EstadoPrestamo estado) {

        return ResponseEntity.ok(prestamoServicio.listarPorEstado(estado));
    }

    // =========================================================================
    // ENDPOINT 5: GET /prestamos/usuario/{usuarioId} → Préstamos de un usuario
    // =========================================================================
    // El admin puede ver todos los préstamos de un usuario específico.
    // Útil para auditar: "¿Cuántos equipos ha pedido Juan?", "¿Los devolvió?"
    // =========================================================================
    @GetMapping("/usuario/{usuarioId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PrestamoRespuestaDTO>> listarPorUsuario(
            @PathVariable Long usuarioId) {

        return ResponseEntity.ok(prestamoServicio.listarPorUsuario(usuarioId));
    }

    // =========================================================================
    // ENDPOINT 6: GET /prestamos/{id} → Buscar préstamo por ID
    // =========================================================================
    // El admin busca un préstamo específico por su ID para ver todos sus detalles.
    // Retorna 404 si el préstamo no existe (lo lanza el servicio).
    // =========================================================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PrestamoRespuestaDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(prestamoServicio.buscarPorId(id));
    }

    // =========================================================================
    // ENDPOINT 7: PATCH /prestamos/{id}/aprobar → Aprobar solicitud
    // =========================================================================
    // Solo el admin puede aprobar una solicitud en estado SOLICITADO.
    // El servicio cambia el estado a APROBADO y registra quién aprobó y cuándo.
    //
    // ¿Por qué PATCH y no PUT?
    //   PUT = reemplaza el recurso COMPLETO (como hacer un UPDATE de todos los campos)
    //   PATCH = modifica PARCIALMENTE el recurso (solo el campo "estado" cambia)
    //   En este caso solo cambiamos el estado → PATCH es correcto.
    // =========================================================================
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<PrestamoRespuestaDTO> aprobar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correoAdmin = userDetails.getUsername();
        return ResponseEntity.ok(prestamoServicio.aprobar(id, correoAdmin));
    }

    // =========================================================================
    // ENDPOINT 8: PATCH /prestamos/{id}/rechazar → Rechazar solicitud
    // =========================================================================
    // Solo el admin puede rechazar una solicitud SOLICITADA.
    // El estado pasa a RECHAZADO y no hay impacto en stock.
    // =========================================================================
    @PatchMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<PrestamoRespuestaDTO> rechazar(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correoAdmin = userDetails.getUsername();
        return ResponseEntity.ok(prestamoServicio.rechazar(id, correoAdmin));
    }

    // =========================================================================
    // ENDPOINT 9: PATCH /prestamos/{id}/registrar-salida → Confirmar entrega física
    // =========================================================================
    // El admin confirma que los equipos físicamente salieron del almacén.
    // EN ESTE MOMENTO se descuenta el stock (cantidadDisponible -= cantidad).
    // El préstamo pasa de APROBADO → ACTIVO.
    // =========================================================================
    @PatchMapping("/{id}/registrar-salida")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<PrestamoRespuestaDTO> registrarSalida(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correoAdmin = userDetails.getUsername();
        return ResponseEntity.ok(prestamoServicio.registrarSalida(id, correoAdmin));
    }

    // =========================================================================
    // ENDPOINT 10: PATCH /prestamos/{id}/registrar-devolucion → Registrar retorno
    // =========================================================================
    // El admin confirma que los equipos fueron físicamente devueltos.
    // Se repone el stock (cantidadDisponible += cantidad).
    // Cuando TODOS los detalles están devueltos, el préstamo pasa a DEVUELTO.
    // =========================================================================
    @PatchMapping("/{id}/registrar-devolucion")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<PrestamoRespuestaDTO> registrarDevolucion(
            @PathVariable Long id,
            @Valid @RequestBody PrestamoDevolucionDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {

        String correoAdmin = userDetails.getUsername();
        return ResponseEntity.ok(prestamoServicio.registrarDevolucion(id, dto, correoAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        prestamoServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
