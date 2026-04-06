# SIGEA - Contexto Integral de Desarrollo, Operacion y Estado Actual

> Documento maestro del proyecto SIGEA.
> Debe permitir que cualquier persona nueva entienda el negocio, la arquitectura, los modulos, el estado real del sistema, las reglas de negocio activas y la forma segura de intervenir el codigo sin romper el funcionamiento actual.
> Si se pierde el hilo del proyecto, leer este archivo primero.

---

## 1. Identidad del proyecto

- Nombre: SIGEA - Sistema Integral de Gestion de Equipos y Activos
- Institucion: SENA
- Centro: Centro Industrial de Mantenimiento Integral (CIMI)
- Responsable funcional y tecnico principal: Camilo Lopez Romero
- Repositorio: https://github.com/camilo146/SIGEA.git
- Rama principal: main
- Naturaleza: aplicacion web para control operativo de inventario, prestamos, reservas, mantenimientos, usuarios, trazabilidad y reportes de activos institucionales

SIGEA nace para resolver un problema operativo concreto: en el ambiente de formacion existen herramientas, equipos, consumibles y activos que salen a prestamo o uso interno sin trazabilidad suficiente. Eso genera perdida de control, dificultad para saber quien tiene que devolver que, descuadres de inventario y falta de evidencia historica para auditoria o gestion.

La aplicacion busca centralizar esa operacion en una sola plataforma web con reglas de negocio claras, seguridad por roles, trazabilidad historica, dashboard para consulta y reportes exportables.

---

## 2. Problema de negocio que resuelve SIGEA

### 2.1 Dolor operativo original

Antes de SIGEA, el control de equipos y activos dependia en gran medida de registros manuales, memoria operativa o control parcial por parte de instructores y personal del ambiente. Eso llevaba a varios problemas:

1. No habia certeza del stock real disponible.
2. No existia trazabilidad completa de quien solicito, retiro y devolvio cada equipo.
3. Los equipos podian quedar en mora o perderse sin una ruta clara de seguimiento.
4. Los mantenimientos y bajas no quedaban centralizados con el inventario vivo.
5. Los responsables del ambiente no tenian una vista consolidada para tomar decisiones.

### 2.2 Resultado esperado del sistema

SIGEA debe permitir:

1. Saber que equipos existen, donde estan, en que estado se encuentran y cuantas unidades hay disponibles.
2. Controlar prestamos de forma formal, con aprobacion, entrega, devolucion y trazabilidad.
3. Gestionar reservas anticipadas sin sobreasignar inventario.
4. Registrar mantenimientos y bloquear equipos no aptos para prestamo.
5. Gestionar usuarios y roles con seguridad JWT.
6. Emitir notificaciones y alertas de eventos relevantes.
7. Exportar informacion util para seguimiento y toma de decisiones.

---

## 3. Alcance funcional actual del sistema

El sistema hoy ya cubre una porcion importante del negocio real. No esta en estado de maqueta; ya contiene entidades, servicios, reglas de negocio, seguridad, frontend funcional y despliegue por contenedores.

### 3.1 Modulos existentes en backend

El backend tiene paquetes y logica real para estos modulos:

1. Seguridad y autenticacion
2. Usuarios
3. Categorias
4. Ambientes y sub-ubicaciones
5. Equipos y fotos
6. Prestamos
7. Reservas
8. Transferencias
9. Mantenimientos
10. Notificaciones
11. Auditoria
12. Dashboard
13. Reportes
14. Configuracion base y tareas programadas

### 3.2 Modulos existentes en frontend

El frontend Angular tiene rutas, vistas y servicios para estos modulos:

1. Login
2. Verificacion de correo
3. Dashboard
4. Inventario
5. Prestamos
6. Reservas
7. Mi ambiente
8. Ambientes
9. Usuarios
10. Reportes
11. Transferencias
12. Mantenimientos
13. Alimentador
14. Notificaciones integradas en layout principal

### 3.3 Estado general actual

Estado real confirmado por codigo y operacion local:

1. Backend funcional con seguridad JWT, JPA, Flyway, reglas de negocio y endpoints REST.
2. Frontend funcional con autenticacion, layout, guards, servicios HTTP y modulos cableados al backend.
3. Base de datos MariaDB con migraciones controladas por Flyway.
4. Despliegue local por Docker Compose operativo.
5. Entorno local actual funcionando por http://localhost:4043.
6. Persisten consideraciones de infraestructura en servidor para el acceso por dominio interno, no por un problema central del codigo de negocio sino de DNS/red/proxy.

---

## 4. Stack tecnologico real

### 4.1 Backend

- Java 21
- Spring Boot 3.5.10
- Spring Web
- Spring Security
- JWT con jjwt 0.12.6
- Spring Data JPA
- Hibernate
- Flyway
- MariaDB JDBC
- Spring Mail
- Spring Actuator
- Spring Validation
- Thymeleaf (presente como dependencia, aunque el sistema es esencialmente API REST)
- Springdoc OpenAPI 2.8.6
- Apache POI 5.3.0 para Excel
- OpenPDF 2.0.3 para PDF
- Lombok
- Maven

### 4.2 Frontend

