# SIGEA – Contexto de Desarrollo (Archivo para Copilot)

> **¿Para qué sirve este archivo?**  
> Si pierdes la conversación con Copilot, abre este archivo y dile:  
> *"Lee el archivo SIGEA_Contexto_Desarrollo.md y continúa donde quedamos"*

---

## Datos del Proyecto

- **Proyecto:** SIGEA – Sistema Integral de Gestión de Equipos y Activos
- **Desarrollador:** Camilo López Romero (aprendiz SENA)
- **Institución:** SENA – Centro Industrial de Mantenimiento Integral (CIMI)
- **Stack:** Java 21 + Spring Boot 3.5.10 + MariaDB 12.2 + Angular 17+ (frontend aún no iniciado)

## Metodología de Trabajo

- Copilot da el código y Camilo lo escribe manualmente.
- Copilot explica CADA línea de código como un instructor/profesor.
- Se trabaja módulo por módulo, archivo por archivo.

## Documentos de Referencia

| Documento | Descripción |
|---|---|
| `SIGEA_Documento_Requerimientos.md` | Requerimientos funcionales y no funcionales completos |
| `SIGEA_Diagramas_Diseno.md` | Diagramas ER, modelo relacional, clases Java, enums, principios SOLID aplicados |

---

## Estado Actual del Backend

### ✅ COMPLETADO

#### Capa de Persistencia (100%)
- [x] `pom.xml` – Todas las dependencias configuradas
- [x] `application.properties` – Configuración completa (BD, JWT, SMTP, Swagger, etc.)
- [x] `V1__crear_tablas.sql` – Migración Flyway con 15 tablas + datos semilla
- [x] `SigeaBackendApplication.java` – Clase principal
- [x] `EntidadBase.java` – Clase abstracta con id, fechaCreacion, fechaActualizacion
- [x] 14 Entidades JPA completas (Usuario, Categoria, Ambiente, Equipo, FotoEquipo, Prestamo, DetallePrestamo, ExtensionPrestamo, ReporteDano, Reserva, Transferencia, Mantenimiento, Notificacion, LogAuditoria, Configuracion)
- [x] 11 Enums (Rol, TipoDocumento, EstadoEquipo, EstadoPrestamo, EstadoCondicion, EstadoReserva, EstadoExtension, TipoNotificacion, MedioEnvio, EstadoEnvio, TipoMantenimiento)
- [x] 15 Repositories con métodos custom
- [x] `ErrorRespuesta.java` – DTO común de errores

#### Excepciones Globales (100%)
- [x] `RecursoNoEncontradoException.java` – RuntimeException para 404
- [x] `RecursoDuplicadoException.java` – RuntimeException para 409
- [x] `OperacionNoPermitidaException.java` – RuntimeException para 403
- [x] `ManejadorGlobalExcepciones.java` – @RestControllerAdvice con 4 handlers:
  - RecursoNoEncontradoException → 404
  - RecursoDuplicadoException → 409
  - OperacionNoPermitidaException → 403
  - MethodArgumentNotValidException → 400 (validaciones de DTOs)

#### Módulo Categoría (100%) ← PRIMER MÓDULO COMPLETO
- [x] `CategoriaCrearDTO.java` – DTO de entrada con @NotBlank, @Size
- [x] `CategoriaRespuestaDTO.java` – DTO de salida con @Builder
- [x] `CategoriaServicio.java` – Lógica de negocio (crear, listarActivas, listarTodas, buscarPorId, actualizar, eliminar lógico)
- [x] `CategoriaControlador.java` – 6 endpoints REST:
  - POST /api/v1/categorias (201)
  - GET /api/v1/categorias (200, solo activas)
  - GET /api/v1/categorias/todas (200, incluye inactivas)
  - GET /api/v1/categorias/{id} (200)
  - PUT /api/v1/categorias/{id} (200)
  - DELETE /api/v1/categorias/{id} (204, soft delete)

---

