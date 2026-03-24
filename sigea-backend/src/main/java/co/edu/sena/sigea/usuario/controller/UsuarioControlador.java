package co.edu.sena.sigea.usuario.controller;

// =============================================================================
// CONTROLADOR: UsuarioControlador
// =============================================================================
// Define todos los endpoints REST para la gestión de usuarios.
//
// BASE URL: /api/v1/usuarios (el /api/v1 viene del application.properties)
//
// ENDPOINTS:
//   POST   /usuarios                    → Crear usuario (admin)
//   GET    /usuarios                    → Listar usuarios activos (admin)
//   GET    /usuarios/todos              → Listar todos incluidos inactivos (admin)
//   GET    /usuarios/rol/{rol}          → Listar por rol (admin)
//   GET    /usuarios/{id}               → Buscar por ID (admin)
//   GET    /usuarios/perfil             → Obtener perfil propio (cualquier usuario)
//   PUT    /usuarios/{id}               → Actualizar usuario (admin)
//   PATCH  /usuarios/cambiar-contrasena → Cambiar contraseña propia (cualquier usuario)
//   PATCH  /usuarios/{id}/rol           → Cambiar rol (admin)
//   PATCH  /usuarios/{id}/activar       → Reactivar usuario (admin)
//   PATCH  /usuarios/{id}/desactivar    → Desactivar usuario (admin)
//   PATCH  /usuarios/{id}/desbloquear   → Desbloquear cuenta (admin)
//
// SEGURIDAD:
//   @PreAuthorize("hasRole('ADMINISTRADOR')") → Solo admins pueden acceder.
//   Spring Security verifica el token JWT, extrae el rol, y si no es
//   ADMINISTRADOR retorna 403 Forbidden automáticamente.
//
//   Para los endpoints del usuario propio (perfil, cambiar contraseña),
//   NO se usa @PreAuthorize → cualquier usuario autenticado puede usarlos.
//   Se usa SecurityContextHolder para obtener el correo del token JWT.
//
// ¿POR QUÉ PATCH Y NO PUT PARA ALGUNAS OPERACIONES?
//   PUT = Reemplazar TODO el recurso (actualizar todos los campos)
//   PATCH = Modificar PARTE del recurso (solo un campo o aspecto)
//   Cambiar rol, activar, desactivar → solo modifican UN campo → PATCH
//   Actualizar información → modifica VARIOS campos → PUT
// =============================================================================

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.sena.sigea.common.enums.Rol;
import co.edu.sena.sigea.usuario.dto.UsuarioActualizadoDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCambiarContrasenaDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCambiarRolDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioCrearDTO;
import co.edu.sena.sigea.usuario.dto.UsuarioRespuestaDTO;
import co.edu.sena.sigea.usuario.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/usuarios")
public class UsuarioControlador {

    private final UsuarioService usuarioServicio;

    // Constructor: Spring inyecta el servicio automáticamente
    public UsuarioControlador(UsuarioService usuarioServicio) {
        this.usuarioServicio = usuarioServicio;
    }

    // =========================================================================
    // POST /api/v1/usuarios → Crear usuario (solo admin)
    // =========================================================================
    // @PreAuthorize: Spring verifica que el usuario tenga ROLE_ADMINISTRADOR.
    // Internamente mira el SecurityContext → el JwtFiltroAutenticacion
    // puso ahí el rol extraído del token JWT.
    // Si no es admin → 403 Forbidden automático.
    //
    // @Valid: Activa las validaciones de UsuarioCrearDTO (@NotBlank, @Email, etc.)
    // Si alguna falla → ManejadorGlobalExcepciones captura y retorna 400.
    //
    // @RequestBody: Convierte el JSON del body de la petición al DTO.
    //
    // Retorna 201 Created con el UsuarioRespuestaDTO en el body.
    // =========================================================================
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioRespuestaDTO> crear(
            @Valid @RequestBody UsuarioCrearDTO dto) {

        UsuarioRespuestaDTO respuesta = usuarioServicio.crear(dto);
        return new ResponseEntity<>(respuesta, HttpStatus.CREATED);
    }

    // =========================================================================
    // GET /api/v1/usuarios → Listar usuarios activos (solo admin)
    // =========================================================================
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioRespuestaDTO>> listarActivos() {

        List<UsuarioRespuestaDTO> usuarios = usuarioServicio.listarActivos();
        return ResponseEntity.ok(usuarios);
    }