- Angular 18
- TypeScript
- RxJS
- HttpClient
- Guards de rutas
- Interceptor JWT
- Nginx dentro del contenedor del frontend para servir estaticos y proxear API

### 4.3 Infraestructura y despliegue

- Docker
- Docker Compose v2
- MariaDB 11.4
- Contenedor backend Spring Boot
- Contenedor frontend Nginx/Angular
- Contenedor Caddy como proxy reverso en escenarios donde se use dominio

---

## 5. Arquitectura de alto nivel

### 5.1 Flujo general

El flujo funcional del sistema es:

1. El usuario entra desde navegador al frontend.
2. El frontend Angular muestra vistas y llama a la API REST.
3. El backend Spring Boot valida JWT, aplica reglas de negocio y persiste datos.
4. MariaDB almacena el estado operativo de inventario, usuarios, prestamos, reservas, notificaciones y auditoria.
5. En despliegues con dominio, Caddy puede ubicarse por delante para resolver acceso por host y eventualmente HTTPS.

### 5.2 Diagrama mental simplificado

Navegador -> Caddy o acceso directo al frontend -> Frontend Angular/Nginx -> Backend Spring Boot -> MariaDB

### 5.3 Base path de la API

Todos los endpoints del backend salen bajo:

`/api/v1`

Ejemplo:

`/api/v1/auth/login`

### 5.4 Acceso local actual

En local, el acceso confirmado y estable actualmente es:

`http://localhost:4043`

Ese puerto sale de un override local para no contaminar la configuracion compartida del repositorio.

---

## 6. Modelo de negocio y dominio funcional

### 6.1 Conceptos principales del negocio

#### Usuario

Persona que interactua con el sistema. Puede ser administrador, instructor, alimentador de equipos, aprendiz, funcionario o usuario estandar de compatibilidad.

#### Categoria

Clasificacion del inventario. Permite agrupar equipos por tipo logico y facilita filtros y reportes.

#### Ambiente

Ubicacion principal o espacio de formacion al que pertenecen equipos. El sistema tambien soporta sub-ubicaciones asociadas a un ambiente padre.

#### Equipo

Activo inventariable del sistema. Puede representar herramientas, equipos de medicion, activos reutilizables o consumibles. Tiene stock total, stock disponible, categoria, estado operativo y relaciones con ambiente, fotos, prestamos, reservas, mantenimientos y transferencias.

#### Prestamo

Proceso formal de salida temporal de uno o varios equipos a un usuario. Tiene ciclo de vida, aprobacion administrativa, salida fisica y devolucion.

#### Reserva

Separacion temporal anticipada de un equipo para un usuario. Puede convertirse luego en prestamo efectivo cuando el equipo se recoge.

#### Transferencia

Movimiento de equipos entre ambientes o responsables.

#### Mantenimiento

Registro del ciclo de reparacion, intervencion o mantenimiento preventivo/correctivo de un equipo.

#### Notificacion

Registro y entrega de eventos relevantes del sistema al usuario.

#### Auditoria

Historial de acciones relevantes, util para trazabilidad operativa y control administrativo.

#### Reporte

Consulta exportable en PDF o Excel sobre inventario, prestamos, mora o uso.

---

## 7. Roles existentes y permisos funcionales

### 7.1 Roles presentes en codigo

Los roles definidos actualmente en [Rol.java](sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/Rol.java) son:

1. ADMINISTRADOR
2. ALIMENTADOR_EQUIPOS
3. INSTRUCTOR
4. APRENDIZ
5. FUNCIONARIO
6. USUARIO_ESTANDAR

### 7.2 Resumen practico por rol

#### ADMINISTRADOR

Es el rol con mayor control operativo. Puede:

1. Gestionar usuarios.
2. Aprobar o rechazar usuarios pendientes.
3. Cambiar roles.
4. Activar, desactivar y desbloquear usuarios.
5. Crear, editar y administrar ambientes.
6. Crear, editar, activar, dar de baja, recuperar y administrar equipos.
7. Aprobar, rechazar, entregar y recibir prestamos.
8. Gestionar reservas y marcar recogida.
9. Gestionar mantenimientos.
10. Consultar auditoria.
11. Generar reportes.
12. Operar transferencias.

#### INSTRUCTOR

Es un rol operativo intermedio. Puede:

1. Gestionar ambientes asignados o relacionados al flujo operativo.
2. Gestionar inventario operativo.
3. Aprobar/rechazar/entregar/devolver prestamos.
4. Gestionar reservas en el flujo operativo.
5. Gestionar mantenimientos.
6. Usar reportes.
7. Usar transferencias.

No puede administrar usuarios como un administrador.

#### ALIMENTADOR_EQUIPOS

Rol orientado a carga o alimentacion operativa. En codigo puede:

1. Crear equipos.
2. Crear ambientes y sub-ubicaciones.

No tiene acceso amplio a modulos administrativos.

#### APRENDIZ / FUNCIONARIO / USUARIO_ESTANDAR

Roles de usuario final. Pueden:

1. Iniciar sesion.
2. Consultar dashboard.
3. Consultar inventario.
4. Solicitar prestamos.
5. Consultar sus prestamos.
6. Crear y consultar sus reservas.
7. Consultar sus notificaciones.

