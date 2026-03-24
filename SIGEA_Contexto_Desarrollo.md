# SIGEA – Contexto de Desarrollo (Archivo para Copilot)

> **¿Para qué sirve este archivo?**  
> Si pierdes la conversación con Copilot, abre este archivo y dile:  
> *"Lee el archivo SIGEA_Contexto_Desarrollo.md y continúa donde quedamos"*

---

## Datos del Proyecto

- **Proyecto:** SIGEA – Sistema Integral de Gestión de Equipos y Activos
- **Desarrollador:** Camilo López Romero (aprendiz SENA)
- **Institución:** SENA – Centro Industrial de Mantenimiento Integral (CIMI)
- **Stack:** Java 21 + Spring Boot 3.5.10 + MariaDB 12.2 + Angular 17+ (frontend en uso con módulos principales implementados)

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
- [x] `AuthControlador.java` – POST /auth/login, POST /auth/registro, **POST /auth/verificar-email** (body: correo + código 6 dígitos)
- [x] **Verificación de email por código:** En registro se genera código numérico de 6 dígitos (no enlace). Se envía por correo; el usuario lo ingresa en la app. `VerificacionEmailServicio`: `generarCodigoVerificacion()`, `enviarEmailVerificacion(Usuario)`, `verificarCodigo(correo, codigo)`. `VerificarCodigoDTO`: correo, codigo (6 dígitos).
- [x] **Admin exento:** El rol ADMINISTRADOR no requiere verificar correo para iniciar sesión; el resto de roles sí.

---

#### Módulo Usuarios (100%) ✅
- [x] `UsuarioCrearDTO.java` – DTO de entrada para que un admin cree usuario (incluye rol)
- [x] `UsuarioActualizadoDTO.java` – DTO para actualizar datos básicos (sin contraseña ni rol)
- [x] `UsuarioRespuestaDTO.java` – DTO de salida con @Builder (NUNCA expone contraseña)
- [x] `UsuarioCambiarContrasenaDTO.java` – DTO para cambiar contraseña propia (actual + nueva)
- [x] `UsuarioCambiarRolDTO.java` – DTO para cambiar rol de otro usuario (solo admin)
- [x] `UsuarioService.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorRol, buscarPorId, obtenerPerfil, actualizar, cambiarContrasena, cambiarRol, desactivar, activar, desbloquearCuenta
- [x] `UsuarioControlador.java` – 13 endpoints REST (incl. **DELETE /api/v1/usuarios/{id}**)
- [x] **Eliminación real (hard delete):** DELETE elimina el usuario de la BD. No permite si tiene préstamos como solicitante; antes borra notificaciones, reservas y desvincula de ambientes como instructor responsable. Super admin no se puede eliminar.
- [x] `UsuarioRepository.java` – Actualizado con método `findByNumeroDocumento`

---

#### Módulo Ambientes (100%) ✅
- [x] `AmbienteCrearDTO.java` – DTO de entrada con nombre, ubicación, descripción, idInstructorResponsable
- [x] `AmbienteRespuestaDTO.java` – DTO de salida con @Builder (incluye nombre del instructor)
- [x] `AmbienteService.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorInstructor, buscarPorId, actualizar, desactivar, activar
- [x] `AmbienteControlador.java` – 8 endpoints REST (POST, GET varios, PUT, PATCH activar/desactivar)
- [x] **Foto opcional del ambiente:** Entidad y DTO tienen `rutaFoto` (V6__ambiente_ruta_foto.sql). Servicio mapea `rutaFoto` en respuesta.

---