    // =========================================================================
    // GET /api/v1/usuarios/todos → Listar TODOS (autenticado:
    // formularios/dropdowns)
    // =========================================================================
    @GetMapping("/todos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioRespuestaDTO>> listarTodos() {

        List<UsuarioRespuestaDTO> usuarios = usuarioServicio.listarTodos();
        return ResponseEntity.ok(usuarios);
    }

    // =========================================================================
    // GET /api/v1/usuarios/rol/{rol} → Listar por rol (autenticado)
    // =========================================================================
    @GetMapping("/rol/{rol}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<UsuarioRespuestaDTO>> listarPorRol(
            @PathVariable Rol rol) {

        List<UsuarioRespuestaDTO> usuarios = usuarioServicio.listarPorRol(rol);
        return ResponseEntity.ok(usuarios);
    }

    // =========================================================================
    // GET /api/v1/usuarios/{id} → Buscar por ID (autenticado)
    // =========================================================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioRespuestaDTO> buscarPorId(
            @PathVariable Long id) {

        UsuarioRespuestaDTO usuario = usuarioServicio.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    // =========================================================================
    // GET /api/v1/usuarios/perfil → Obtener perfil propio (cualquier usuario)
    // =========================================================================
    // Este endpoint NO tiene @PreAuthorize → cualquier usuario autenticado
    // puede ver SU PROPIO perfil.
    //
    // ¿Cómo sabe quién es el usuario?
    // SecurityContextHolder guarda la información de autenticación.
    // El JwtFiltroAutenticacion puso ahí el correo del usuario al validar el JWT.
    // Authentication.getName() retorna el "username" = correo electrónico.
    // =========================================================================
    @GetMapping("/perfil")
    public ResponseEntity<UsuarioRespuestaDTO> obtenerPerfil() {

        // Obtener el correo del usuario autenticado desde el SecurityContext
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String correo = autenticacion.getName();

        UsuarioRespuestaDTO usuario = usuarioServicio.obtenerPerfil(correo);
        return ResponseEntity.ok(usuario);
    }

    // =========================================================================
    // PUT /api/v1/usuarios/{id} → Actualizar usuario (solo admin)
    // =========================================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioRespuestaDTO> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioActualizadoDTO dto) {

        UsuarioRespuestaDTO respuesta = usuarioServicio.actualizar(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/cambiar-contrasena → Cambiar contraseña propia
    // =========================================================================
    // Sin @PreAuthorize → cualquier usuario autenticado puede cambiar
    // SU PROPIA contraseña.
    //
    // Retorna 204 No Content (no hay nada útil que retornar).
    // =========================================================================
    @PatchMapping("/cambiar-contrasena")
    public ResponseEntity<Void> cambiarContrasena(
            @Valid @RequestBody UsuarioCambiarContrasenaDTO dto) {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String correo = autenticacion.getName();

        usuarioServicio.cambiarContrasena(correo, dto);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/{id}/rol → Cambiar rol (solo admin)
    // =========================================================================
    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioRespuestaDTO> cambiarRol(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioCambiarRolDTO dto) {

        UsuarioRespuestaDTO respuesta = usuarioServicio.cambiarRol(id, dto);
        return ResponseEntity.ok(respuesta);
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/{id}/activar → Reactivar usuario (solo admin)
    // =========================================================================
    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> activar(@PathVariable Long id) {

        usuarioServicio.activar(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/{id}/desactivar → Desactivar usuario (solo admin)
    // =========================================================================
    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {

        usuarioServicio.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/{id}/desbloquear → Desbloquear cuenta (solo admin)
    // =========================================================================
    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> desbloquearCuenta(@PathVariable Long id) {

        usuarioServicio.desbloquearCuenta(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // GET /api/v1/usuarios/pendientes → Usuarios pendientes de aprobación (admin)
    // =========================================================================
    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioRespuestaDTO>> listarPendientes() {
        return ResponseEntity.ok(usuarioServicio.listarPendientes());
    }

    // =========================================================================
    // PATCH /api/v1/usuarios/{id}/aprobar → Aprobar usuario pendiente (admin)
    // =========================================================================
    @PatchMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioRespuestaDTO> aprobar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioServicio.aprobar(id));
    }

    // =========================================================================
    // DELETE /api/v1/usuarios/{id}/rechazar → Rechazar y eliminar usuario pendiente
    // (admin)
    // =========================================================================
    @DeleteMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> rechazar(@PathVariable Long id) {
        usuarioServicio.rechazar(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // DELETE /api/v1/usuarios/{id} → Eliminar usuario (solo admin, eliminación
    // lógica)
    // =========================================================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        usuarioServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}