---

## 8. Reglas de negocio activas mas importantes

Esta seccion es critica. Resume reglas reales ya implementadas y que no deben romperse al hacer cambios.

### 8.1 Autenticacion y acceso

1. El login usa correoElectronico y contrasena.
2. La autenticacion es stateless con JWT.
3. El token se devuelve como Bearer.
4. La contrasena se guarda con BCrypt.
5. Tras 3 intentos fallidos, la cuenta se bloquea temporalmente.
6. El primer bloqueo es de 5 minutos.
7. Bloqueos posteriores escalan a 15 minutos.
8. Si la configuracion exige verificacion de correo, los usuarios no administradores deben verificarlo antes de iniciar sesion.
9. Los usuarios no administradores pueden quedar en estado pendiente de aprobacion y no iniciar sesion hasta ser aprobados.

### 8.2 Registro de usuarios

1. No se permite duplicar numero de documento.
2. No se permite duplicar correo electronico.
3. El registro por defecto crea usuario con rol USUARIO_ESTANDAR.
4. El estado inicial de aprobacion del usuario registrado es PENDIENTE.
5. Si la verificacion de correo esta activa, se genera codigo y vigencia temporal.

### 8.3 Inventario y equipos

1. Un equipo tiene cantidad total y cantidad disponible.
2. El campo activo indica si el equipo existe logicamente en el sistema.
3. El enum EstadoEquipo indica si operativamente esta ACTIVO o EN_MANTENIMIENTO.
4. Un equipo dado de baja no debe tratarse como disponible operativamente.
5. Se soportan fotos por equipo.
6. Los equipos pueden pertenecer a ambientes y opcionalmente a sub-ubicaciones.
7. Hay inventarios filtrados por ambiente y por responsable.

### 8.4 Prestamos

Reglas extraidas de [PrestamoServicio.java](sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/service/PrestamoServicio.java):

1. Un usuario no puede tener mas de 3 prestamos ACTIVOS simultaneos.
2. No se crea una solicitud si no hay stock suficiente.
3. El stock no se descuenta al solicitar; se descuenta al registrar la salida fisica.
4. El prestamo pasa por estos estados: SOLICITADO, APROBADO, RECHAZADO, ACTIVO, DEVUELTO, EN_MORA, CANCELADO.
5. Solo prestamos SOLICITADOS pueden aprobarse o rechazarse.
6. Solo prestamos APROBADOS pueden registrar salida.
7. La salida descuenta stock disponible.
8. La devolucion repone stock para equipos no consumibles.
9. Los consumibles tienen tratamiento especial: pueden cerrar flujo sin esperar devolucion clasica.
10. La devolucion documenta condicion de entrega y devolucion.
11. Existen tareas programadas para marcar mora automaticamente.

### 8.5 Reservas

Reglas extraidas de [ReservaServicio.java](sigea-backend/src/main/java/co/edu/sena/sigea/reserva/service/ReservaServicio.java):

1. La fecha de inicio de la reserva no puede superar 5 dias habiles de anticipacion.
2. La reserva solo puede iniciar en el futuro.
3. La disponibilidad se calcula restando reservas activas solapadas del stock disponible.
4. Una reserva activa puede cancelarse solo antes de la fecha/hora de inicio.
5. Solo el duenio puede cancelar su propia reserva.
6. Una reserva activa no recogida dentro de 2 horas expira automaticamente.
7. Al marcar equipo recogido, se crea un prestamo ACTIVO y se descuenta stock.
8. La reserva pasa a estado PRESTADO cuando se convierte en prestamo.

### 8.6 Mantenimientos

1. Un mantenimiento afecta la disponibilidad operativa del equipo.
2. Existen consultas por equipo, por tipo y por mantenimientos en curso.
3. Solo ADMINISTRADOR e INSTRUCTOR pueden crear, cerrar, editar o eliminar mantenimientos.

### 8.7 Transferencias

1. El sistema registra movimientos entre inventarios de origen y destino.
2. ADMINISTRADOR e INSTRUCTOR pueden crear transferencias.
3. Existen consultas por equipo y por inventario origen/destino.

### 8.8 Notificaciones

1. Hay servicio dedicado de notificaciones.
2. El layout principal del frontend muestra contador y listado de notificaciones.
3. Se soporta marcar notificaciones como leidas.
4. Existen eventos ligados a reservas, prestamos, mora, stock bajo y recordatorios.

---

## 9. Estados importantes del dominio

### 9.1 Estado de equipo

Valores activos en codigo:

1. ACTIVO
2. EN_MANTENIMIENTO

### 9.2 Estado de prestamo

Valores activos en codigo:

1. SOLICITADO
2. APROBADO
3. RECHAZADO
4. ACTIVO
5. DEVUELTO
6. EN_MORA
7. CANCELADO

### 9.3 Estado de reserva

Valores activos en codigo:

1. ACTIVA
2. CANCELADA
3. COMPLETADA
4. EXPIRADA
5. PRESTADO

---

## 10. Arquitectura tecnica del backend

### 10.1 Estilo general

El backend sigue una arquitectura tipica por capas:

1. Controller: expone endpoints REST.
2. Service: concentra la logica de negocio.
3. Repository: persistencia JPA.
4. Entity: modelo de datos persistente.
5. DTO: contratos de entrada y salida.
6. Config: seguridad, CORS, scheduler y comportamiento transversal.

### 10.2 Paquetes principales

- ambiente
- auditoria
- categoria
- common
- configuracion
- dashboard
- equipo
- mantenimiento
- notificacion
- prestamo
- reporte
- reserva
- seguridad
- transferencia
- usuario

### 10.3 Seguridad

Archivo clave: [SecurityConfig.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/config/SecurityConfig.java)

Comportamiento importante:

1. CSRF deshabilitado por ser API REST stateless.
2. CORS configurado por `sigea.app.url` y origenes locales comunes.
3. Cualquier ruta que contenga `/auth` queda publica.
4. `/actuator/**` y `/uploads/**` quedan permitidos sin JWT.
5. Todo lo demas requiere autenticacion.
6. El filtro JWT se ejecuta antes de UsernamePasswordAuthenticationFilter.

### 10.4 JWT

Archivos clave:

- [JwtProveedor.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/jwt/JwtProveedor.java)
- [JwtFiltroAutenticacion.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/jwt/JwtFiltroAutenticacion.java)

Uso:

1. Login genera token.
2. Frontend lo guarda en localStorage.
3. Interceptor Angular lo adjunta en Authorization.
4. Backend reconstruye autenticacion a partir del JWT.

### 10.5 Migraciones y base de datos

La base de datos se gobierna con Flyway. No debe dependerse de `ddl-auto=update` para evolucionar esquema. El proyecto usa:

`spring.jpa.hibernate.ddl-auto=validate`

Eso obliga a mantener migraciones consistentes y evita cambios silenciosos de esquema.

---

## 11. Arquitectura tecnica del frontend

### 11.1 Estructura principal

- app.routes.ts define rutas y guards
- core contiene servicios, modelos, guards e interceptor JWT
- layout contiene el contenedor visual principal
- pages contiene modulos/pantallas
- shared contiene componentes compartidos de soporte

### 11.2 Flujo de autenticacion frontend

Archivo clave: [auth.service.ts](sigea-frontend/src/app/core/services/auth.service.ts)

Comportamiento actual:

1. Login llama a `/api/v1/auth/login`.
2. Guarda token y sesion en localStorage.
3. Expone signals para rol, sesion e indicadores de acceso.
4. Logout limpia token y redirige a login.

### 11.3 Layout y experiencia principal

Archivo clave: [main-layout.component.ts](sigea-frontend/src/app/layout/main-layout/main-layout.component.ts)

El layout principal hoy gestiona:

1. Sidebar
2. Menu de usuario
3. Centro de notificaciones
4. Contador de no leidas
5. Breadcrumb simple por ruta
6. Visibilidad de opciones segun rol

### 11.4 Rutas activas del frontend

Rutas confirmadas en [app.routes.ts](sigea-frontend/src/app/app.routes.ts):

1. /login
2. /verificar-email
3. /dashboard
4. /inventario
5. /prestamos
6. /reservas
7. /mi-ambiente
8. /ambientes
9. /usuarios
10. /reportes
11. /transferencias
12. /mantenimientos
13. /alimentador

### 11.5 Guards importantes

Guardas activas:

1. authGuard
2. adminGuard
3. adminOrInstructorGuard
4. alimentadorGuard

Estas guardas son la primera barrera UX. La seguridad real sigue estando en backend mediante JWT y `@PreAuthorize`.

---

## 12. Integracion frontend-backend por modulo

Esta seccion describe como se conectan realmente los modulos hoy.

### 12.1 Auth

Frontend consume:

1. POST /auth/login
2. POST /auth/registro
3. POST /auth/verificar-email

### 12.2 Dashboard

Frontend consume:

1. GET /dashboard/estadisticas
2. GET /dashboard/grafico-prestamos-por-mes
3. GET /dashboard/grafico-equipos-por-categoria

### 12.3 Usuarios

Frontend consume:

1. GET /usuarios
2. GET /usuarios/todos
3. GET /usuarios/rol/{rol}
4. GET /usuarios/{id}
5. POST /usuarios
6. PUT /usuarios/{id}
7. PATCH /usuarios/{id}/rol
8. PATCH /usuarios/{id}/activar
9. PATCH /usuarios/{id}/desactivar
10. PATCH /usuarios/{id}/desbloquear
11. DELETE /usuarios/{id}
12. GET /usuarios/pendientes
13. PATCH /usuarios/{id}/aprobar
14. DELETE /usuarios/{id}/rechazar

### 12.4 Ambientes

Frontend consume:

1. GET /ambientes
2. GET /ambientes/todos
3. GET /ambientes/mi-ambiente
4. GET /ambientes/{id}
5. POST /ambientes
6. PUT /ambientes/{id}
7. PATCH /ambientes/{id}/activar
8. PATCH /ambientes/{id}/desactivar
9. GET /ambientes/{padreId}/sub-ubicaciones
10. POST /ambientes/{padreId}/sub-ubicaciones

### 12.5 Categorias

Frontend consume:

1. GET /categorias
2. GET /categorias/todas
3. GET /categorias/{id}

### 12.6 Equipos

Frontend consume:

1. GET /equipos
2. GET /equipos/todos
3. GET /equipos/categoria/{categoriaId}
4. GET /equipos/ambiente/{ambienteId}
5. GET /equipos/estado/{estado}
6. GET /equipos/{id}
7. POST /equipos
8. PUT /equipos/{id}
9. PATCH /equipos/{id}/estado/{nuevoEstado}
10. PATCH /equipos/{id}/dar-de-baja
11. PATCH /equipos/{id}/activar
12. DELETE /equipos/{id}
13. POST /equipos/{id}/fotos
14. DELETE /equipos/{id}/fotos/{fotoId}
15. GET /equipos/mi-inventario
16. GET /equipos/mis-equipos
17. PATCH /equipos/{id}/recuperar

### 12.7 Prestamos

Frontend consume:

1. GET /prestamos
2. GET /prestamos/mis-prestamos
3. GET /prestamos/estado/{estado}
4. GET /prestamos/{id}
5. POST /prestamos
6. PATCH /prestamos/{id}/aprobar
7. PATCH /prestamos/{id}/rechazar
8. PATCH /prestamos/{id}/registrar-salida
9. PATCH /prestamos/{id}/registrar-devolucion
10. DELETE /prestamos/{id}

### 12.8 Reservas

Frontend consume:

1. GET /reservas
2. GET /reservas/mis-reservas
3. GET /reservas/estado/{estado}
4. GET /reservas/{id}
5. POST /reservas
6. PATCH /reservas/{id}/cancelar
7. PATCH /reservas/{id}/equipo-recogido
8. DELETE /reservas/{id}

### 12.9 Mantenimientos

Frontend consume:

1. GET /mantenimientos
2. GET /mantenimientos/equipo/{equipoId}
3. GET /mantenimientos/tipo/{tipo}
4. GET /mantenimientos/en-curso
5. GET /mantenimientos/{id}
6. POST /mantenimientos
7. PATCH /mantenimientos/{id}/cerrar
8. PUT /mantenimientos/{id}
9. DELETE /mantenimientos/{id}

### 12.10 Transferencias

Frontend consume:

1. GET /transferencias
2. GET /transferencias/{id}
3. POST /transferencias
4. GET /transferencias/inventario-origen/{instructorId}
5. GET /transferencias/inventario-destino/{instructorId}

### 12.11 Reportes

Frontend consume endpoints blob para descarga desde reporte.service.
Se soportan exportaciones sobre:

1. inventario
2. prestamos
3. equipos mas solicitados
4. usuarios en mora

### 12.12 Notificaciones

Frontend consume:

1. GET /notificaciones/mis-notificaciones
2. GET /notificaciones/mis-notificaciones/no-leidas
3. GET /notificaciones/mis-notificaciones/contador
4. PATCH /notificaciones/{id}/marcar-leida

---

## 13. Endpoints principales del backend por contexto funcional

### 13.1 Auth

- POST /api/v1/auth/login
- POST /api/v1/auth/registro
- POST /api/v1/auth/verificar-email

### 13.2 Dashboard

- GET /api/v1/dashboard/estadisticas
- GET /api/v1/dashboard/grafico-prestamos-por-mes
- GET /api/v1/dashboard/grafico-equipos-por-categoria

### 13.3 Inventario

- /api/v1/categorias
- /api/v1/ambientes
- /api/v1/equipos
- /api/v1/mantenimientos
- /api/v1/transferencias

### 13.4 Operacion de usuarios

- /api/v1/usuarios

### 13.5 Operacion de prestamo y reserva

- /api/v1/prestamos
- /api/v1/reservas

### 13.6 Soporte y observabilidad

- /api/v1/actuator/health
- /api/v1/swagger-ui.html
- /api/v1/api-docs

---

## 14. Modelo de datos y entidades persistentes

El modelo de datos detallado se explica mejor en [SIGEA_Diagramas_Diseno.md](SIGEA_Diagramas_Diseno.md), pero para trabajo rapido hay que recordar que las entidades nucleares reales son:

1. Usuario
2. Categoria
3. Ambiente
4. Equipo
5. FotoEquipo
6. Prestamo
7. DetallePrestamo
8. ExtensionPrestamo
9. ReporteDano
10. Reserva
11. Transferencia
12. Mantenimiento
13. Notificacion
14. LogAuditoria
15. Configuracion

Relaciones que mas afectan cambios:

1. Usuario se relaciona con prestamos, reservas, ambientes, notificaciones y auditoria.
2. Equipo se relaciona con ambiente, categoria, fotos, prestamos, reservas, transferencias y mantenimientos.
3. Prestamo contiene detalles y puede nacer desde una reserva.
4. Reserva y prestamo compiten o encadenan disponibilidad sobre el mismo equipo.

Si se cambia la logica de stock, prestamos o reservas, hay que pensar siempre en estas relaciones juntas.

---

## 15. Configuracion principal del sistema

Archivo clave: [application.properties](sigea-backend/src/main/resources/application.properties)

Parametros criticos:

1. `server.port=8080`
2. `server.servlet.context-path=/api/v1`
3. `spring.datasource.url` apuntando por defecto a MariaDB local por 3305
4. `spring.jpa.hibernate.ddl-auto=validate`
5. `spring.flyway.enabled=true`
6. `sigea.jwt.secret`
7. `sigea.jwt.expiration-ms=28800000`
8. `sigea.auth.require-email-verification`
9. `sigea.app.url`
10. `sigea.uploads.path`

### 15.1 Variables operativas comunes del .env

En despliegue por contenedores, se usan variables como:

1. MARIADB_DATABASE
2. MARIADB_USER
3. MARIADB_PASSWORD
4. MARIADB_ROOT_PASSWORD
5. SPRING_DATASOURCE_URL
6. SPRING_DATASOURCE_USERNAME
7. SPRING_DATASOURCE_PASSWORD
8. SIGEA_APP_URL
9. SIGEA_UPLOADS_PATH
10. SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION
11. SIGEA_SMTP_HOST
12. SIGEA_SMTP_PORT
13. SIGEA_SMTP_USERNAME
14. SIGEA_SMTP_PASSWORD
15. HIKARI_MAX_POOL_SIZE y afines

---

## 16. Despliegue y operacion actual

### 16.1 Contenedores del stack

El stack usa estos servicios base:

1. db
2. backend
3. frontend
4. caddy

### 16.2 Funcion de cada contenedor

#### db

MariaDB con persistencia del dominio del sistema.

#### backend

Expone la API Spring Boot y toda la logica de negocio.

#### frontend

Sirve el build Angular mediante Nginx y hace proxy de `/api/v1` al backend.

#### caddy

Actua como proxy web frontal en escenarios con dominio. Su funcion es recibir peticiones por host y reenviarlas al frontend.

### 16.3 Estado local actual confirmado

En local, hoy esta confirmado:

1. frontend accesible por http://localhost:4043
2. docker compose levanta correctamente la aplicacion
3. backend y db quedan saludables
4. caddy puede correr, pero para desarrollo local el acceso recomendado sigue siendo el frontend directo por 4043

### 16.4 Separacion entre configuracion compartida y local

Esto es muy importante para no romper pull ni servidor:

1. [docker-compose.yml](docker-compose.yml) debe mantenerse como configuracion compartida del proyecto.
2. [docker-compose.override.yml](docker-compose.override.yml) existe solo para tu maquina local.
3. `docker-compose.override.yml` esta excluido localmente via [.git/info/exclude](.git/info/exclude), no por `.gitignore` compartido.
4. [.env](.env) tambien se usa localmente y esta ignorado por git.

Consecuencia practica:

1. Los cambios locales para levantar el proyecto no deben tocar el compose compartido.
2. Si se necesita un ajuste solo local, debe ir al override local o al `.env` local.

---

## 17. Estado del acceso por dominio y consideraciones de infraestructura

Este punto es clave porque mezcla aplicacion con red institucional.

### 17.1 Situacion entendida hasta ahora

1. El servidor objetivo usa IP privada.
2. En SENA existe o existira un DNS interno para resolver el dominio solo dentro de la red institucional.
3. La maquina no se publica todavia a internet hasta que todo este asegurado.
4. Por lo tanto, la estrategia de certificado publico con Let's Encrypt no es la correcta en esta fase.

### 17.2 Problema probable del dominio no disponible

Cuando aparecia mensaje de sitio no disponible, la causa mas probable no era el backend en si, sino una combinacion de:

1. DNS interno no resolviendo al host correcto.
2. Clientes usando un DNS distinto al interno de SENA.
3. Trafico interno a 80/443 no llegando al servidor.
4. Intentos de TLS publico en un entorno que aun no sale a internet.

### 17.3 Ruta correcta para ese frente

Mientras el sistema sea solo interno:

1. El dominio interno debe apuntar a la IP privada del servidor.
2. Debe validarse llegada por 80 y/o 443 dentro de la red SENA.
3. No debe dependerse de Let's Encrypt.
4. Si se necesita HTTPS interno, debe usarse certificado interno de SENA o una CA interna aprobada.

---

## 18. Estado funcional modulo por modulo

### 18.1 Seguridad y autenticacion

Estado: funcional.

Soporta:

1. Login JWT
2. Registro
3. Verificacion de correo por codigo
4. Bloqueo por intentos fallidos
5. Pendiente de aprobacion para usuarios no administradores
6. Guards frontend por rol
7. Seguridad backend con `@PreAuthorize`

### 18.2 Usuarios

Estado: funcional.

Soporta:

1. Creacion
2. Consulta
3. Edicion
4. Cambio de rol
5. Activacion y desactivacion
6. Desbloqueo
7. Aprobacion/rechazo de registros pendientes
8. Consulta de perfil

### 18.3 Ambientes

Estado: funcional.

Soporta:

1. CRUD de ambientes
2. Activar/desactivar
3. Mi ambiente
4. Sub-ubicaciones

### 18.4 Inventario y equipos

Estado: funcional.

Soporta:

1. Crear equipo
2. Listar con filtros
3. Consultar por categoria, ambiente y estado
4. Editar
5. Cambiar estado
6. Dar de baja y activar
7. Recuperar
8. Cargar y eliminar fotos
9. Consultas por inventario propio/mi inventario segun rol