#### Módulo Equipos (100%) ✅
- [x] `FotoEquipoRespuestaDTO.java` – DTO de salida para fotos (id, nombreArchivo, rutaArchivo, tamanoBytes, fechaSubida)
- [x] `EquipoCrearDTO.java` – DTO de entrada con validaciones (@NotBlank, @NotNull, @Min, @Size)
- [x] `EquipoRespuestaDTO.java` – DTO de salida con @Builder (incluye nombre de categoría y ambiente, lista de fotos)
- [x] `EquipoServicio.java` – Lógica de negocio: crear, listarActivos, listarTodos, listarPorCategoria, listarPorAmbiente, listarPorEstado, buscarPorId, actualizar, cambiarEstado, darDeBaja, activar, subirFoto, eliminarFoto
- [x] `EquipoControlador.java` – 14 endpoints REST:
  - POST /api/v1/equipos (201, solo admin/instructor)
  - GET /api/v1/equipos (200, activos), GET /api/v1/equipos/todos (200, incluye inactivos)
  - GET /api/v1/equipos/categoria/{categoriaId}, GET /api/v1/equipos/ambiente/{ambienteId}, GET /api/v1/equipos/estado/{estado}
  - GET /api/v1/equipos/{id}, PUT /api/v1/equipos/{id}
  - PATCH /api/v1/equipos/{id}/estado/{nuevoEstado}, PATCH /api/v1/equipos/{id}/dar-de-baja, PATCH /api/v1/equipos/{id}/activar
  - POST /api/v1/equipos/{id}/fotos (201, multipart), DELETE /api/v1/equipos/{id}/fotos/{fotoId}
  - **DELETE /api/v1/equipos/{id}** (204, eliminación permanente; no permite si tiene reservas/préstamos/mantenimientos/transferencias)
- [x] Foto obligatoria al registrar equipo; `WebMvcConfig` sirve `/uploads/**`; `SecurityConfig` permite acceso público a `/uploads/**`

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
- [x] `PrestamoServicio.java` — solicitar, aprobar, rechazar, registrarSalida, **registrarDevolucion** (si el préstamo viene de reserva, marca la reserva como COMPLETADA), cancelar, **eliminar** (solo estado SOLICITADO), listarTodos, listarMisPrestamos, listarPorUsuario, listarPorEstado, buscarPorId
- [x] **Vínculo reserva–préstamo:** Tabla `prestamo` tiene `reserva_id` (V5__prestamo_reserva_id.sql). Al crear préstamo desde "equipo recogido" en reserva se asigna `prestamo.reserva`; al registrar devolución se actualiza `reserva.estado = COMPLETADA`.
- [x] `PrestamoControlador.java` — Incluye **DELETE /api/v1/prestamos/{id}** (solo SOLICITADO), resto de endpoints

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
- [x] `ReservaControlador.java` — Incluye **DELETE /api/v1/reservas/{id}** (no permitido si estado PRESTADO); POST, GET, GET mis-reservas, GET por estado/usuario, GET {id}, PATCH cancelar
- [x] `ReservaServicio.java` — eliminar(id): no permite si estado PRESTADO
- [x] `FechasUtil.java` (common/util) — sumarDiasHabiles para RF-RES-01

**Reglas implementadas:** RF-RES-01 (máx. 5 días hábiles), RF-RES-02 (expiración automática 2h), RF-RES-03 (cancelar antes de inicio), RF-RES-04 (disponibilidad por periodo).

---

#### Módulo Mantenimiento (100%) ✅
- [x] Crear, listar, buscar por equipo/estado, cerrar mantenimiento
- [x] **PUT /api/v1/mantenimientos/{id}** — Actualizar (solo si no está cerrado, `fechaFin` null)
- [x] **DELETE /api/v1/mantenimientos/{id}** — Eliminar (solo si no cerrado); si era el último del equipo, estado vuelve a ACTIVO

---

#### Correo y notificaciones
- [x] `CorreoServicio.java`: si el envío falla (ej. sin SMTP real), se registra en log el **contenido completo** del correo (código de verificación, notificaciones) para verlo en consola en desarrollo.
- [x] `application.properties`: nota indicando que con `spring.mail.host=localhost` los correos no llegan a bandeja; para producción configurar SMTP real (Gmail, Outlook, SENA).

---

#### Migraciones Flyway recientes
- [x] **V5__prestamo_reserva_id.sql** — Columna `prestamo.reserva_id` (FK a reserva) para vincular préstamo originado desde reserva.
- [x] **V6__ambiente_ruta_foto.sql** — Columna `ambiente.ruta_foto` (VARCHAR 500, opcional) para foto del ambiente.

---

### 🔄 EN PROGRESO

*(Ninguno actualmente — listo para Módulo Reportes o mejoras frontend)*

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
| 12 | ~~**Módulo Mantenimiento**~~ | ~~Historial, crear, cerrar, editar, eliminar~~ ✅ |
| 13 | **Dashboard** | Endpoint de estadísticas |
| 14 | **Frontend Angular** | En progreso: login, registro, verificación por código, inventario, ambientes, reservas, préstamos, mantenimientos, usuarios, notificaciones (ver sección abajo) |