#### MariaDB Configurada (100%)
- [x] MariaDB 12.2 instalada en Windows (puerto 3305)
- [x] Base de datos `sigea_db` creada (utf8mb4)
- [x] Usuario `sigea_user` creado con permisos completos
- [x] Plugin de autenticación cambiado a `mysql_native_password` (compatibilidad JDBC)
- [x] Spring Boot arranca correctamente y Flyway crea las 15 tablas
- [x] `V2__corregir_columnas_prestamo.sql` — Migración para corregir nombres de columnas en tabla prestamo
- [x] `PrestamoRepository.java` — Corregido método `findByFechaHoraDevolucionEstimadaBeforeAndEstado`

**Nota:** MariaDB 12.2 usa `sha256_password` por defecto. Se configuró `mysql_native_password=ON` en `my.ini` y se cambió el plugin de los usuarios para compatibilidad con el driver JDBC.

---

#### Módulo Seguridad – JWT (100%) ✅
- [x] `JwtProveedor.java` – Genera tokens JWT, los valida, extrae correo y rol del token (usa jjwt 0.12.6 + HMAC-SHA256)
- [x] `UsuarioDetallesServicio.java` – Implementa UserDetailsService: carga usuario de BD por correo, verifica activo/bloqueado, asigna ROLE_
- [x] `JwtFiltroAutenticacion.java` – OncePerRequestFilter: intercepta cada petición, extrae token del header Authorization, valida y autentica en SecurityContext
- [x] `SecurityConfig.java` – Configuración de Spring Security: rutas públicas/protegidas, CORS, CSRF, BCrypt, SessionPolicy STATELESS
- [x] `LoginDTO.java` – DTO de entrada para login con @NotBlank, @Email
- [x] `LoginRespuestaDTO.java` – DTO de salida con token, tipo, nombreCompleto, rol
- [x] `RegistroDTO.java` – DTO de entrada para registro con validaciones completas
- [x] `AutenticacionServicio.java` – Lógica de login (autenticar, generar JWT, intentos fallidos/bloqueo) y registro (BCrypt, validar duplicados)
- [x] `AuthControlador.java` – Endpoints POST /auth/login y POST /auth/registro (públicos)

---

#### Módulo Usuarios (100%) ✅
- [x] `UsuarioCrearDTO.java` – DTO de entrada para que un admin cree usuario (incluye rol)
- [x] `UsuarioActualizadoDTO.java` – DTO para actualizar datos básicos (sin contraseña ni rol)
- [x] `UsuarioRespuestaDTO.java` – DTO de salida con @Builder (NUNCA expone contraseña)
- [x] `UsuarioCambiarContrasenaDTO.java` – DTO para cambiar contraseña propia (actual + nueva)
- [x] `UsuarioCambiarRolDTO.java` – DTO para cambiar rol de otro usuario (solo admin)
- [x] `UsuarioService.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorRol, buscarPorId, obtenerPerfil, actualizar, cambiarContrasena, cambiarRol, desactivar, activar, desbloquearCuenta
- [x] `UsuarioControlador.java` – 12 endpoints REST:
  - POST /api/v1/usuarios (201, solo admin)
  - GET /api/v1/usuarios (200, activos, solo admin)
  - GET /api/v1/usuarios/todos (200, incluye inactivos, solo admin)
  - GET /api/v1/usuarios/rol/{rol} (200, filtrar por rol, solo admin)
  - GET /api/v1/usuarios/{id} (200, solo admin)
  - GET /api/v1/usuarios/perfil (200, cualquier usuario autenticado)
  - PUT /api/v1/usuarios/{id} (200, solo admin)
  - PATCH /api/v1/usuarios/cambiar-contrasena (204, cualquier usuario autenticado)
  - PATCH /api/v1/usuarios/{id}/rol (200, solo admin)
  - PATCH /api/v1/usuarios/{id}/activar (204, solo admin)
  - PATCH /api/v1/usuarios/{id}/desactivar (204, solo admin)
  - PATCH /api/v1/usuarios/{id}/desbloquear (204, solo admin)
- [x] `UsuarioRepository.java` – Actualizado con método `findByNumeroDocumento`

---

#### Módulo Ambientes (100%) ✅
- [x] `AmbienteCrearDTO.java` – DTO de entrada con nombre, ubicación, descripción, idInstructorResponsable
- [x] `AmbienteRespuestaDTO.java` – DTO de salida con @Builder (incluye nombre del instructor)
- [x] `AmbienteService.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorInstructor, buscarPorId, actualizar, desactivar, activar
- [x] `AmbienteControlador.java` – 8 endpoints REST:
  - POST /api/v1/ambientes (201, solo admin)
  - GET /api/v1/ambientes (200, activos, solo admin)
  - GET /api/v1/ambientes/todos (200, incluye inactivos, solo admin)
  - GET /api/v1/ambientes/instructor/{instructorId} (200, filtrar por instructor, solo admin)
  - GET /api/v1/ambientes/{id} (200, solo admin)
  - PUT /api/v1/ambientes/{id} (200, solo admin)
  - PATCH /api/v1/ambientes/{id}/activar (204, solo admin)
  - PATCH /api/v1/ambientes/{id}/desactivar (204, solo admin)