### 18.5 Prestamos

Estado: funcional y con reglas de negocio fuertes.

Soporta:

1. Solicitud
2. Listado general
3. Mis prestamos
4. Filtro por estado
5. Aprobacion
6. Rechazo
7. Registrar salida
8. Registrar devolucion
9. Eliminacion de solicitudes no consumadas

### 18.6 Reservas

Estado: funcional.

Soporta:

1. Crear reserva
2. Mis reservas
3. Filtro por estado
4. Cancelar
5. Convertir en prestamo al recoger equipo
6. Expiracion automatica por no recogida

### 18.7 Mantenimientos

Estado: funcional.

Soporta:

1. Crear
2. Cerrar
3. Consultar general
4. Consultar por equipo
5. Consultar por tipo
6. Consultar en curso
7. Editar
8. Eliminar

### 18.8 Transferencias

Estado: funcional a nivel base.

Soporta:

1. Crear transferencia
2. Listar
3. Consultar por id
4. Consultar inventario origen y destino

### 18.9 Dashboard

Estado: funcional.

Soporta:

1. Estadisticas generales
2. Grafico de prestamos por mes
3. Grafico de equipos por categoria

### 18.10 Reportes

Estado: operativo con exportacion.

Soporta:

1. Reporte de inventario
2. Reporte de prestamos
3. Equipos mas solicitados
4. Usuarios en mora
5. Exportaciones descargables desde frontend

### 18.11 Notificaciones

Estado: funcional.

Soporta:

1. Consulta de mis notificaciones
2. Consulta de no leidas
3. Contador
4. Marcar leida
5. Integracion visual en layout

### 18.12 Auditoria

Estado: funcional para consulta administrativa.

Soporta:

1. Consulta general
2. Consulta por usuario
3. Consulta por entidad
4. Consulta por rango

---

## 19. Flujo funcional completo del sistema

### 19.1 Flujo de acceso

1. Usuario entra al frontend.
2. Login envia correoElectronico y contrasena.
3. Backend valida estado, aprobacion, verificacion y bloqueo.
4. Si pasa, emite JWT.
5. Frontend guarda token y habilita menu segun rol.

### 19.2 Flujo de inventario

1. Operador crea ambiente si hace falta.
2. Operador crea categoria si aplica.
3. Operador registra equipo.
4. Equipo queda disponible segun stock y estado.
5. Se pueden subir fotos y asociarlo a ambiente/sub-ubicacion.

### 19.3 Flujo de prestamo

1. Usuario solicita prestamo.
2. Sistema valida stock y limite de prestamos activos.
3. Solicitud queda SOLICITADA.
4. Admin o instructor aprueba o rechaza.
5. Si aprueba, queda APROBADO.
6. Al entregar fisicamente, se registra salida y se descuenta stock.
7. Prestamo queda ACTIVO.
8. Al devolver, se registra devolucion y se repone stock.
9. Prestamo queda DEVUELTO.
10. Si vence sin devolucion, pasa a EN_MORA.

### 19.4 Flujo de reserva

1. Usuario crea reserva futura.
2. Sistema valida 5 dias habiles maximos y disponibilidad solapada.
3. Reserva queda ACTIVA.
4. Si el usuario recoge el equipo, admin marca recogida y nace un prestamo ACTIVO.
5. Si no lo recoge dentro de la ventana, la reserva EXPIRA.

### 19.5 Flujo de mantenimiento

1. Operador registra mantenimiento.
2. Equipo puede quedar no disponible operativamente.
3. Cuando termina, se cierra el mantenimiento.

### 19.6 Flujo de notificaciones

1. Evento de negocio dispara notificacion.
2. Se registra en backend.
3. Frontend la consulta y la muestra en layout.

---

## 20. Operacion local y comandos utiles

### 20.1 Levantar todo por Docker

```bash
docker compose up -d --build
```

### 20.2 Ver estado de contenedores

```bash
docker compose ps
```

### 20.3 Ver logs de un servicio

```bash
docker compose logs --tail=150 backend
docker compose logs --tail=150 frontend
docker compose logs --tail=150 db
docker compose logs --tail=150 caddy
```

### 20.4 URL local valida hoy

```text
http://localhost:4043
```

### 20.5 Healthcheck API

```bash
curl http://localhost:4043/api/v1/actuator/health
```

### 20.6 Login por API

```bash
curl -X POST http://localhost:4043/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correoElectronico":"admin@sena.edu.co","contrasena":"Admin2026!"}'
```

---

## 21. Archivos clave del proyecto

### 21.1 Documentacion

- [SIGEA_Contexto_Desarrollo.md](SIGEA_Contexto_Desarrollo.md)
- [SIGEA_Documento_Requerimientos.md](SIGEA_Documento_Requerimientos.md)
- [SIGEA_Diagramas_Diseno.md](SIGEA_Diagramas_Diseno.md)
- [DESPLIEGUE_SENA.md](DESPLIEGUE_SENA.md)

### 21.2 Backend