---

## Estado del Frontend (Angular)

### ✅ Implementado
- **Login y registro:** Formularios, JWT en localStorage, guard de rutas por rol (admin, instructor, aprendiz, funcionario).
- **Verificación de email:** Flujo por **código de 6 dígitos** (no enlace): tras registro se muestra paso para ingresar código; página `/verificar-email` con formulario correo + código.
- **Inventario (equipos):** Listado con filtros (categoría, ambiente, estado), búsqueda. Por defecto **no se muestran equipos dados de baja**; opción "Incluir dados de baja" para admin/instructor. **Card al clic** en foto o nombre: detalle con foto grande, datos del equipo, botones "Solicitar préstamo" y "Reservar". Crear/editar equipo con **foto obligatoria** al crear. Acciones: Editar, Dar de baja, Activar, **Eliminar** (permanente). Export Excel/PDF.
- **Ambientes:** Listado, crear/editar/desactivar/activar. **Card al clic** en fila o "Ver equipos": detalle del ambiente con **foto opcional**, ubicación, descripción, responsable, estado y **lista de equipos del ambiente** (con miniatura de foto si existe). Modelo `Ambiente` con `rutaFoto` opcional.
- **Reservas:** Tabs (activas, canceladas, cumplidas). Crear reserva; desde inventario se puede navegar con `reservarEquipoId` en state y se abre modal con equipo preseleccionado. Admin/instructor: **Eliminar** reserva (no si estado PRESTADO). Estado reserva: COMPLETADA cuando se devuelve el préstamo originado desde reserva.
- **Préstamos:** Solicitar, aprobar/rechazar, registrar salida/devolución. **Eliminar** solo si estado SOLICITADO. Devolución de préstamo creado desde reserva actualiza la reserva a COMPLETADA.
- **Mantenimientos:** Listado, crear, **editar** y **eliminar** (solo si no cerrado). Botones visibles en filas "En curso"; en finalizados se muestra "—".
- **Usuarios (admin):** Listado, crear, editar, cambiar rol, activar/desactivar, desbloquear. **Eliminar** = borrado permanente en BD (no si tiene préstamos; se borran notificaciones y reservas, se desvincula de ambientes).
- **Notificaciones:** Listado de mis notificaciones, contador no leídas, marcar como leída.
- **Layout:** Menú según rol (admin, instructor, aprendiz/funcionario); rutas protegidas.

### Pendiente / mejoras
- Subida de foto para ambientes (endpoint multipart y formulario; el campo y la visualización en card ya están).
- Mostrar foto del equipo en listados de préstamos/reservas en cada fila (evitar confusiones).
- Módulo Reportes (backend), Transferencias (backend), Dashboard (backend + frontend).

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

*Última actualización: 10 de marzo de 2026*

**Resumen de lo incorporado en esta actualización:**
- Verificación de email por código de 6 dígitos; admin exento de verificación.
- Equipos: foto obligatoria al crear, DELETE permanente, card detalle al clic (Solicitar préstamo / Reservar), filtro "Incluir dados de baja", equipos inactivos ocultos por defecto en búsqueda.
- Ambientes: campo opcional `rutaFoto`, card detalle al clic con info y equipos (foto opcional, miniaturas de equipos).
- Usuarios: DELETE = eliminación real en BD (con validaciones y limpieza de notificaciones/reservas/ambientes).
- Reserva–préstamo: `prestamo.reserva_id`; al devolver préstamo originado desde reserva, la reserva pasa a COMPLETADA.
- Mantenimientos, reservas, préstamos: editar/eliminar según reglas (mantenimiento solo si no cerrado; reserva no si PRESTADO; préstamo solo si SOLICITADO).
- Correos: log del contenido cuando falla el envío; nota en `application.properties` sobre SMTP real para producción.
- Migraciones V5 (prestamo.reserva_id), V6 (ambiente.ruta_foto).

*Próxima sesión sugerida: Módulo Reportes (Excel/PDF), subida de foto para ambientes, o foto de equipo en listados de préstamos/reservas.*