---

#### Módulo Equipos (100%) ✅
- [x] `FotoEquipoRespuestaDTO.java` – DTO de salida para fotos (id, nombreArchivo, rutaArchivo, tamanoBytes, fechaSubida)
- [x] `EquipoCrearDTO.java` – DTO de entrada con validaciones (@NotBlank, @NotNull, @Min, @Size)
- [x] `EquipoRespuestaDTO.java` – DTO de salida con @Builder (incluye nombre de categoría y ambiente, lista de fotos)
- [x] `EquipoServicio.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorCategoria, listarPorAmbiente, listarPorEstado, buscarPorId, actualizar, cambiarEstado, darDeBaja, activar, subirFoto, eliminarFoto
- [x] `EquipoControlador.java` – 13 endpoints REST:
  - POST /api/v1/equipos (201, solo admin)
  - GET /api/v1/equipos (200, activos, solo admin)
  - GET /api/v1/equipos/todos (200, incluye inactivos, solo admin)
  - GET /api/v1/equipos/categoria/{categoriaId} (200, filtrar por categoría, solo admin)
  - GET /api/v1/equipos/ambiente/{ambienteId} (200, filtrar por ambiente, solo admin)
  - GET /api/v1/equipos/estado/{estado} (200, filtrar por estado, solo admin)
  - GET /api/v1/equipos/{id} (200, solo admin)
  - PUT /api/v1/equipos/{id} (200, solo admin)
  - PATCH /api/v1/equipos/{id}/estado/{nuevoEstado} (200, solo admin)
  - PATCH /api/v1/equipos/{id}/dar-de-baja (204, soft delete, solo admin)
  - PATCH /api/v1/equipos/{id}/activar (204, solo admin)
  - POST /api/v1/equipos/{id}/fotos (201, multipart/form-data, solo admin)
  - DELETE /api/v1/equipos/{id}/fotos/{fotoId} (204, solo admin)

---

#### Correcciones y aprendizajes de esta sesión (25 feb 2026)

- **Fix application.properties:** Se agregó `management.health.mail.enabled=false` para evitar que el `MailHealthIndicator` marque el servidor como DOWN (503) cuando no hay servidor SMTP disponible en desarrollo.
- **Enum TipoDocumento:** Los valores son códigos cortos: `CC, CE, PEP, TI, PP` (NO `CEDULA_CIUDADANIA`).
- **Enum EstadoEquipo:** Los valores son `ACTIVO` y `EN_MANTENIMIENTO` (con prefijo `EN_`).
- **Usuario de prueba creado en BD:**
  - Correo: `admin@sena.edu.co` / Contraseña: `Admin2026!`
  - Rol cambiado a `ADMINISTRADOR` directo en BD con `UPDATE usuario SET rol = 'ADMINISTRADOR'`
- **Datos de prueba en BD:** Categoría id=1 ("Computadores"), Ambiente id=1 ("Laboratorio de Sistemas"), Equipo id=1 ("Laptop Dell Latitude", código "LAP-DELL-001")

---

#### Módulo Préstamos (100%) ✅
- [x] `DetallePrestamoDTO.java` — DTO entrada (equipoId + cantidad)
- [x] `PrestamoCrearDTO.java` — DTO entrada corregido: LocalDateTime en vez de String, sin usuarioSolicitanteId
- [x] `DetallePrestamoRespuestaDTO.java` — DTO salida de detalle con campos completos (id, nombre, código, cantidad, condición, devuelto)
- [x] `PrestamoRespuestaDTO.java` — DTO salida de cabecera con datos enriquecidos
- [x] `PrestamoServicio.java` — Lógica de negocio: solicitar, aprobar, rechazar, registrarSalida, registrarDevolucion, cancelar, listarTodos, listarMisPrestamos, listarPorUsuario, listarPorEstado, buscarPorId
- [x] `PrestamoControlador.java` — 10 endpoints REST

**Notas de la sesión (27 feb 2026):**
- `DetallePrestamoRespuetaDTO.java` (con typo) quedó en disco — **eliminar manualmente**
- El Usuario tiene `nombreCompleto` (no `nombre`+`apellido`)
- `Equipo.activo` es `Boolean` (wrapper) → Lombok genera `getActivo()`, no `isActivo()`
- El usuarioSolicitanteId NO va en el DTO de entrada (viene del token JWT)
- `@Future` solo funciona con tipos de fecha de Java, no con `String`

---

#### Módulo Notificaciones (100%) ✅
- [x] `NotificacionRespuestaDTO.java` — DTO salida (campo: `tipoNotificacion`, no `tipo`)
- [x] `CorreoServicio.java` — Servicio de envío de emails via SMTP (en `notificacion/service/`)
- [x] `NotificacionServicio.java` — Lógica completa: notificarSolicitud, notificarAprobacion, notificarRechazo, detectarMoras (@Scheduled 8AM diario), enviarRecordatorios (@Scheduled 9AM diario), verificarStockBajo (@Scheduled 7AM lunes), listarPorUsuario, listarNoLeidasPorUsuario, contarNoLeidas, listarMisNotificaciones, listarMisNoLeidas, contarMisNoLeidas, marcarComoLeida
- [x] `NotificacionControlador.java` — 5 endpoints en `notificacion/controller/`:
  - GET /api/v1/notificaciones/usuario/{usuarioId} (solo ADMIN)
  - GET /api/v1/notificaciones/mis-notificaciones (autenticado)
  - GET /api/v1/notificaciones/mis-notificaciones/no-leidas (autenticado)
  - GET /api/v1/notificaciones/mis-notificaciones/contador → {"noLeidas": N}
  - PATCH /api/v1/notificaciones/{id}/marcar-leida
- [x] `TareasProgramadasConfig.java` — @Configuration + @EnableScheduling en `configuracion/`

**Notas clave:**
- `EstadoEnvio` es FEMENINO: `ENVIADA`, `FALLIDA` (NO `ENVIADO/FALLIDO`)
- `TipoNotificacion` válidos: `RECORDATORIO_VENCIMIENTO, MORA, STOCK_BAJO, SOLICITUD_PRESTAMO, GENERAL`
- Archivo `service/NotificacionControlador.java` quedó como stub (solo comentario) — el real está en `controller/`
- BUILD SUCCESS confirmado (27 feb 2026)

---

#### Módulo Auditoría (100%) ✅
- [x] `LogAuditoriaRespuestaDTO.java` — DTO salida en `auditoria/dto/`
- [x] `AuditoriaServicio.java` — Service en `auditoria/service/`: registrar, listarTodos, listarPorUsuario, listarPorEntidad, listarPorRangoFechas
- [x] `AuditoriaControlador.java` — 4 endpoints en `auditoria/controller/` (todos `@PreAuthorize ADMINISTRADOR`):
  - GET /api/v1/auditoria
  - GET /api/v1/auditoria/usuario/{usuarioId}
  - GET /api/v1/auditoria/entidad/{entidad}/{entidadId}
  - GET /api/v1/auditoria/rango?desde=...&hasta=...
- BUILD SUCCESS confirmado (27 feb 2026)

**Nota clave:** `AuditoriaServicio.registrar()` se llama desde otros servicios para guardar trazabilidad. `usuario` puede ser null (acciones del sistema/cron).

#### Módulo Reservas (100%) ✅
- [x] `ReservaCrearDTO.java` — DTO entrada: equipoId, cantidad, fechaHoraInicio (fechaHoraFin = inicio + 2h en servicio)
- [x] `ReservaRespuestaDTO.java` — DTO salida con datos enriquecidos (usuario, equipo)
- [x] `ReservaRepository.java` — findByEstado, findByEstadoAndFechaHoraFinBefore, findReservasSolapadas (JPQL)
- [x] `ReservaServicio.java` — crear (validación 5 días hábiles, disponibilidad en periodo), cancelar (solo antes de inicio, RF-RES-03), listarTodos, listarPorEstado, listarPorUsuario, listarMisReservas, buscarPorId, expirarReservasNoRecogidas (@Scheduled cada 10 min)
- [x] `ReservaControlador.java` — 7 endpoints: POST /reservas, GET /reservas, GET /reservas/mis-reservas, GET /reservas/estado/{estado}, GET /reservas/usuario/{usuarioId}, GET /reservas/{id}, PATCH /reservas/{id}/cancelar
- [x] `FechasUtil.java` (common/util) — sumarDiasHabiles para RF-RES-01

**Reglas implementadas:** RF-RES-01 (máx. 5 días hábiles), RF-RES-02 (expiración automática 2h), RF-RES-03 (cancelar antes de inicio), RF-RES-04 (disponibilidad por periodo).

---

### 🔄 EN PROGRESO

*(Ninguno actualmente — listo para Módulo Reportes)*

---

### ❌ PENDIENTE (orden de desarrollo)

| # | Módulo | Descripción |
|---|---|---|
| 1 | ~~**Configurar MariaDB**~~ | ~~Crear BD, usuario, permisos, probar arranque~~ ✅ |
| 2 | ~~**Seguridad (JWT + Spring Security)**~~ | ~~JWT, login, registro, bloqueo de cuentas~~ ✅ |
| 3 | ~~**Módulo Usuarios**~~ | ~~DTOs, Service, Controller (CRUD con roles)~~ ✅ |
| 4 | ~~**Módulo Ambientes**~~ | ~~DTOs, Service, Controller~~ ✅ |
| 5 | ~~**Módulo Equipos**~~ | ~~DTOs, Service, Controller + manejo de fotos~~ ✅ |
| 6 | ~~**Módulo Préstamos**~~ | ~~DTOs, Service, Controller~~ ✅ |
| 7 | ~~**Módulo Notificaciones**~~ | ~~Envío de correos + alertas internas + tareas programadas~~ ✅ |
| 8 | ~~**Módulo Auditoría**~~ | ~~Log de acciones críticas~~ ✅ |
| 9 | **Módulo Reportes** | Generación de Excel (POI) y PDF (OpenPDF) |
| 10 | ~~**Módulo Reservas**~~ | ~~Reservas anticipadas (Could Have)~~ ✅ |
| 11 | **Módulo Transferencias** | Movimiento entre ambientes (Should) |
| 12 | **Módulo Mantenimiento** | Historial de reparaciones (Could) |
| 13 | **Dashboard** | Endpoint de estadísticas |
| 14 | **Frontend Angular** | Toda la interfaz de usuario |

---

## Estructura de Paquetes (Patrón por Módulo)

Cada módulo sigue esta estructura:
```
modulo/
├── controller/   → Endpoints REST (@RestController)
├── dto/          → Objetos de transferencia (entrada y salida)
├── entity/       → Entidad JPA (@Entity)
├── exception/    → Excepciones específicas del módulo (si las hay)
├── repository/   → Interfaz JPA (@Repository)
└── service/      → Lógica de negocio (@Service)
```

---

## Configuración de Conexión a BD (application.properties)

```properties
spring.datasource.url=jdbc:mariadb://localhost:3305/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
spring.datasource.username=sigea_user
spring.datasource.password=sigea_password_2026
server.servlet.context-path=/api/v1
server.port=8080
```

---

*Última actualización: 9 de marzo de 2026*  
*Módulo Reservas completado. Próxima sesión: Módulo Reportes (Excel con Apache POI + PDF con OpenPDF)*