- [pom.xml](sigea-backend/pom.xml)
- [application.properties](sigea-backend/src/main/resources/application.properties)
- [SecurityConfig.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/config/SecurityConfig.java)
- [AuthControlador.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/controller/AuthControlador.java)
- [AutenticacionServicio.java](sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/service/AutenticacionServicio.java)
- [PrestamoServicio.java](sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/service/PrestamoServicio.java)
- [ReservaServicio.java](sigea-backend/src/main/java/co/edu/sena/sigea/reserva/service/ReservaServicio.java)

### 21.3 Frontend

- [app.routes.ts](sigea-frontend/src/app/app.routes.ts)
- [auth.service.ts](sigea-frontend/src/app/core/services/auth.service.ts)
- [main-layout.component.ts](sigea-frontend/src/app/layout/main-layout/main-layout.component.ts)
- [environment.ts](sigea-frontend/src/environments/environment.ts)
- [proxy.conf.json](sigea-frontend/proxy.conf.json)

### 21.4 Infraestructura

- [docker-compose.yml](docker-compose.yml)
- [docker-compose.override.yml](docker-compose.override.yml)
- [Caddyfile](Caddyfile)
- [.env.example](.env.example)
- [deploy.sh](deploy.sh)

---

## 22. Riesgos y puntos delicados al hacer cambios

Esta es una de las secciones mas importantes para cualquier desarrollador nuevo.

### 22.1 No romper la separacion entre config compartida y config local

1. No meter ajustes solo locales en `docker-compose.yml` tracked.
2. Usar `docker-compose.override.yml` para tu maquina.
3. Usar `.env` para variables locales.

### 22.2 No romper reglas de stock

Si tocas prestamos, reservas o mantenimientos, revisa siempre:

1. cantidadDisponible
2. reservas solapadas
3. salida fisica
4. devolucion
5. conversion reserva -> prestamo

### 22.3 No romper seguridad por asumir que el frontend basta

1. Los guards Angular ayudan UX, pero no protegen por si solos.
2. La proteccion real esta en JWT y `@PreAuthorize`.
3. Cualquier nuevo endpoint debe definir autorizacion explicitamente.

### 22.4 No romper migraciones

1. No confiar en cambios implicitos de Hibernate.
2. Si cambias entidades, piensa en Flyway.
3. Verifica compatibilidad con el esquema real.

### 22.5 No romper autenticacion

1. El login espera `correoElectronico`, no `correo`.
2. Existen reglas de aprobacion y verificacion.
3. Los mensajes de error tienen implicacion funcional.

### 22.6 No suponer que dominio interno implica internet

1. El entorno SENA descrito es de DNS interno.
2. No forzar Let's Encrypt si el entorno no sale a internet.
3. Si se quiere HTTPS interno, debe coordinarse con infraestructura institucional.

---

## 23. Checklist seguro antes de modificar algo importante

1. Leer este archivo completo.
2. Confirmar modulo exacto a intervenir.
3. Verificar rutas frontend y endpoints backend relacionados.
4. Revisar servicio de negocio del modulo.
5. Revisar enums y estados afectados.
6. Revisar si toca stock, roles, aprobacion o scheduler.
7. Levantar local por Docker.
8. Probar healthcheck y flujo minimo del modulo.

---

## 24. Estado actual consolidado al momento de esta actualizacion

Fecha de actualizacion del contexto: 6 de abril de 2026.

Estado consolidado:

1. El proyecto tiene base funcional real en backend y frontend.
2. La operacion local esta funcional por Docker Compose.
3. El acceso local util hoy es `http://localhost:4043`.
4. El backend, frontend y base de datos levantan correctamente en local.
5. Caddy existe y puede correr, pero el frente de dominio institucional depende de DNS/red interna y no debe confundirse con un problema central del negocio del sistema.
6. La configuracion local fue separada para evitar conflictos al hacer pull en servidor o en la maquina de desarrollo.

---

## 25. Resumen ejecutivo para alguien nuevo

Si alguien nuevo llega al proyecto, debe entender esto en una sola lectura:

1. SIGEA es una aplicacion web para controlar inventario, prestamos, reservas, mantenimientos, usuarios y reportes de activos del SENA.
2. El backend es Spring Boot 3.5.10 con Java 21, JWT, JPA, Flyway y MariaDB.
3. El frontend es Angular 18 servido por Nginx.
4. La API vive bajo `/api/v1`.
5. El proyecto ya tiene modulos reales, no solo estructura vacia.
6. Los modulos mas sensibles son autenticacion, inventario, prestamos y reservas porque ahi vive la logica critica del negocio.
7. Los roles activos en codigo son ADMINISTRADOR, INSTRUCTOR, ALIMENTADOR_EQUIPOS, APRENDIZ, FUNCIONARIO y USUARIO_ESTANDAR.
8. Localmente, el acceso recomendado es `http://localhost:4043`.
9. El compose compartido no debe usarse para meter hacks locales.
10. El problema del dominio institucional es principalmente de infraestructura interna, no de la logica central de la aplicacion.

Este archivo debe mantenerse vivo. Si cambia el modelo de negocio, la arquitectura, los endpoints, los roles o la estrategia de despliegue, este documento debe actualizarse en la misma tarea.
