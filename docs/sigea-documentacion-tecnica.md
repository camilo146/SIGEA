# SIGEA - Documentacion Tecnica Integral

Fecha de generacion: 2026-04-13

## 1. Resumen General

### 1.1 Proposito del proyecto

SIGEA es una aplicacion web para gestionar inventario, prestamos, reservas, transferencias, mantenimientos, notificaciones y reportes de equipos/activos del SENA. El backend expone una API REST versionada y el frontend Angular consume esa API con autenticacion JWT y control de acceso por roles.

### 1.2 Arquitectura general

```text
Usuario Web
   |
   v
Angular 18 (standalone, guards, interceptor JWT)
   |
   v
API REST /api/v1 (Spring Boot 3.5, Security, JPA, Flyway)
   |
   v
MariaDB 11
```

```text
Frontend
  core -> models/services/guards/interceptor
  layout -> shell principal
  pages -> vistas de negocio

Backend
  controller -> dto -> service -> repository -> entity
  common/configuracion/seguridad -> infraestructura transversal
```

### 1.3 Tecnologias y dependencias principales

- Backend: Java 21, Spring Boot 3.5.10, Spring Web, Spring Security, Spring Data JPA, Flyway, Lombok, MariaDB, OpenPDF, Apache POI, Spring Mail.
- Frontend: Angular 18 standalone, RxJS, Chart.js, SCSS, HttpClient con interceptor funcional.
- Infraestructura: Dockerfile en ambos servicios, nginx para servir frontend, scripts SQL operativos, migraciones Flyway para el esquema.

### 1.4 Alcance documentado

- Incluye codigo fuente y configuracion principal del backend y frontend.
- Incluye scripts SQL y pruebas del backend.
- Excluye artefactos generados: target, dist, node_modules y cache de Angular, porque no son la fuente autoritativa de mantenimiento.

### 1.5 Inventario analizado

- Archivos backend considerados: 192
- Archivos frontend considerados: 104
- Total documentado: 296

## 2. Flujos Importantes

### 2.1 Flujo de autenticacion

```text
Login UI -> AuthService.login() -> POST /auth/login
        -> AutenticacionServicio.login()
        -> UsuarioRepository + PasswordEncoder + JwtProveedor
        -> token JWT
        -> localStorage + signal de sesion
        -> jwtInterceptor agrega Authorization en siguientes requests
```

### 2.2 Flujo principal de inventario y prestamos

```text
Inventario UI -> EquipoService / AmbienteService
            -> API equipos/ambientes
            -> EquipoRepository / AmbienteRepository
            -> persistencia MariaDB

Solicitud de prestamo -> PrestamoServicio.solicitar
                     -> validacion stock + reglas de mora/reserva
                     -> aprobacion/admin
                     -> salida/devolucion
                     -> actualizacion de disponibilidad y notificaciones
```

### 2.3 Flujo de reservas

```text
Reserva UI -> ReservaService.create
          -> ReservaServicio.crear
          -> validacion de solape/disponibilidad
          -> expiracion automatica por tarea programada si no se recoge
```

### 2.4 Flujo de reportes

```text
UI reportes -> ReporteService frontend -> endpoints /reportes
           -> ReporteServicio backend
           -> consulta repositorios
           -> exporta bytes PDF/XLSX mediante OpenPDF o Apache POI
```

## 3. Decisiones de Diseno

- Arquitectura por capas en backend: facilita aislar reglas de negocio de HTTP y persistencia.
- DTOs separados de entidades: reduce acoplamiento de API con el modelo JPA y permite evolucionar contratos.
- JWT stateless: simplifica despliegue horizontal y elimina dependencia de sesion server-side.
- Angular standalone + lazy loading: reduce boilerplate y divide la aplicacion por pantallas.
- Signals en frontend: estado local mas directo que Subject/BehaviorSubject para interaccion de UI.
- Flyway como fuente de verdad del esquema: evita cambios manuales no trazables en BD.

### 3.1 Trade-offs observados

- Lombok reduce ruido en entidades/DTOs, pero genera falsos positivos en algunos analizadores del editor.
- Servicios backend concentran bastante logica; facilita localizar reglas, pero crece el tamano de clases como UsuarioService, PrestamoServicio o ReservaServicio.
- Los componentes de pagina del frontend integran mucha logica visual y de orquestacion; acelera entrega, pero dificulta pruebas unitarias finas.

### 3.2 Deuda tecnica identificada

- Varias clases de servicio y componentes son grandes y mezclan validacion, orquestacion y mapeo.
- La documentacion original del frontend README quedo parcialmente desactualizada respecto al estado actual del producto.
- Hay configuracion sensible con valores por defecto de desarrollo en properties; requiere endurecimiento explicito en produccion.
- Parte de las descripciones de esta documentacion se infieren por convencion de nombres y comentarios; cuando el codigo no lo explicita, se marca con ⚠️.

### 3.3 Decisiones de arquitectura y su razonamiento

- Soft delete con campo `activo`: se usa para mantener trazabilidad historica y evitar romper referencias entre prestamos, reservas, mantenimientos y reportes. En este contexto, borrar fisicamente un equipo o un usuario puede invalidar historicos y auditoria.
- Flyway como control de esquema: permite evolucionar la base de datos con versionado y despliegues repetibles; evita cambios manuales no trazables en produccion.
- JWT en `localStorage`: se priorizo simplicidad de integracion con SPA Angular y despliegue stateless. Implica reforzar controles XSS, expiracion de token y politicas de cabeceras de seguridad.
- Rol `ALIMENTADOR_EQUIPOS` separado de `ADMINISTRADOR`: aplica principio de minimo privilegio para operar inventario sin exponer capacidades de gobierno de usuarios, aprobaciones globales o configuracion sensible.

## 4. Reglas de Negocio Criticas

### 4.1 Prestamos y mora

El flujo de prestamo valida disponibilidad, estado del equipo y restricciones del solicitante antes de crear movimientos. Un prestamo entra en mora cuando supera la fecha de devolucion esperada y sigue sin cierre; ese estado impacta indicadores, alertas y nuevas solicitudes.

Si un equipo esta en mantenimiento o no activo, no debe quedar disponible para prestamo. Esta regla evita doble asignacion fisica y preserva consistencia entre inventario, mantenimiento y operaciones en curso.

### 4.2 Reservas y vencimiento

Las reservas se crean con control de solape para evitar conflictos de horario/uso sobre el mismo recurso. Si la reserva no se materializa dentro del tiempo esperado, una tarea programada puede marcarla como expirada y liberar cupo para nuevos solicitantes.

El paso de reserva a prestamo debe conservar trazabilidad del usuario, del recurso y del estado final, de forma que auditoria y reportes no pierdan contexto operativo.

### 4.3 Equipos: desactivar vs dar de baja

`desactivar` representa inhabilitacion operativa reversible (ejemplo: recurso temporalmente no utilizable). `darDeBaja` representa retiro definitivo de operacion del inventario. Esta distincion evita confundir indisponibilidad temporal con salida permanente del activo.

En ambientes con historico de prestamos y reportes, diferenciar ambos estados evita inconsistencias en dashboards y consultas de trazabilidad.

### 4.4 Evaluacion mensual y mantenimiento

La evaluacion periodica y los procesos de mantenimiento sostienen calidad del inventario: detectan degradacion, disparan acciones correctivas y ajustan la disponibilidad real de equipos.

Cuando existe mantenimiento abierto, el sistema debe tratar el equipo como no disponible para prestamo y reflejar ese estado en los tableros operativos.

## 5. Modelo de Datos y Relaciones Clave

Relaciones que un desarrollador nuevo debe entender desde el primer dia:

```text
Ambiente (padre)
    1 --- N SubUbicaciones (Ambiente hijo)

Categoria 1 --- N Equipo
Marca     1 --- N Equipo
Ambiente  1 --- N Equipo

Prestamo  1 --- N DetallePrestamo --- 1 Equipo
Reserva   N --- 1 Equipo
Mantenimiento N --- 1 Equipo
Transferencia N --- 1 Equipo

Usuario (solicitante/responsable) participa en:
    Prestamo, Reserva, Transferencia, Auditoria
```

Lectura operacional minima:

- `Equipo -> Ambiente -> SubUbicacion` define ubicacion fisica y contexto de disponibilidad.
- `Prestamo -> DetallePrestamo -> Equipo` define consumo de inventario por transaccion.
- `Usuario + Rol` define que acciones son validas por flujo de negocio.

## 6. Estado Actual del Sistema

### 6.1 Funcionalidad operativa esperada

- Gestion de autenticacion, inventario, prestamos, reservas, mantenimiento, transferencias y reportes.
- Trazabilidad basica via auditoria y estados de dominio.
- Despliegue con contenedores y automatizacion de actualizacion en servidor self-hosted.

### 6.2 Pendientes y deuda tecnica conocida

- Servicios y componentes grandes con alta concentracion de reglas; conviene extraer casos de uso para mantenimiento a mediano plazo.
- Dependencia de convenciones y comentarios para parte del entendimiento funcional; se recomienda incorporar ADRs y pruebas de reglas criticas.
- Endurecimiento pendiente de seguridad operativa en entornos reales (secretos, certificados, politica de CORS estricta, rotacion de credenciales).

### 6.3 Riesgos operativos de configuracion

- No usar credenciales de ejemplo en despliegues reales.
- No publicar `.env` ni secretos en repositorios o anexos de documentacion.
- Antes de pasar a produccion, validar hardening de DB, SMTP, JWT y politicas de acceso de red.

## 7. Guia de Uso y Puesta en Marcha

### 4.1 Backend

1. Requisitos: Java 21, Maven Wrapper, MariaDB accesible.
2. Archivo principal de configuracion: sigea-backend/src/main/resources/application.properties.
3. Arranque: ejecutar mvnw spring-boot:run desde sigea-backend.
4. Opcional: definir variables SMTP y .env segun RUN.md si se requiere verificacion por correo.

### 4.2 Frontend

1. Requisitos: Node.js 18+, npm.
2. Instalar dependencias con npm install dentro de sigea-frontend.
3. Levantar backend primero o usar proxy de desarrollo.
4. Ejecutar npm start y abrir http://localhost:4200.

### 4.3 Errores comunes

- Checksum Flyway invalido: usar mvn flyway:repair si una migracion historica fue modificada.
- Errores visuales por Lombok en VS Code: validar primero con mvnw -DskipTests compile.
- SMTP no configurado: si la verificacion por correo esta activa y falla el envio, el registro puede responder con error operativo.
- Sesion expirada: el interceptor JWT marca sessionExpired y la UI debe reenviar al login.

## 8. Inventario por Archivo y Modulo

En las siguientes secciones cada archivo incluye: descripcion, dependencias, exports y elementos principales (clases, interfaces, funciones o scripts). Cuando no existe documentacion explicita dentro del codigo, la finalidad se infiere por convencion de nombres y ubicacion de carpeta.
## Modulo: sigea-backend/.mvn

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/.mvn/wrapper/maven-wrapper.properties

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: maven-wrapper.properties

Notas:
- Claves destacadas: wrapperVersion, distributionType, distributionUrl.

Elementos internos:

#### Properties: maven-wrapper.properties

Configura 3 claves de Spring/entorno.

## Modulo: sigea-backend/Dockerfile

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/Dockerfile

Descripcion: Artefacto de despliegue para empaquetado y entrega del servicio.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: Dockerfile

Elementos internos:

#### Archivo: Dockerfile

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/HELP.md

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/HELP.md

Descripcion: Documento auxiliar del repositorio con instrucciones o contexto funcional.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: HELP.md

Elementos internos:

#### Archivo: HELP.md

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/RUN.md

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/RUN.md

Descripcion: Documento auxiliar del repositorio con instrucciones o contexto funcional.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: RUN.md

Elementos internos:

#### Archivo: RUN.md

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/mvnw

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/mvnw

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mvnw

Elementos internos:

#### Archivo: mvnw

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/mvnw.cmd

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/mvnw.cmd

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mvnw.cmd

Elementos internos:

#### Archivo: mvnw.cmd

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/pom.xml

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/pom.xml

Descripcion: Define el build Maven, plugins y dependencias del backend.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: pom.xml

Elementos internos:

#### Archivo: pom.xml

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-backend/scripts

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/scripts/crear-usuario-admin.sql

Descripcion: Script operativo/manual para inicializacion, limpieza o reparacion de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: crear-usuario-admin.sql

Notas:
- Inserta datos en usuario.
- Actualiza datos en nombre_completo.

Elementos internos:

#### Sql: crear-usuario-admin.sql

Script SQL ejecutable.

### sigea-backend/scripts/crear-usuario-alimentador.sql

Descripcion: Script operativo/manual para inicializacion, limpieza o reparacion de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: crear-usuario-alimentador.sql

Notas:
- Inserta datos en usuario.
- Actualiza datos en nombre_completo.

Elementos internos:

#### Sql: crear-usuario-alimentador.sql

Script SQL ejecutable.

### sigea-backend/scripts/limpiar-bd-conservar-admin-y-alimentacion.sql

Descripcion: Script operativo/manual para inicializacion, limpieza o reparacion de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: limpiar-bd-conservar-admin-y-alimentacion.sql

Notas:
- Actualiza datos en usuario.
- Elimina datos de reporte_dano.
- Elimina datos de observacion_equipo.
- Elimina datos de extension_prestamo.
- Elimina datos de detalle_prestamo.
- Elimina datos de prestamo_ambiente.
- Elimina datos de prestamo.
- Elimina datos de reserva.
- Elimina datos de transferencia.
- Elimina datos de mantenimiento.
- Elimina datos de foto_equipo.
- Elimina datos de equipo.

Elementos internos:

#### Sql: limpiar-bd-conservar-admin-y-alimentacion.sql

Script SQL ejecutable.

### sigea-backend/scripts/limpiar-datos-excepto-admin.sql

Descripcion: Script operativo/manual para inicializacion, limpieza o reparacion de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: limpiar-datos-excepto-admin.sql

Notas:
- Elimina datos de usuario.

Elementos internos:

#### Sql: limpiar-datos-excepto-admin.sql

Script SQL ejecutable.

### sigea-backend/scripts/restaurar-rol-admin.sql

Descripcion: Script operativo/manual para inicializacion, limpieza o reparacion de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: restaurar-rol-admin.sql

Notas:
- Actualiza datos en usuario.

Elementos internos:

#### Sql: restaurar-rol-admin.sql

Script SQL ejecutable.

## Modulo: sigea-backend/src

Describe una pieza de configuracion o soporte del proyecto.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/controller/AmbienteControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (IOException, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, MediaType, ResponseEntity, PreAuthorize, AuthenticationPrincipal.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (AmbienteCrearDTO, AmbienteRespuestaDTO, AmbienteService).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: AmbienteControlador

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.controller.

Elementos internos:

#### Class: AmbienteControlador

Class del archivo AmbienteControlador.java.

Campos/props principales:
- ambienteService: AmbienteService. Opcional: no.

Miembros principales:
- AmbienteControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de AmbienteControlador.java.
  Parametros: - ambienteService: AmbienteService. Opcional: no.
  Retorno: AmbienteControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.AmbienteControlador(dato);
- crearSinFoto (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody AmbienteCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<AmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearSinFoto(dato, dato);
- crearSubUbicacion (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - padreId: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody AmbienteCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<AmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearSubUbicacion(1, dato, dato);
- listarSubUbicaciones (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - padreId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<AmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarSubUbicaciones(1);
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<AmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<AmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
- listarPorInstructor (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<AmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructor(1);
- listarMiAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<AmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMiAmbiente(dato);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<AmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody AmbienteCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<AmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.activar(1, dato);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.desactivar(1, dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/dto/AmbienteCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: AmbienteCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: AmbienteCrearDTO

Class del archivo AmbienteCrearDTO.java.

Campos/props principales:
- nombre: String. Opcional: no.
- ubicacion: String. Opcional: no.
- descripcion: String. Opcional: no.
- direccion: String. Opcional: no.
- padreId: Long. Opcional: no.
- idInstructorResponsable: Long. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/dto/AmbienteRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: AmbienteRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: AmbienteRespuestaDTO

Class del archivo AmbienteRespuestaDTO.java.

Campos/props principales:
- id: long. Opcional: no.
- nombre: String. Opcional: no.
- ubicacion: String. Opcional: no.
- descripcion: String. Opcional: no.
- direccion: String. Opcional: no.
- instructorResponsableId: Long. Opcional: no.
- instructorResponsableNombre: String. Opcional: no.
- propietarioId: Long. Opcional: no.
- propietarioNombre: String. Opcional: no.
- padreId: Long. Opcional: no.
- padreNombre: String. Opcional: no.
- subUbicaciones: List<SubUbicacionResumenDTO>. Opcional: no.
- activo: Boolean. Opcional: no.
- rutaFoto: String. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/dto/SubUbicacionResumenDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: SubUbicacionResumenDTO

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: SubUbicacionResumenDTO

Resumen de una sub-ubicación (sin incluir sus propias sub-ubicaciones para evitar recursión infinita en la serialización JSON).

Campos/props principales:
- id: Long. Opcional: no.
- nombre: String. Opcional: no.
- ubicacion: String. Opcional: no.
- descripcion: String. Opcional: no.
- activo: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/entity/Ambiente.java

Descripcion: Modela datos persistentes del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, JoinColumn, ManyToOne).
- Java SE: estructuras base y utilidades de lenguaje (ArrayList, List).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Ambiente

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Ambiente

Class del archivo Ambiente.java.

Campos/props principales:
- nombre: String. Opcional: no.
- ubicacion: String. Opcional: no.
- descripcion: String. Opcional: no.
- direccion: String. Opcional: no.
- instructorResponsable: Usuario. Opcional: no.
- propietario: Usuario. Opcional: no.
- padre: Ambiente. Opcional: no.
- subUbicaciones: List<Ambiente>. Opcional: no.
- activo: Boolean. Opcional: no.
- rutaFoto: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/repository/AmbienteRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List, Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente).

Exports:
- interface: AmbienteRepository

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.repository.

Elementos internos:

#### Interface: AmbienteRepository

Interface del archivo AmbienteRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/ambiente/service/AmbienteService.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona ambientes, sububicaciones e inventario fisico asociado.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (IOException, Files, Path, Paths, StandardCopyOption).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, Service, Isolation, Transactional, MultipartFile.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (AmbienteCrearDTO, AmbienteRespuestaDTO, SubUbicacionResumenDTO, Ambiente, AmbienteRepository).

Exports:
- class: AmbienteService

Notas:
- Paquete Java: co.edu.sena.sigea.ambiente.service.

Elementos internos:

#### Class: AmbienteService

Class del archivo AmbienteService.java.

Campos/props principales:
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- rutaUploads: String. Opcional: no.

Miembros principales:
- AmbienteService (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de AmbienteService.java.
  Parametros: - ambienteRepository: AmbienteRepository. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.
  Retorno: AmbienteService.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.AmbienteService(dato, dato);
- crear (method)
  Proposito: Crea un ambiente (JSON, sin foto).
  Parametros: - dto: AmbienteCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.crear(dato, "valor");
- crearConFoto (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: AmbienteCrearDTO. Opcional: no.; - archivo: MultipartFile. Opcional: no.; - correoInstructor: String. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: IOException, OperacionNoPermitidaException, RecursoDuplicadoException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.crearConFoto(dato, dato, "valor");
  Anotaciones/metadata: @Transactional(isolation = Isolation.READ_COMMITTED)
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<AmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<AmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorInstructor (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: Long. Opcional: no.
  Retorno: List<AmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructor(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorCorreoInstructor (method)
  Proposito: Lista ambientes que el usuario actual (instructor) administra.
  Parametros: - correo: String. Opcional: no.
  Retorno: List<AmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorCorreoInstructor("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: Long. Opcional: no.; - dto: AmbienteCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato, "valor");
  Anotaciones/metadata: @Transactional
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.desactivar(1, "valor");
  Anotaciones/metadata: @Transactional
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.activar(1, "valor");
  Anotaciones/metadata: @Transactional
- verificarPropiedadInstructor (method)
  Proposito: Valida un codigo, token o condicion del negocio.
  Parametros: - ambiente: Ambiente. Opcional: no.; - correoUsuario: String. Opcional: no.; - mensajeError: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.verificarPropiedadInstructor(dato, "valor", "valor");
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AmbienteService.java.
  Parametros: - ambiente: Ambiente. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);
- crearSubUbicacion (method)
  Proposito: Crear una sub-ubicación hija de un ambiente padre.
  Parametros: - padreId: Long. Opcional: no.; - dto: AmbienteCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: AmbienteRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearSubUbicacion(1, dato, "valor");
  Anotaciones/metadata: @Transactional(isolation = Isolation.READ_COMMITTED)
- listarSubUbicaciones (method)
  Proposito: Listar sub-ubicaciones de un ambiente padre dado.
  Parametros: - padreId: Long. Opcional: no.
  Retorno: List<AmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarSubUbicaciones(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- copiarConPadre (method)
  Proposito: Asignar un equipo ya existente a una sub-ubicación.
  Parametros: - dto: AmbienteCrearDTO. Opcional: no.; - padreId: Long. Opcional: no.
  Retorno: AmbienteCrearDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.copiarConPadre(dato, 1);

### sigea-backend/src/main/java/co/edu/sena/sigea/auditoria/controller/AuditoriaControlador.java

Descripcion: Expone endpoints REST del dominio. Centraliza trazabilidad y consulta de eventos auditables.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante DateTimeFormat, ResponseEntity, PreAuthorize, GetMapping, PathVariable.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (LogAuditoriaRespuestaDTO, AuditoriaServicio).

Exports:
- class: AuditoriaControlador

Notas:
- Paquete Java: co.edu.sena.sigea.auditoria.controller.

Elementos internos:

#### Class: AuditoriaControlador

Class del archivo AuditoriaControlador.java.

Campos/props principales:
- auditoriaServicio: AuditoriaServicio. Opcional: no.

Miembros principales:
- AuditoriaControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de AuditoriaControlador.java.
  Parametros: - auditoriaServicio: AuditoriaServicio. Opcional: no.
  Retorno: AuditoriaControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.AuditoriaControlador(dato);
- listarTodos (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarTodos.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<LogAuditoriaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping -> listarTodos(...)
  Anotaciones/metadata: @GetMapping
- listarPorUsuario (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarPorUsuario.
  Parametros: - usuarioId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<LogAuditoriaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/usuario/{usuarioId}") -> listarPorUsuario(...)
  Anotaciones/metadata: @GetMapping("/usuario/{usuarioId}")
- listarPorEntidad (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarPorEntidad.
  Parametros: - entidad: @PathVariable String. Opcional: no.; - entidadId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<LogAuditoriaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/entidad/{entidad}/{entidadId}") -> listarPorEntidad(...)
  Anotaciones/metadata: @GetMapping("/entidad/{entidad}/{entidadId}")

### sigea-backend/src/main/java/co/edu/sena/sigea/auditoria/dto/LogAuditoriaRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Centraliza trazabilidad y consulta de eventos auditables.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: LogAuditoriaRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.auditoria.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: LogAuditoriaRespuestaDTO

Class del archivo LogAuditoriaRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- usuarioId: Long. Opcional: no.
- nombreUsuario: String. Opcional: no.
- accion: String. Opcional: no.
- entidadAfectada: String. Opcional: no.
- entidadId: Long. Opcional: no.
- detalles: String. Opcional: no.
- direccionIp: String. Opcional: no.
- fechaHora: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/auditoria/entity/LogAuditoria.java

Descripcion: Modela datos persistentes del dominio. Centraliza trazabilidad y consulta de eventos auditables.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, GeneratedValue, GenerationType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: LogAuditoria

Notas:
- Paquete Java: co.edu.sena.sigea.auditoria.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: LogAuditoria

Class del archivo LogAuditoria.java.

Campos/props principales:
- id: Long. Opcional: no.
- usuario: Usuario. Opcional: no.
- accion: String. Opcional: no.
- entidadAfectada: String. Opcional: no.
- entidadId: Long. Opcional: no.
- detalles: String. Opcional: no.
- direccionIp: String. Opcional: no.
- fechaHora: LocalDateTime. Opcional: no.

Miembros principales:
- alCrear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de LogAuditoria.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.alCrear();
  Anotaciones/metadata: @jakarta.persistence.PrePersist

### sigea-backend/src/main/java/co/edu/sena/sigea/auditoria/repository/LogAuditoriaRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Centraliza trazabilidad y consulta de eventos auditables.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (LogAuditoria).

Exports:
- interface: LogAuditoriaRepository

Notas:
- Paquete Java: co.edu.sena.sigea.auditoria.repository.

Elementos internos:

#### Interface: LogAuditoriaRepository

Interface del archivo LogAuditoriaRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/auditoria/service/AuditoriaServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Centraliza trazabilidad y consulta de eventos auditables.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (LogAuditoriaRespuestaDTO, LogAuditoria, LogAuditoriaRepository, Usuario).

Exports:
- class: AuditoriaServicio

Notas:
- Paquete Java: co.edu.sena.sigea.auditoria.service.

Elementos internos:

#### Class: AuditoriaServicio

Class del archivo AuditoriaServicio.java.

Campos/props principales:
- logAuditoriaRepository: LogAuditoriaRepository. Opcional: no.

Miembros principales:
- AuditoriaServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de AuditoriaServicio.java.
  Parametros: - logAuditoriaRepository: LogAuditoriaRepository. Opcional: no.
  Retorno: AuditoriaServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.AuditoriaServicio(dato);
- registrar (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - usuario: Usuario. Opcional: no.; - accion: String. Opcional: no.; - entidadAfectada: String. Opcional: no.; - entidadId: Long. Opcional: no.; - detalles: String. Opcional: no.; - direccionIp: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.registrar(dato, "valor", "valor", 1, "valor", "valor");
  Anotaciones/metadata: @Transactional
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<LogAuditoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: Long. Opcional: no.
  Retorno: List<LogAuditoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEntidad (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - entidad: String. Opcional: no.; - entidadId: Long. Opcional: no.
  Retorno: List<LogAuditoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEntidad("valor", 1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorRangoFechas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - desde: LocalDateTime. Opcional: no.; - hasta: LocalDateTime. Opcional: no.
  Retorno: List<LogAuditoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorRangoFechas(fecha, fecha);
  Anotaciones/metadata: @Transactional(readOnly = true)
- mapear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AuditoriaServicio.java.
  Parametros: - log: LogAuditoria. Opcional: no.
  Retorno: LogAuditoriaRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapear(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/controller/CategoriaControlador.java

Descripcion: Expone endpoints REST del dominio. Administra catalogos de categorias de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, DeleteMapping, GetMapping, PathVariable.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (CategoriaCrearDTO, CategoriaRespuestaDTO, CategoriaServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: CategoriaControlador

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.controller.

Elementos internos:

#### Class: CategoriaControlador

Class del archivo CategoriaControlador.java.

Campos/props principales:
- categoriaServicio: CategoriaServicio. Opcional: no.

Miembros principales:
- CategoriaControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de CategoriaControlador.java.
  Parametros: - categoriaServicio: CategoriaServicio. Opcional: no.
  Retorno: CategoriaControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.CategoriaControlador(dato);
- crear (method)
  Proposito: Atiende una operacion HTTP POST para crear.
  Parametros: - dto: @Valid @RequestBody CategoriaCrearDTO. Opcional: no.
  Retorno: ResponseEntity<CategoriaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping -> crear(...)
  Anotaciones/metadata: @PostMapping
- listarActivas (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarActivas.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<CategoriaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping -> listarActivas(...)
  Anotaciones/metadata: @GetMapping
- listarTodas (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarTodas.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<CategoriaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/todas") -> listarTodas(...)
  Anotaciones/metadata: @GetMapping("/todas")
- buscarPorId (method)
  Proposito: Atiende una consulta HTTP GET para la operacion buscarPorId.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<CategoriaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/{id}") -> buscarPorId(...)
  Anotaciones/metadata: @GetMapping("/{id}")
- actualizar (method)
  Proposito: Atiende una actualizacion HTTP PUT relacionada con actualizar.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody CategoriaCrearDTO. Opcional: no.
  Retorno: ResponseEntity<CategoriaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PutMapping("/{id}") -> actualizar(...)
  Anotaciones/metadata: @PutMapping("/{id}")
- eliminar (method)
  Proposito: Atiende una operacion HTTP DELETE para eliminar.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @DeleteMapping("/{id}") -> eliminar(...)
  Anotaciones/metadata: @DeleteMapping("/{id}")

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/dto/CategoriaCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Administra catalogos de categorias de equipos.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: CategoriaCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: CategoriaCrearDTO

Class del archivo CategoriaCrearDTO.java.

Campos/props principales:
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/dto/CategoriaRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Administra catalogos de categorias de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: CategoriaRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: CategoriaRespuestaDTO

Class del archivo CategoriaRespuestaDTO.java.

Campos/props principales:
- id: long. Opcional: no.
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- activo: Boolean. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/entity/Categoria.java

Descripcion: Modela datos persistentes del dominio. Administra catalogos de categorias de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, Table).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Categoria

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Categoria

Class del archivo Categoria.java.

Campos/props principales:
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- activo: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/repository/CategoriaRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Administra catalogos de categorias de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List, Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Categoria).

Exports:
- interface: CategoriaRepository

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.repository.

Elementos internos:

#### Interface: CategoriaRepository

Interface del archivo CategoriaRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/categoria/service/CategoriaServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Administra catalogos de categorias de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (CategoriaCrearDTO, CategoriaRespuestaDTO, Categoria, CategoriaRepository, RecursoDuplicadoException).

Exports:
- class: CategoriaServicio

Notas:
- Paquete Java: co.edu.sena.sigea.categoria.service.

Elementos internos:

#### Class: CategoriaServicio

Class del archivo CategoriaServicio.java.

Campos/props principales:
- categoriaRepository: CategoriaRepository. Opcional: no.

Miembros principales:
- CategoriaServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de CategoriaServicio.java.
  Parametros: - categoriaRepository: CategoriaRepository. Opcional: no.
  Retorno: CategoriaServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.CategoriaServicio(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: CategoriaCrearDTO. Opcional: no.
  Retorno: CategoriaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException
  Ejemplo de uso: var resultado = servicio.crear(dato);
  Anotaciones/metadata: @Transactional
- listarActivas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<CategoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivas();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarTodas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<CategoriaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodas();
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: CategoriaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: Long. Opcional: no.; - dto: CategoriaCrearDTO. Opcional: no.
  Retorno: CategoriaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
  Anotaciones/metadata: @Transactional
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CategoriaServicio.java.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.eliminar(1);
  Anotaciones/metadata: @Transactional
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CategoriaServicio.java.
  Parametros: - categoria: Categoria. Opcional: no.
  Retorno: CategoriaRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/common/dto/ErrorRespuesta.java

Descripcion: Define contratos de entrada/salida del dominio. Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Otros: dependencias auxiliares (JsonInclude, AllArgsConstructor, Builder, Getter, Setter).

Exports:
- class: ErrorRespuesta

Notas:
- Paquete Java: co.edu.sena.sigea.common.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ErrorRespuesta

Class del archivo ErrorRespuesta.java.

Campos/props principales:
- timestamp: LocalDateTime. Opcional: no.
- status: int. Opcional: no.
- error: String. Opcional: no.
- message: String. Opcional: no.
- ruta: String. Opcional: no.
- detalles: List<String>. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/common/entity/EntidadBase.java

Descripcion: Modela datos persistentes del dominio. Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Column, GeneratedValue, GenerationType, Id, MappedSuperclass).
- Otros: dependencias auxiliares (Getter, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: EntidadBase

Notas:
- Paquete Java: co.edu.sena.sigea.common.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: EntidadBase

Class del archivo EntidadBase.java.

Campos/props principales:
- id: Long. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

Miembros principales:
- onCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EntidadBase.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.onCreate();
  Anotaciones/metadata: @PrePersist
- onUpdate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EntidadBase.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.onUpdate();
  Anotaciones/metadata: @PreUpdate

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoAprobacion.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoAprobacion

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoAprobacion

Enum del archivo EstadoAprobacion.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoCondicion.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoCondicion

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoCondicion

Enum del archivo EstadoCondicion.java.

Valores expuestos:
- EXCELENTE, BUENO, REGULAR, MALO

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoEnvio.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoEnvio

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoEnvio

Enum del archivo EstadoEnvio.java.

Valores expuestos:
- PENDIENTE, ENVIADA, FALLIDA

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoEquipo.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoEquipo

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoEquipo

Enum del archivo EstadoEquipo.java.

Valores expuestos:
- ACTIVO, EN_MANTENIMIENTO

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoExtension.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoExtension

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoExtension

Enum del archivo EstadoExtension.java.

Valores expuestos:
- SOLICITADA, APROBADA, RECHAZADA

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoPrestamo.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoPrestamo

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoPrestamo

Enum del archivo EstadoPrestamo.java.

Valores expuestos:
- SOLICITADO, APROBADO, RECHAZADO, ACTIVO, DEVUELTO, EN_MORA, CANCELADO

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/EstadoReserva.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoReserva

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: EstadoReserva

Enum del archivo EstadoReserva.java.

Valores expuestos:
- ACTIVA, CANCELADA, COMPLETADA, EXPIRADA, PRESTADO

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/MedioEnvio.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: MedioEnvio

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: MedioEnvio

Enum del archivo MedioEnvio.java.

Valores expuestos:
- EMAIL, INTERNA

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/Rol.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: Rol

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: Rol

Enum del archivo Rol.java.

Valores expuestos:
- ADMINISTRADOR, ALIMENTADOR_EQUIPOS, INSTRUCTOR, APRENDIZ, FUNCIONARIO, USUARIO_ESTANDAR

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/TipoDocumento.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: TipoDocumento

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: TipoDocumento

Enum del archivo TipoDocumento.java.

Valores expuestos:
- CC, TI, CE, PP, PEP

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/TipoMantenimiento.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: TipoMantenimiento

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: TipoMantenimiento

Enum del archivo TipoMantenimiento.java.

Valores expuestos:
- PREVENTIVO, CORRECTIVO

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/TipoNotificacion.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: TipoNotificacion

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: TipoNotificacion

Enum del archivo TipoNotificacion.java.

Valores expuestos:
- RECORDATORIO_VENCIMIENTO, MORA, STOCK_BAJO, SOLICITUD_PRESTAMO, RESERVA_CREADA, RESERVA_CANCELADA, RESERVA_EXPIRADA, EQUIPO_RECOGIDO, PRESTAMO_SALIDA, PRESTAMO_CANCELADO, PRESTAMO_DEVUELTO, SOLICITUD_PRESTAMO_AMBIENTE, PRESTAMO_AMBIENTE_APROBADO, PRESTAMO_AMBIENTE_RECHAZADO, PRESTAMO_AMBIENTE_CANCELADO, PRESTAMO_AMBIENTE_DEVUELTO, GENERAL

### sigea-backend/src/main/java/co/edu/sena/sigea/common/enums/TipoUsoEquipo.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: TipoUsoEquipo

Notas:
- Paquete Java: co.edu.sena.sigea.common.enums.

Elementos internos:

#### Enum: TipoUsoEquipo

Enum del archivo TipoUsoEquipo.java.

Valores expuestos:
- CONSUMIBLE, NO_CONSUMIBLE

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/ManejadorGlobalExcepciones.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, MethodArgumentNotValidException, ExceptionHandler, RestControllerAdvice.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ErrorRespuesta).
- Jakarta: validaciones y contratos web/JPA (HttpServletRequest).

Exports:
- class: ManejadorGlobalExcepciones

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: ManejadorGlobalExcepciones

Class del archivo ManejadorGlobalExcepciones.java.

Miembros principales:
- manejarRecursoNoEncontrado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: RecursoNoEncontradoException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarRecursoNoEncontrado(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(RecursoNoEncontradoException.class)
- manejarRecursoDuplicado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: RecursoDuplicadoException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarRecursoDuplicado(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(RecursoDuplicadoException.class)
- manejarOperacionNoPermitida (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: OperacionNoPermitidaException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarOperacionNoPermitida(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(OperacionNoPermitidaException.class)
- manejarServicioCorreo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: ServicioCorreoException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarServicioCorreo(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(ServicioCorreoException.class)
- manejarServicioWhatsapp (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: ServicioWhatsappException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarServicioWhatsapp(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(ServicioWhatsappException.class)
- manejarValidacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ManejadorGlobalExcepciones.java.
  Parametros: - ex: MethodArgumentNotValidException. Opcional: no.; - request: HttpServletRequest. Opcional: no.
  Retorno: ResponseEntity<ErrorRespuesta>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarValidacion(dato, dato);
  Anotaciones/metadata: @ExceptionHandler(MethodArgumentNotValidException.class)

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/OperacionNoPermitidaException.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- class: OperacionNoPermitidaException

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: OperacionNoPermitidaException

Class del archivo OperacionNoPermitidaException.java.

Miembros principales:
- OperacionNoPermitidaException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de OperacionNoPermitidaException.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: OperacionNoPermitidaException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.OperacionNoPermitidaException("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/RecursoDuplicadoException.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- class: RecursoDuplicadoException

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: RecursoDuplicadoException

Class del archivo RecursoDuplicadoException.java.

Miembros principales:
- RecursoDuplicadoException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de RecursoDuplicadoException.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: RecursoDuplicadoException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.RecursoDuplicadoException("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/RecursoNoEncontradoException.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- class: RecursoNoEncontradoException

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: RecursoNoEncontradoException

Class del archivo RecursoNoEncontradoException.java.

Miembros principales:
- RecursoNoEncontradoException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de RecursoNoEncontradoException.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: RecursoNoEncontradoException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.RecursoNoEncontradoException("valor");
- RecursoNoEncontradoException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de RecursoNoEncontradoException.java.
  Parametros: - entidad: String. Opcional: no.; - id: long. Opcional: no.
  Retorno: RecursoNoEncontradoException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.RecursoNoEncontradoException("valor", 1);

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/ServicioCorreoException.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- class: ServicioCorreoException

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: ServicioCorreoException

Class del archivo ServicioCorreoException.java.

Miembros principales:
- ServicioCorreoException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de ServicioCorreoException.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: ServicioCorreoException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.ServicioCorreoException("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/common/exception/ServicioWhatsappException.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- class: ServicioWhatsappException

Notas:
- Paquete Java: co.edu.sena.sigea.common.exception.

Elementos internos:

#### Class: ServicioWhatsappException

Class del archivo ServicioWhatsappException.java.

Miembros principales:
- ServicioWhatsappException (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de ServicioWhatsappException.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: ServicioWhatsappException.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.ServicioWhatsappException("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/common/util/FechasUtil.java

Descripcion: Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (DayOfWeek, LocalDate).

Exports:
- class: FechasUtil

Notas:
- Paquete Java: co.edu.sena.sigea.common.util.

Elementos internos:

#### Class: FechasUtil

Class del archivo FechasUtil.java.

Miembros principales:
- FechasUtil (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de FechasUtil.java.
  Parametros: Sin parametros.
  Retorno: FechasUtil.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.FechasUtil();
- sumarDiasHabiles (method)
  Proposito: Suma n días hábiles a una fecha (excluye sábado y domingo).
  Parametros: - fechaInicio: LocalDate. Opcional: no.; - diasHabiles: int. Opcional: no.
  Retorno: LocalDate.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.sumarDiasHabiles(fecha, 1);

### sigea-backend/src/main/java/co/edu/sena/sigea/configuracion/entity/Configuracion.java

Descripcion: Modela datos persistentes del dominio. Declara configuracion transversal y tareas programadas.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, Table).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Configuracion

Notas:
- Paquete Java: co.edu.sena.sigea.configuracion.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Configuracion

Class del archivo Configuracion.java.

Campos/props principales:
- clave: String. Opcional: no.
- valor: String. Opcional: no.
- tipo: String. Opcional: no.
- descripcion: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/configuracion/repository/ConfiguracionRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Declara configuracion transversal y tareas programadas.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Configuracion).

Exports:
- interface: ConfiguracionRepository

Notas:
- Paquete Java: co.edu.sena.sigea.configuracion.repository.

Elementos internos:

#### Interface: ConfiguracionRepository

Interface del archivo ConfiguracionRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/configuracion/TareasProgramadasConfig.java

Descripcion: Declara configuracion transversal y tareas programadas.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante Configuration, EnableScheduling.

Exports:
- class: TareasProgramadasConfig

Notas:
- Paquete Java: co.edu.sena.sigea.configuracion.

Elementos internos:

#### Class: TareasProgramadasConfig

Class del archivo TareasProgramadasConfig.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/configuracion/WebMvcConfig.java

Descripcion: Declara configuracion transversal y tareas programadas.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, Configuration, ResourceHandlerRegistry, WebMvcConfigurer.

Exports:
- class: WebMvcConfig

Notas:
- Paquete Java: co.edu.sena.sigea.configuracion.

Elementos internos:

#### Class: WebMvcConfig

Configura el servidor para servir archivos subidos (fotos de equipos).

Campos/props principales:
- uploadsPath: String. Opcional: no.

Miembros principales:
- addResourceHandlers (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WebMvcConfig.java.
  Parametros: - registry: ResourceHandlerRegistry. Opcional: no.
  Retorno: void.
  Efectos secundarios: Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.addResourceHandlers(dato);
  Anotaciones/metadata: @Override

### sigea-backend/src/main/java/co/edu/sena/sigea/dashboard/controller/DashboardControlador.java

Descripcion: Expone endpoints REST del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante ResponseEntity, PreAuthorize, GetMapping, RequestMapping, RestController.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (DashboardEstadisticasDTO, EquiposPorCategoriaDTO, PrestamosPorMesDTO, DashboardServicio).

Exports:
- class: DashboardControlador

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.controller.

Elementos internos:

#### Class: DashboardControlador

Class del archivo DashboardControlador.java.

Campos/props principales:
- dashboardServicio: DashboardServicio. Opcional: no.

Miembros principales:
- DashboardControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de DashboardControlador.java.
  Parametros: - dashboardServicio: DashboardServicio. Opcional: no.
  Retorno: DashboardControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.DashboardControlador(dato);
- obtenerEstadisticas (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<DashboardEstadisticasDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.obtenerEstadisticas();
- prestamosPorMes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de DashboardControlador.java.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<PrestamosPorMesDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.prestamosPorMes();
- equiposPorCategoria (method)
  Proposito: Resuelve una responsabilidad puntual dentro de DashboardControlador.java.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<EquiposPorCategoriaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.equiposPorCategoria();

### sigea-backend/src/main/java/co/edu/sena/sigea/dashboard/dto/DashboardEstadisticasDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: DashboardEstadisticasDTO

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: DashboardEstadisticasDTO

Class del archivo DashboardEstadisticasDTO.java.

Campos/props principales:
- totalEquipos: long. Opcional: no.
- equiposActivos: long. Opcional: no.
- totalCategorias: long. Opcional: no.
- totalAmbientes: long. Opcional: no.
- totalUsuarios: long. Opcional: no.
- prestamosSolicitados: long. Opcional: no.
- prestamosActivos: long. Opcional: no.
- prestamosEnMora: long. Opcional: no.
- prestamosDevueltos: long. Opcional: no.
- reservasActivas: long. Opcional: no.
- mantenimientosEnCurso: long. Opcional: no.
- totalTransferencias: long. Opcional: no.
- equiposStockBajo: long. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/dashboard/dto/EquiposPorCategoriaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: EquiposPorCategoriaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: EquiposPorCategoriaDTO

Class del archivo EquiposPorCategoriaDTO.java.

Campos/props principales:
- categoriaNombre: String. Opcional: no.
- cantidad: long. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/dashboard/dto/PrestamosPorMesDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: PrestamosPorMesDTO

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamosPorMesDTO

Class del archivo PrestamosPorMesDTO.java.

Campos/props principales:
- mes: String. Opcional: no.
- cantidad: long. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/dashboard/service/DashboardServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, Month, Year, TextStyle, ArrayList).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamo, EstadoReserva, DashboardEstadisticasDTO, EquiposPorCategoriaDTO, PrestamosPorMesDTO).

Exports:
- class: DashboardServicio

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.service.

Elementos internos:

#### Class: DashboardServicio

Class del archivo DashboardServicio.java.

Campos/props principales:
- equipoRepository: EquipoRepository. Opcional: no.
- categoriaRepository: CategoriaRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- transferenciaRepository: TransferenciaRepository. Opcional: no.

Miembros principales:
- DashboardServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de DashboardServicio.java.
  Parametros: - equipoRepository: EquipoRepository. Opcional: no.; - categoriaRepository: CategoriaRepository. Opcional: no.; - ambienteRepository: AmbienteRepository. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.; - prestamoRepository: PrestamoRepository. Opcional: no.; - reservaRepository: ReservaRepository. Opcional: no.; - mantenimientoRepository: MantenimientoRepository. Opcional: no.; - transferenciaRepository: TransferenciaRepository. Opcional: no.
  Retorno: DashboardServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.DashboardServicio(dato, dato, dato, dato, dato, dato, dato, dato);
- obtenerEstadisticas (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: Sin parametros.
  Retorno: DashboardEstadisticasDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.obtenerEstadisticas();
  Anotaciones/metadata: @Transactional(readOnly = true)
- prestamosPorMes (method)
  Proposito: Últimos 6 meses con cantidad de préstamos solicitados por mes.
  Parametros: Sin parametros.
  Retorno: List<PrestamosPorMesDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.prestamosPorMes();
  Anotaciones/metadata: @Transactional(readOnly = true)
- equiposPorCategoria (method)
  Proposito: Cantidad de equipos activos por categoría (para gráfico de distribución).
  Parametros: Sin parametros.
  Retorno: List<EquiposPorCategoriaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.equiposPorCategoria();
  Anotaciones/metadata: @Transactional(readOnly = true)

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/controller/EquipoControlador.java

Descripcion: Expone endpoints REST del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (IOException, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, MediaType, ResponseEntity, PreAuthorize, AuthenticationPrincipal.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, EquipoCrearDTO, EquipoRespuestaDTO, FotoEquipoRespuestaDTO, EquipoServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: EquipoControlador

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.controller.

Elementos internos:

#### Class: EquipoControlador

Class del archivo EquipoControlador.java.

Campos/props principales:
- equipoServicio: EquipoServicio. Opcional: no.

Miembros principales:
- EquipoControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - equipoServicio: EquipoServicio. Opcional: no.
  Retorno: EquipoControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.EquipoControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody EquipoCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato, dato);
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
- listarPorCategoria (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - categoriaId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorCategoria(1);
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorAmbiente(1);
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: @PathVariable EstadoEquipo. Opcional: no.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
- listarMiInventario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMiInventario(dato);
- listarMisEquiposComoPropietario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<EquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMisEquiposComoPropietario(dato);
- recuperarEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.recuperarEquipo(1, dato);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody EquipoCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato, dato);
- cambiarEstado (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: @PathVariable Long. Opcional: no.; - nuevoEstado: @PathVariable EstadoEquipo. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cambiarEstado(1, dato, dato);
- darDeBaja (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.darDeBaja(1, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.activar(1, dato);
- eliminarFoto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - fotoId: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: IOException
  Ejemplo de uso: var resultado = servicio.eliminarFoto(1, 1, dato);
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.eliminar(1, dato);
- asignarSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - subUbicacionId: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.asignarSubUbicacion(1, 1, dato);
- quitarSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<EquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.quitarSubUbicacion(1, dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/dto/EquipoCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoUsoEquipo).
- Jakarta: validaciones y contratos web/JPA (Max, Min, NotBlank, NotNull, Pattern).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: EquipoCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: EquipoCrearDTO

Class del archivo EquipoCrearDTO.java.

Campos/props principales:
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- placa: String. Opcional: no.
- serial: String. Opcional: no.
- modelo: String. Opcional: no.
- marcaId: Long. Opcional: no.
- estadoEquipoEscala: Integer. Opcional: no.
- codigoUnico: String. Opcional: no.
- categoriaId: Long. Opcional: no.
- ambienteId: Long. Opcional: no.
- subUbicacionId: Long. Opcional: no.
- propietarioId: Long. Opcional: no.
- cantidadTotal: Integer. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- umbralMinimo: Integer. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/dto/EquipoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, TipoUsoEquipo).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: EquipoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: EquipoRespuestaDTO

Class del archivo EquipoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- placa: String. Opcional: no.
- serial: String. Opcional: no.
- modelo: String. Opcional: no.
- marcaId: Long. Opcional: no.
- marcaNombre: String. Opcional: no.
- estadoEquipoEscala: Integer. Opcional: no.
- codigoUnico: String. Opcional: no.
- categoriaId: Long. Opcional: no.
- categoriaNombre: String. Opcional: no.
- ambienteId: Long. Opcional: no.
- ambienteNombre: String. Opcional: no.
- subUbicacionId: Long. Opcional: no.
- subUbicacionNombre: String. Opcional: no.
- propietarioId: Long. Opcional: no.
- propietarioNombre: String. Opcional: no.
- inventarioActualInstructorId: Long. Opcional: no.
- inventarioActualInstructorNombre: String. Opcional: no.
- estado: EstadoEquipo. Opcional: no.
- cantidadTotal: Integer. Opcional: no.
- cantidadDisponible: Integer. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- umbralMinimo: Integer. Opcional: no.
- activo: Boolean. Opcional: no.
- fotos: List<FotoEquipoRespuestaDTO>. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/dto/FotoEquipoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: FotoEquipoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: FotoEquipoRespuestaDTO

Class del archivo FotoEquipoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- nombreArchivo: String. Opcional: no.
- rutaArchivo: String. Opcional: no.
- tamanoBytes: Long. Opcional: no.
- fechaSubida: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/entity/Equipo.java

Descripcion: Modela datos persistentes del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, Categoria, EntidadBase, EstadoEquipo, TipoUsoEquipo).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Equipo

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Equipo

Class del archivo Equipo.java.

Campos/props principales:
- nombre: String. Opcional: no.
- placa: String. Opcional: no.
- serial: String. Opcional: no.
- modelo: String. Opcional: no.
- marca: Marca. Opcional: no.
- estadoEquipoEscala: Integer. Opcional: no.
- descripcion: String. Opcional: no.
- codigoUnico: String. Opcional: no.
- categoria: Categoria. Opcional: no.
- estado: EstadoEquipo. Opcional: no.
- cantidadTotal: Integer. Opcional: no.
- cantidadDisponible: Integer. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- ambiente: Ambiente. Opcional: no.
- propietario: Usuario. Opcional: no.
- inventarioActualInstructor: Usuario. Opcional: no.
- subUbicacion: Ambiente. Opcional: no.
- umbralMinimo: Integer. Opcional: no.
- activo: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/entity/FotoEquipo.java

Descripcion: Modela datos persistentes del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, JoinColumn, ManyToOne).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: FotoEquipo

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: FotoEquipo

Class del archivo FotoEquipo.java.

Campos/props principales:
- equipo: Equipo. Opcional: no.
- nombreArchivo: String. Opcional: no.
- rutaArchivo: String. Opcional: no.
- tamanoBytes: Long. Opcional: no.
- fechaSubida: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/repository/EquipoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List, Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, Equipo).

Exports:
- interface: EquipoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.repository.

Elementos internos:

#### Interface: EquipoRepository

Interface del archivo EquipoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/repository/FotoEquipoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (FotoEquipo).

Exports:
- interface: FotoEquipoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.repository.

Elementos internos:

#### Interface: FotoEquipoRepository

Interface del archivo FotoEquipoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/equipo/service/EquipoServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Modela inventario, fotos, estados y disponibilidad de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (IOException, Files, Path, Paths, StandardCopyOption).
- Otros: dependencias auxiliares (Logger, LoggerFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, Service, Isolation, Transactional, MultipartFile.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, AmbienteRepository, Categoria, CategoriaRepository, EstadoEquipo).

Exports:
- class: EquipoServicio

Notas:
- Paquete Java: co.edu.sena.sigea.equipo.service.

Elementos internos:

#### Class: EquipoServicio

Class del archivo EquipoServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- fotoEquipoRepository: FotoEquipoRepository. Opcional: no.
- categoriaRepository: CategoriaRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- transferenciaRepository: TransferenciaRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- marcaRepository: MarcaRepository. Opcional: no.
- rutaUploads: String. Opcional: no.

Miembros principales:
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: EquipoCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.crear(dato, "valor");
  Anotaciones/metadata: @Transactional(isolation = Isolation.READ_COMMITTED)
- generarCodigoUnico (method)
  Proposito: Genera un código único automático (ej: SIGEA-EQ-20260310143022).
  Parametros: Sin parametros.
  Retorno: String.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarCodigoUnico();
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorCategoria (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - categoriaId: Long. Opcional: no.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarPorCategoria(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: Long. Opcional: no.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarPorAmbiente(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: EstadoEquipo. Opcional: no.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMiInventario (method)
  Proposito: Equipos que actualmente están en el inventario del instructor autenticado.
  Parametros: - correoUsuario: String. Opcional: no.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMiInventario("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMisEquiposComoPropietario (method)
  Proposito: Todos los equipos de los que el instructor autenticado es propietario (estén donde estén).
  Parametros: - correoUsuario: String. Opcional: no.
  Retorno: List<EquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMisEquiposComoPropietario("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- recuperarEquipo (method)
  Proposito: El propietario recupera un equipo transferido, devolviéndolo a su inventario.
  Parametros: - equipoId: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.recuperarEquipo(1, "valor");
  Anotaciones/metadata: @Transactional
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: Long. Opcional: no.; - dto: EquipoCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato, "valor");
  Anotaciones/metadata: @Transactional
- asignarSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipoId: Long. Opcional: no.; - subUbicacionId: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.asignarSubUbicacion(1, 1, "valor");
  Anotaciones/metadata: @Transactional(isolation = Isolation.READ_COMMITTED)
- cambiarEstado (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: Long. Opcional: no.; - nuevoEstado: EstadoEquipo. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.cambiarEstado(1, dato, "valor");
- darDeBaja (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.darDeBaja(1, "valor");
  Anotaciones/metadata: @Transactional
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.activar(1, "valor");
  Anotaciones/metadata: @Transactional
- eliminar (method)
  Proposito: Elimina permanentemente un equipo.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.eliminar(1, "valor");
  Anotaciones/metadata: @Transactional
- subirFoto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipoId: Long. Opcional: no.; - archivo: MultipartFile. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: FotoEquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: IOException, OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.subirFoto(1, dato, "valor");
  Anotaciones/metadata: @Transactional
- eliminarFoto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipoId: Long. Opcional: no.; - fotoId: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.eliminarFoto(1, 1, "valor");
  Anotaciones/metadata: @Transactional
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipo: Equipo. Opcional: no.
  Retorno: EquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);
- convertirFotoADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - foto: FotoEquipo. Opcional: no.
  Retorno: FotoEquipoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirFotoADTO(dato);
- resolverPropietarioParaCreacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - dto: EquipoCrearDTO. Opcional: no.; - usuarioActual: Usuario. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.resolverPropietarioParaCreacion(dato, dato);
- obtenerUsuarioActual (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: - correoUsuario: String. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.obtenerUsuarioActual("valor");
- validarPermisoGestion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipo: Equipo. Opcional: no.; - usuarioActual: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.validarPermisoGestion(dato, dato);
- validarPermisoGestion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - equipo: Equipo. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.validarPermisoGestion(dato, "valor");
- resolverSubUbicacionParaActualizacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - subUbicacionId: Long. Opcional: no.; - ambiente: Ambiente. Opcional: no.
  Retorno: Ambiente.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.resolverSubUbicacionParaActualizacion(1, dato);
- resolverPropietarioParaActualizacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - dto: EquipoCrearDTO. Opcional: no.; - usuarioActual: Usuario. Opcional: no.; - propietarioActual: Usuario. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverPropietarioParaActualizacion(dato, dato, dato);
- validarPermisoCambioPropietario (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - usuarioActual: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.validarPermisoCambioPropietario(dato);
- usuarioPuedeCambiarPropietario (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - usuarioActual: Usuario. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.usuarioPuedeCambiarPropietario(dato);
- resolverPropietarioSeleccionado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de EquipoServicio.java.
  Parametros: - propietarioId: Long. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.resolverPropietarioSeleccionado(1);
- obtenerSuperAdminActivo (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: Sin parametros.
  Retorno: Usuario.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.obtenerSuperAdminActivo();

### sigea-backend/src/main/java/co/edu/sena/sigea/evaluacion/service/EvaluacionMensualServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Ejecuta procesos de evaluacion periodica o tareas automaticas.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalDateTime, List, Map).
- Otros: dependencias auxiliares (Logger, LoggerFactory, RequiredArgsConstructor).
- Spring: soporte de web, seguridad, DI o persistencia mediante Scheduled, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, Rol, TipoMantenimiento, Equipo, EquipoRepository).

Exports:
- class: EvaluacionMensualServicio

Notas:
- Paquete Java: co.edu.sena.sigea.evaluacion.service.

Elementos internos:

#### Class: EvaluacionMensualServicio

Servicio que ejecuta automáticamente el primer día de cada mes la evaluación del estado de todos los equipos activos a partir del promedio de sus observaciones de devolución del mes anterior.

Campos/props principales:
- log: Logger. Opcional: no.
- UMBRAL_ESTADO_CRITICO: double. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- observacionRepository: ObservacionEquipoRepository. Opcional: no.
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- correoServicio: CorreoServicio. Opcional: no.

Miembros principales:
- generarMantenimientoCorrectivo (method)
  Proposito: Construye una salida derivada, reporte o token.
  Parametros: - equipo: Equipo. Opcional: no.; - promedioEstado: double. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarMantenimientoCorrectivo(dato, dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/controller/MantenimientoControlador.java

Descripcion: Expone endpoints REST del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, DeleteMapping, GetMapping.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoMantenimiento, MantenimientoCerrarDTO, MantenimientoCrearDTO, MantenimientoRespuestaDTO, MantenimientoServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: MantenimientoControlador

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.controller.

Elementos internos:

#### Class: MantenimientoControlador

Class del archivo MantenimientoControlador.java.

Campos/props principales:
- mantenimientoServicio: MantenimientoServicio. Opcional: no.

Miembros principales:
- MantenimientoControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoControlador.java.
  Parametros: - mantenimientoServicio: MantenimientoServicio. Opcional: no.
  Retorno: MantenimientoControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.MantenimientoControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody MantenimientoCrearDTO. Opcional: no.
  Retorno: ResponseEntity<MantenimientoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato);
- cerrar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody MantenimientoCerrarDTO. Opcional: no.
  Retorno: ResponseEntity<MantenimientoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cerrar(1, dato);
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<MantenimientoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
- listarPorTipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - tipo: @PathVariable TipoMantenimiento. Opcional: no.
  Retorno: ResponseEntity<List<MantenimientoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorTipo(dato);
- listarEnCurso (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<MantenimientoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarEnCurso();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<MantenimientoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody MantenimientoCrearDTO. Opcional: no.
  Retorno: ResponseEntity<MantenimientoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.eliminar(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/dto/MantenimientoCerrarDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate).
- Jakarta: validaciones y contratos web/JPA (NotNull, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: MantenimientoCerrarDTO

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: MantenimientoCerrarDTO

Class del archivo MantenimientoCerrarDTO.java.

Campos/props principales:
- fechaFin: LocalDate. Opcional: no.
- observaciones: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/dto/MantenimientoCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoMantenimiento).
- Jakarta: validaciones y contratos web/JPA (NotBlank, NotNull, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: MantenimientoCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: MantenimientoCrearDTO

Class del archivo MantenimientoCrearDTO.java.

Campos/props principales:
- equipoId: Long. Opcional: no.
- tipo: TipoMantenimiento. Opcional: no.
- descripcion: String. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- responsable: String. Opcional: no.
- observaciones: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/dto/MantenimientoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoMantenimiento).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: MantenimientoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: MantenimientoRespuestaDTO

Class del archivo MantenimientoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- equipoId: Long. Opcional: no.
- nombreEquipo: String. Opcional: no.
- codigoEquipo: String. Opcional: no.
- tipo: TipoMantenimiento. Opcional: no.
- descripcion: String. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- responsable: String. Opcional: no.
- observaciones: String. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/entity/Mantenimiento.java

Descripcion: Modela datos persistentes del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, TipoMantenimiento, Equipo).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Mantenimiento

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Mantenimiento

Class del archivo Mantenimiento.java.

Campos/props principales:
- equipo: Equipo. Opcional: no.
- tipo: TipoMantenimiento. Opcional: no.
- descripcion: String. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- responsable: String. Opcional: no.
- observaciones: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/repository/MantenimientoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoMantenimiento, Mantenimiento).

Exports:
- interface: MantenimientoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.repository.

Elementos internos:

#### Interface: MantenimientoRepository

Interface del archivo MantenimientoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/mantenimiento/service/MantenimientoServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, TipoUsoEquipo, TipoMantenimiento, OperacionNoPermitidaException, RecursoNoEncontradoException).

Exports:
- class: MantenimientoServicio

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.service.

Elementos internos:

#### Class: MantenimientoServicio

Class del archivo MantenimientoServicio.java.

Campos/props principales:
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.

Miembros principales:
- MantenimientoServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicio.java.
  Parametros: - mantenimientoRepository: MantenimientoRepository. Opcional: no.; - equipoRepository: EquipoRepository. Opcional: no.
  Retorno: MantenimientoServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.MantenimientoServicio(dato, dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: MantenimientoCrearDTO. Opcional: no.
  Retorno: MantenimientoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.crear(dato);
- cerrar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicio.java.
  Parametros: - id: Long. Opcional: no.; - dto: MantenimientoCerrarDTO. Opcional: no.
  Retorno: MantenimientoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.cerrar(1, dato);
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<MantenimientoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: Long. Opcional: no.
  Retorno: List<MantenimientoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorTipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - tipo: TipoMantenimiento. Opcional: no.
  Retorno: List<MantenimientoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorTipo(dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarEnCurso (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<MantenimientoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarEnCurso();
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: MantenimientoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza un mantenimiento solo si aún no está cerrado (fechaFin null).
  Parametros: - id: Long. Opcional: no.; - dto: MantenimientoCrearDTO. Opcional: no.
  Retorno: MantenimientoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
- eliminar (method)
  Proposito: Elimina un mantenimiento solo si aún no está cerrado.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.eliminar(1);
- marcarEquipoEnMantenimiento (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicio.java.
  Parametros: - equipo: co.edu.sena.sigea.equipo.entity.Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.marcarEquipoEnMantenimiento(dato);
- mapear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicio.java.
  Parametros: - m: Mantenimiento. Opcional: no.
  Retorno: MantenimientoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapear(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/controller/MarcaControlador.java

Descripcion: Expone endpoints REST del dominio. Administra marcas asociadas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, GetMapping, PathVariable.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (MarcaCrearDTO, MarcaRespuestaDTO, MarcaServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: MarcaControlador

Notas:
- Paquete Java: co.edu.sena.sigea.marca.controller.

Elementos internos:

#### Class: MarcaControlador

Class del archivo MarcaControlador.java.

Campos/props principales:
- marcaServicio: MarcaServicio. Opcional: no.

Miembros principales:
- MarcaControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de MarcaControlador.java.
  Parametros: - marcaServicio: MarcaServicio. Opcional: no.
  Retorno: MarcaControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.MarcaControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody MarcaCrearDTO. Opcional: no.
  Retorno: ResponseEntity<MarcaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato);
- listarActivas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<MarcaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivas();
- listarTodas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<MarcaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodas();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<MarcaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody MarcaCrearDTO. Opcional: no.
  Retorno: ResponseEntity<MarcaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<MarcaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.activar(1);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<MarcaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.desactivar(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/dto/MarcaCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Administra marcas asociadas a equipos.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: MarcaCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.marca.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: MarcaCrearDTO

Class del archivo MarcaCrearDTO.java.

Campos/props principales:
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/dto/MarcaRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Administra marcas asociadas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: MarcaRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.marca.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: MarcaRespuestaDTO

Class del archivo MarcaRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- activo: Boolean. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/entity/Marca.java

Descripcion: Modela datos persistentes del dominio. Administra marcas asociadas a equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, Table).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: Marca

Notas:
- Paquete Java: co.edu.sena.sigea.marca.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Marca

Class del archivo Marca.java.

Campos/props principales:
- nombre: String. Opcional: no.
- descripcion: String. Opcional: no.
- activo: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/repository/MarcaRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Administra marcas asociadas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List, Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Marca).

Exports:
- interface: MarcaRepository

Notas:
- Paquete Java: co.edu.sena.sigea.marca.repository.

Elementos internos:

#### Interface: MarcaRepository

Interface del archivo MarcaRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/marca/service/MarcaServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Administra marcas asociadas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (OperacionNoPermitidaException, RecursoDuplicadoException, RecursoNoEncontradoException, MarcaCrearDTO, MarcaRespuestaDTO).

Exports:
- class: MarcaServicio

Notas:
- Paquete Java: co.edu.sena.sigea.marca.service.

Elementos internos:

#### Class: MarcaServicio

Class del archivo MarcaServicio.java.

Campos/props principales:
- marcaRepository: MarcaRepository. Opcional: no.

Miembros principales:
- MarcaServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de MarcaServicio.java.
  Parametros: - marcaRepository: MarcaRepository. Opcional: no.
  Retorno: MarcaServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.MarcaServicio(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: MarcaCrearDTO. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException
  Ejemplo de uso: var resultado = servicio.crear(dato);
- listarActivas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<MarcaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivas();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarTodas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<MarcaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodas();
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: Long. Opcional: no.; - dto: MarcaCrearDTO. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: Long. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.activar(1);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: Long. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.desactivar(1);
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MarcaServicio.java.
  Parametros: - m: Marca. Opcional: no.
  Retorno: MarcaRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/controller/NotificacionControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (NotificacionRespuestaDTO, PruebaCorreoDTO, NotificacionServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).
- Spring: soporte de web, seguridad, DI o persistencia mediante ResponseEntity, PreAuthorize, AuthenticationPrincipal, UserDetails, *.
- Java SE: estructuras base y utilidades de lenguaje (List, Map).

Exports:
- class: NotificacionControlador

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.controller.

Elementos internos:

#### Class: NotificacionControlador

Class del archivo NotificacionControlador.java.

Campos/props principales:
- notificacionServicio: NotificacionServicio. Opcional: no.

Miembros principales:
- NotificacionControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionControlador.java.
  Parametros: - notificacionServicio: NotificacionServicio. Opcional: no.
  Retorno: NotificacionControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.NotificacionControlador(dato);
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<NotificacionRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
- listarMisNotificaciones (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarMisNotificaciones.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<NotificacionRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/mis-notificaciones") -> listarMisNotificaciones(...)
  Anotaciones/metadata: @GetMapping("/mis-notificaciones")
- listarMisNoLeidas (method)
  Proposito: Atiende una consulta HTTP GET para la operacion listarMisNoLeidas.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<NotificacionRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/mis-notificaciones/no-leidas") -> listarMisNoLeidas(...)
  Anotaciones/metadata: @GetMapping("/mis-notificaciones/no-leidas")
- contarMisNoLeidas (method)
  Proposito: Atiende una consulta HTTP GET para la operacion contarMisNoLeidas.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Map<String, Long>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/mis-notificaciones/contador") -> contarMisNoLeidas(...)
  Anotaciones/metadata: @GetMapping("/mis-notificaciones/contador")
- marcarComoLeida (method)
  Proposito: Atiende una actualizacion parcial HTTP PATCH relacionada con marcarComoLeida.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PatchMapping("/{id}/marcar-leida") -> marcarComoLeida(...)
  Anotaciones/metadata: @PatchMapping("/{id}/marcar-leida")
- probarCorreo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionControlador.java.
  Parametros: - dto: @Valid @RequestBody PruebaCorreoDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Map<String, String>>.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.probarCorreo(dato, dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/dto/NotificacionRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEnvio, MedioEnvio, TipoNotificacion).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: NotificacionRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: NotificacionRespuestaDTO

Class del archivo NotificacionRespuestaDTO.java.

Campos/props principales:
- id: long. Opcional: no.
- usuarioDestinoId: long. Opcional: no.
- nombreUsuarioDestino: String. Opcional: no.
- tipoNotificacion: TipoNotificacion. Opcional: no.
- mensaje: String. Opcional: no.
- titulo: String. Opcional: no.
- medioEnvio: MedioEnvio. Opcional: no.
- estadoEnvio: EstadoEnvio. Opcional: no.
- leida: Boolean. Opcional: no.
- fechaEnvio: LocalDateTime. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/dto/PruebaCorreoDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Email).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: PruebaCorreoDTO

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PruebaCorreoDTO

Class del archivo PruebaCorreoDTO.java.

Campos/props principales:
- destinatario: String. Opcional: no.
- suiteCompleta: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/entity/Notificacion.java

Descripcion: Modela datos persistentes del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoEnvio, MedioEnvio, TipoNotificacion, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: Notificacion

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Notificacion

Class del archivo Notificacion.java.

Campos/props principales:
- usuarioDestino: Usuario. Opcional: no.
- tipo: TipoNotificacion. Opcional: no.
- titulo: String. Opcional: no.
- mensaje: String. Opcional: no.
- medioEnvio: MedioEnvio. Opcional: no.
- estadoEnvio: EstadoEnvio. Opcional: no.
- leida: Boolean. Opcional: no.
- fechaEnvio: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/repository/NotificacionRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEnvio, Notificacion).

Exports:
- interface: NotificacionRepository

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.repository.

Elementos internos:

#### Interface: NotificacionRepository

Interface del archivo NotificacionRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/service/CorreoServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (Map).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ServicioCorreoException).
- Jakarta: validaciones y contratos web/JPA (MimeMessage).
- Otros: dependencias auxiliares (Logger, LoggerFactory, TemplateEngine, Context).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, SimpleMailMessage, JavaMailSender, MimeMessageHelper, Service.

Exports:
- class: CorreoServicio

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.service.

Elementos internos:

#### Class: CorreoServicio

Class del archivo CorreoServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- mailSender: JavaMailSender. Opcional: no.
- templateEngine: TemplateEngine. Opcional: no.
- remitente: String. Opcional: no.
- remitenteNombre: String. Opcional: no.

Miembros principales:
- CorreoServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de CorreoServicio.java.
  Parametros: - mailSender: JavaMailSender. Opcional: no.; - templateEngine: TemplateEngine. Opcional: no.
  Retorno: CorreoServicio.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.CorreoServicio(dato, dato);
- enviarCorreo (method)
  Proposito: Envía correo de texto plano.
  Parametros: - destinatario: String. Opcional: no.; - asunto: String. Opcional: no.; - cuerpo: String. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarCorreo("valor", "valor", "valor");
- enviarCorreoObligatorio (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CorreoServicio.java.
  Parametros: - destinatario: String. Opcional: no.; - asunto: String. Opcional: no.; - cuerpo: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: ServicioCorreoException
  Ejemplo de uso: var resultado = servicio.enviarCorreoObligatorio("valor", "valor", "valor");
- enviarCorreoHtml (method)
  Proposito: Envía correo HTML usando una plantilla Thymeleaf.
  Parametros: - destinatario: String. Opcional: no.; - asunto: String. Opcional: no.; - plantilla: String. Opcional: no.; - variables: Map<String, Object>. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarCorreoHtml("valor", "valor", "valor", "valor");
- enviarCorreoHtmlObligatorio (method)
  Proposito: Envía correo HTML y lanza excepción si falla.
  Parametros: - destinatario: String. Opcional: no.; - asunto: String. Opcional: no.; - plantilla: String. Opcional: no.; - variables: Map<String, Object>. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: ServicioCorreoException
  Ejemplo de uso: var resultado = servicio.enviarCorreoHtmlObligatorio("valor", "valor", "valor", "valor");
- enviarCorreoPruebaObligatorio (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CorreoServicio.java.
  Parametros: - destinatario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarCorreoPruebaObligatorio("valor");
- enviarCorreoNotificacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CorreoServicio.java.
  Parametros: - destinatario: String. Opcional: no.; - asunto: String. Opcional: no.; - variables: Map<String, Object>. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarCorreoNotificacion("valor", "valor", "valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/service/NotificacionServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, ArrayList, LinkedHashSet, List, Set).
- Otros: dependencias auxiliares (Logger, LoggerFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Scheduled, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEnvio, EstadoPrestamo, MedioEnvio, Rol, TipoNotificacion).

Exports:
- class: NotificacionServicio

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.service.

Elementos internos:

#### Class: NotificacionServicio

Class del archivo NotificacionServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- notificacionRepository: NotificacionRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- correoServicio: CorreoServicio. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.

Miembros principales:
- NotificacionServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - notificacionRepository: NotificacionRepository. Opcional: no.; - prestamoRepository: PrestamoRepository. Opcional: no.; - equipoRepository: EquipoRepository. Opcional: no.; - correoServicio: CorreoServicio. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.
  Retorno: NotificacionServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.NotificacionServicio(dato, dato, dato, dato, dato);
- notificarSolicitudPrestamo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarSolicitudPrestamo(dato);
- notificarAprobacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarAprobacion(dato);
- notificarRechazo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarRechazo(dato);
- notificarReservaCreada (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - reserva: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarReservaCreada(dato);
- notificarReservaCancelada (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - reserva: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarReservaCancelada(dato);
- notificarReservaExpirada (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - reserva: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarReservaExpirada(dato);
- notificarEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - reserva: Reserva. Opcional: no.; - fechaHoraDevolucion: java.time.LocalDateTime. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarEquipoRecogido(dato, fecha);
- notificarSalidaPrestamo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarSalidaPrestamo(dato);
- notificarPrestamoDevuelto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoDevuelto(dato);
- notificarPrestamoCancelado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoCancelado(dato);
- detectarMoras (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.detectarMoras();
  Anotaciones/metadata: @Scheduled(cron = "0 */15 * * * *"), @Transactional
- reenviarAlertasDeMora (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.reenviarAlertasDeMora();
  Anotaciones/metadata: @Scheduled(cron = "0 0 8 * * *"), @Transactional
- enviarRecordatorios (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarRecordatorios();
  Anotaciones/metadata: @Scheduled(cron = "0 0 9 * * *"), @Transactional
- notificarSolicitudPrestamoAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - solicitante: Usuario. Opcional: no.; - propietario: Usuario. Opcional: no.; - ambienteNombre: String. Opcional: no.; - fechaReferencia: LocalDateTime. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarSolicitudPrestamoAmbiente(dato, dato, "valor", fecha);
- notificarPrestamoAmbienteAprobado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - solicitante: Usuario. Opcional: no.; - propietario: Usuario. Opcional: no.; - ambienteNombre: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoAmbienteAprobado(dato, dato, "valor");
- notificarPrestamoAmbienteRechazado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - solicitante: Usuario. Opcional: no.; - propietario: Usuario. Opcional: no.; - ambienteNombre: String. Opcional: no.; - motivo: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoAmbienteRechazado(dato, dato, "valor", "valor");
- notificarPrestamoAmbienteCancelado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - solicitante: Usuario. Opcional: no.; - propietario: Usuario. Opcional: no.; - ambienteNombre: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoAmbienteCancelado(dato, dato, "valor");
- notificarPrestamoAmbienteDevuelto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - solicitante: Usuario. Opcional: no.; - propietario: Usuario. Opcional: no.; - ambienteNombre: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarPrestamoAmbienteDevuelto(dato, dato, "valor");
- enviarCorreoPrueba (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - correoSolicitante: String. Opcional: no.; - destinatario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Invoca canales de notificacion externos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.enviarCorreoPrueba("valor", "valor");
- enviarSuiteCorreosUsuario (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - correoSolicitante: String. Opcional: no.; - destinatario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Invoca canales de notificacion externos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.enviarSuiteCorreosUsuario("valor", "valor");
- verificarStockBajo (method)
  Proposito: Valida un codigo, token o condicion del negocio.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.verificarStockBajo();
  Anotaciones/metadata: @Scheduled(cron = "0 0 7 * * MON"), @Transactional
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: Long. Opcional: no.
  Retorno: List<NotificacionRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarNoLeidasPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: Long. Opcional: no.
  Retorno: List<NotificacionRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarNoLeidasPorUsuario(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- contarNoLeidas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - usuarioId: Long. Opcional: no.
  Retorno: long.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.contarNoLeidas(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMisNotificaciones (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - correo: String. Opcional: no.
  Retorno: List<NotificacionRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarMisNotificaciones("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMisNoLeidas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - correo: String. Opcional: no.
  Retorno: List<NotificacionRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarMisNoLeidas("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- contarMisNoLeidas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - correo: String. Opcional: no.
  Retorno: long.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.contarMisNoLeidas("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- marcarComoLeida (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - notificacionId: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.marcarComoLeida(1, "valor");
- crearYEnviar (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - destinatario: Usuario. Opcional: no.; - tipo: TipoNotificacion. Opcional: no.; - titulo: String. Opcional: no.; - mensaje: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearYEnviar(dato, dato, "valor", "valor");
- enviarVistaPreviaNotificacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - correoDestino: String. Opcional: no.; - nombreUsuario: String. Opcional: no.; - tipo: TipoNotificacion. Opcional: no.; - titulo: String. Opcional: no.; - resumen: String. Opcional: no.; - lineasDetalle: List<String>. Opcional: no.; - notaFinal: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.enviarVistaPreviaNotificacion("valor", "valor", dato, "valor", "valor", "valor", "valor");
- construirVariablesNotificacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - destinatario: Usuario. Opcional: no.; - tipo: TipoNotificacion. Opcional: no.; - titulo: String. Opcional: no.; - mensaje: String. Opcional: no.
  Retorno: java.util.Map<String, Object>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.construirVariablesNotificacion(dato, dato, "valor", "valor");
- construirResumen (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.construirResumen("valor");
- construirLineasDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - mensaje: String. Opcional: no.
  Retorno: List<String>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.construirLineasDetalle("valor");
- resolverSubtitulo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - tipo: TipoNotificacion. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverSubtitulo(dato);
- resolverDescripcion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - tipo: TipoNotificacion. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverDescripcion(dato);
- resolverEtiqueta (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - tipo: TipoNotificacion. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverEtiqueta(dato);
- resolverEstilo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - tipo: TipoNotificacion. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverEstilo(dato);
- resolverNotaFinal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - tipo: TipoNotificacion. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverNotaFinal(dato);
- notificarUsuarios (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - destinatarios: List<Usuario>. Opcional: no.; - tipo: TipoNotificacion. Opcional: no.; - titulo: String. Opcional: no.; - mensaje: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.notificarUsuarios([], dato, "valor", "valor");
- resolverDueniosPrestamo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: List<Usuario>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverDueniosPrestamo(dato);
- resolverDuenioEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - equipo: Equipo. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.resolverDuenioEquipo(dato);
- mapear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de NotificacionServicio.java.
  Parametros: - n: Notificacion. Opcional: no.
  Retorno: NotificacionRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapear(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/notificacion/service/WhatsAppServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona notificaciones internas, correo y WhatsApp.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (StandardCharsets, Base64).
- Otros: dependencias auxiliares (Logger, LoggerFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, HttpEntity, HttpHeaders, HttpStatusCode, MediaType.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ServicioWhatsappException, Usuario).

Exports:
- class: WhatsAppServicio

Notas:
- Paquete Java: co.edu.sena.sigea.notificacion.service.

Elementos internos:

#### Class: WhatsAppServicio

Class del archivo WhatsAppServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- restTemplate: RestTemplate. Opcional: no.
- enabled: boolean. Opcional: no.
- accountSid: String. Opcional: no.
- authToken: String. Opcional: no.
- fromNumber: String. Opcional: no.
- defaultCountryCode: String. Opcional: no.

Miembros principales:
- estaHabilitado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WhatsAppServicio.java.
  Parametros: Sin parametros.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.estaHabilitado();
- enviarCodigoVerificacionObligatorio (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WhatsAppServicio.java.
  Parametros: - usuario: Usuario. Opcional: no.; - codigo: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos., Actualiza estado reactivo local del frontend.
  Errores: ServicioWhatsappException
  Ejemplo de uso: var resultado = servicio.enviarCodigoVerificacionObligatorio(dato, "valor");
- validarCredenciales (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WhatsAppServicio.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: ServicioWhatsappException
  Ejemplo de uso: var resultado = servicio.validarCredenciales();
- codificarBasicAuth (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WhatsAppServicio.java.
  Parametros: - user: String. Opcional: no.; - pass: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.codificarBasicAuth("valor", "valor");
- normalizarTelefono (method)
  Proposito: Resuelve una responsabilidad puntual dentro de WhatsAppServicio.java.
  Parametros: - telefono: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.normalizarTelefono("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/controller/ObservacionEquipoControlador.java

Descripcion: Expone endpoints REST del dominio. Adjunta observaciones historicas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, AuthenticationPrincipal, UserDetails.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ObservacionEquipoCrearDTO, ObservacionEquipoRespuestaDTO, ObservacionEquipoServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).
- Otros: dependencias auxiliares (RequiredArgsConstructor).

Exports:
- class: ObservacionEquipoControlador

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.controller.

Elementos internos:

#### Class: ObservacionEquipoControlador

Class del archivo ObservacionEquipoControlador.java.

Campos/props principales:
- servicio: ObservacionEquipoServicio. Opcional: no.

Miembros principales:
- registrar (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - dto: @Valid @RequestBody ObservacionEquipoCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<ObservacionEquipoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.registrar(dato, dato);
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<ObservacionEquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
- listarPorPrestamo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - prestamoId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<ObservacionEquipoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorPrestamo(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/dto/ObservacionEquipoCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Adjunta observaciones historicas a equipos.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Max, Min, NotBlank, NotNull).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: ObservacionEquipoCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ObservacionEquipoCrearDTO

Class del archivo ObservacionEquipoCrearDTO.java.

Campos/props principales:
- prestamoId: Long. Opcional: no.
- equipoId: Long. Opcional: no.
- observaciones: String. Opcional: no.
- estadoDevolucion: Integer. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/dto/ObservacionEquipoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Adjunta observaciones historicas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: ObservacionEquipoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ObservacionEquipoRespuestaDTO

Class del archivo ObservacionEquipoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- prestamoId: Long. Opcional: no.
- equipoId: Long. Opcional: no.
- equipoNombre: String. Opcional: no.
- equipoPlaca: String. Opcional: no.
- usuarioDuenioId: Long. Opcional: no.
- usuarioDuenioNombre: String. Opcional: no.
- usuarioPrestatarioId: Long. Opcional: no.
- usuarioPrestatarioNombre: String. Opcional: no.
- observaciones: String. Opcional: no.
- estadoDevolucion: Integer. Opcional: no.
- fechaRegistro: LocalDateTime. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/entity/ObservacionEquipo.java

Descripcion: Modela datos persistentes del dominio. Adjunta observaciones historicas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, Equipo, Prestamo, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, JoinColumn, ManyToOne).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: ObservacionEquipo

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ObservacionEquipo

Class del archivo ObservacionEquipo.java.

Campos/props principales:
- prestamo: Prestamo. Opcional: no.
- equipo: Equipo. Opcional: no.
- usuarioDuenio: Usuario. Opcional: no.
- usuarioPrestatario: Usuario. Opcional: no.
- observaciones: String. Opcional: no.
- estadoDevolucion: Integer. Opcional: no.
- fechaRegistro: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/repository/ObservacionEquipoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Adjunta observaciones historicas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Param, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ObservacionEquipo).

Exports:
- interface: ObservacionEquipoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.repository.

Elementos internos:

#### Interface: ObservacionEquipoRepository

Interface del archivo ObservacionEquipoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/observacion/service/ObservacionEquipoServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Adjunta observaciones historicas a equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (OperacionNoPermitidaException, RecursoNoEncontradoException, Equipo, EquipoRepository, ObservacionEquipoCrearDTO).
- Otros: dependencias auxiliares (RequiredArgsConstructor).

Exports:
- class: ObservacionEquipoServicio

Notas:
- Paquete Java: co.edu.sena.sigea.observacion.service.

Elementos internos:

#### Class: ObservacionEquipoServicio

Class del archivo ObservacionEquipoServicio.java.

Campos/props principales:
- observacionRepository: ObservacionEquipoRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.

Miembros principales:
- registrar (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - dto: ObservacionEquipoCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: ObservacionEquipoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.registrar(dato, "valor");
  Anotaciones/metadata: @Transactional
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: Long. Opcional: no.
  Retorno: List<ObservacionEquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorPrestamo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - prestamoId: Long. Opcional: no.
  Retorno: List<ObservacionEquipoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorPrestamo(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ObservacionEquipoServicio.java.
  Parametros: - obs: ObservacionEquipo. Opcional: no.
  Retorno: ObservacionEquipoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/controller/PrestamoControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, GrantedAuthority, PreAuthorize, AuthenticationPrincipal.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamo, PrestamoCrearDTO, PrestamoDevolucionDTO, PrestamoRespuestaDTO, PrestamoServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: PrestamoControlador

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.controller.

Elementos internos:

#### Class: PrestamoControlador

Class del archivo PrestamoControlador.java.

Campos/props principales:
- prestamoServicio: PrestamoServicio. Opcional: no.

Miembros principales:
- PrestamoControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoControlador.java.
  Parametros: - prestamoServicio: PrestamoServicio. Opcional: no.
  Retorno: PrestamoControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.PrestamoControlador(dato);
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoControlador.java.
  Parametros: - dto: @Valid @RequestBody PrestamoCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.solicitar(dato, dato);
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos(dato);
- listarMisPrestamos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMisPrestamos(dato);
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: @PathVariable EstadoPrestamo. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.aprobar(1, dato);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.rechazar(1, dato);
- registrarSalida (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.registrarSalida(1, dato);
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody PrestamoDevolucionDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.registrarDevolucion(1, dato, dato);
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.eliminar(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/DetallePrestamoDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Min, NotNull).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: DetallePrestamoDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: DetallePrestamoDTO

Class del archivo DetallePrestamoDTO.java.

Campos/props principales:
- equipoId: Long. Opcional: no.
- cantidad: Integer. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/DetallePrestamoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoCondicion, TipoUsoEquipo).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: DetallePrestamoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: DetallePrestamoRespuestaDTO

Class del archivo DetallePrestamoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- equipoId: Long. Opcional: no.
- nombreEquipo: String. Opcional: no.
- codigoEquipo: String. Opcional: no.
- cantidad: Integer. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- estadoEquipoEntrega: EstadoCondicion. Opcional: no.
- observacionesEntrega: String. Opcional: no.
- estadoEquipoDevolucion: EstadoCondicion. Opcional: no.
- observacionesDevolucion: String. Opcional: no.
- devuelto: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/PrestamoCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Jakarta: validaciones y contratos web/JPA (Valid, Future, NotEmpty, NotNull, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: PrestamoCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoCrearDTO

Class del archivo PrestamoCrearDTO.java.

Campos/props principales:
- fechaHoraDevolucionEstimada: LocalDateTime. Opcional: no.
- observacionesGenerales: String. Opcional: no.
- detalles: List<DetallePrestamoDTO>. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/PrestamoDevolucionDetalleDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Max, Min, NotBlank, NotNull).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: PrestamoDevolucionDetalleDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoDevolucionDetalleDTO

Class del archivo PrestamoDevolucionDetalleDTO.java.

Campos/props principales:
- detalleId: Long. Opcional: no.
- observacionesDevolucion: String. Opcional: no.
- estadoDevolucion: Integer. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/PrestamoDevolucionDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Jakarta: validaciones y contratos web/JPA (Valid, NotEmpty).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: PrestamoDevolucionDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoDevolucionDTO

Class del archivo PrestamoDevolucionDTO.java.

Campos/props principales:
- detalles: List<PrestamoDevolucionDetalleDTO>. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/dto/PrestamoRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamo).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: PrestamoRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoRespuestaDTO

Class del archivo PrestamoRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- usuarioSolicitanteId: Long. Opcional: no.
- nombreUsuarioSolicitante: String. Opcional: no.
- correoUsuarioSolicitante: String. Opcional: no.
- nombreAdministradorAprueba: String. Opcional: no.
- nombreAdministradorRecibe: String. Opcional: no.
- fechaHoraSolicitud: LocalDateTime. Opcional: no.
- fechaHoraAprobacion: LocalDateTime. Opcional: no.
- fechaHoraSalida: LocalDateTime. Opcional: no.
- fechaHoraDevolucionEstimada: LocalDateTime. Opcional: no.
- fechaHoraDevolucionReal: LocalDateTime. Opcional: no.
- estado: EstadoPrestamo. Opcional: no.
- observacionesGenerales: String. Opcional: no.
- extensionesRealizadas: Integer. Opcional: no.
- detalles: List<DetallePrestamoRespuestaDTO>. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/entity/DetallePrestamo.java

Descripcion: Modela datos persistentes del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoCondicion, Equipo).
- Jakarta: validaciones y contratos web/JPA (CascadeType, Column, Entity, EnumType, Enumerated).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: DetallePrestamo

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: DetallePrestamo

Class del archivo DetallePrestamo.java.

Campos/props principales:
- prestamo: Prestamo. Opcional: no.
- equipo: Equipo. Opcional: no.
- cantidad: Integer. Opcional: no.
- estadoEquipoEntrega: EstadoCondicion. Opcional: no.
- observacionesEntrega: String. Opcional: no.
- estadoEquipoDevolucion: EstadoCondicion. Opcional: no.
- observacionesDevolucion: String. Opcional: no.
- devuelto: Boolean. Opcional: no.
- reporteDano: ReporteDano. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/entity/ExtensionPrestamo.java

Descripcion: Modela datos persistentes del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoExtension, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: ExtensionPrestamo

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ExtensionPrestamo

Class del archivo ExtensionPrestamo.java.

Campos/props principales:
- prestamo: Prestamo. Opcional: no.
- fechaSolicitud: LocalDateTime. Opcional: no.
- nuevaFechaDevolucion: LocalDateTime. Opcional: no.
- administradorAprueba: Usuario. Opcional: no.
- estado: EstadoExtension. Opcional: no.
- motivo: String. Opcional: no.
- fechaRespuesta: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/entity/Prestamo.java

Descripcion: Modela datos persistentes del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoPrestamo, Reserva, Usuario).
- Jakarta: validaciones y contratos web/JPA (CascadeType, Column, Entity, EnumType, Enumerated).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, ArrayList, List).

Exports:
- class: Prestamo

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Prestamo

Class del archivo Prestamo.java.

Campos/props principales:
- usuarioSolicitante: Usuario. Opcional: no.
- administradorAprueba: Usuario. Opcional: no.
- administradorRecibe: Usuario. Opcional: no.
- reserva: Reserva. Opcional: no.
- fechaHoraSolicitud: LocalDateTime. Opcional: no.
- fechaHoraAprobacion: LocalDateTime. Opcional: no.
- fechaHoraSalida: LocalDateTime. Opcional: no.
- fechaHoraDevolucionEstimada: LocalDateTime. Opcional: no.
- fechaHoraDevolucionReal: LocalDateTime. Opcional: no.
- estado: EstadoPrestamo. Opcional: no.
- observacionesGenerales: String. Opcional: no.
- extensionesRealizadas: Integer. Opcional: no.
- detalles: List<DetallePrestamo>. Opcional: no.
- extensiones: List<ExtensionPrestamo>. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/entity/ReporteDano.java

Descripcion: Modela datos persistentes del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, JoinColumn, ManyToOne).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: ReporteDano

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ReporteDano

Class del archivo ReporteDano.java.

Campos/props principales:
- detallePrestamo: DetallePrestamo. Opcional: no.
- descripcion: String. Opcional: no.
- fotoRuta: String. Opcional: no.
- fechaReporte: LocalDateTime. Opcional: no.
- reportadoPor: Usuario. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/repository/DetallePrestamoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (DetallePrestamo).

Exports:
- interface: DetallePrestamoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.repository.

Elementos internos:

#### Interface: DetallePrestamoRepository

Interface del archivo DetallePrestamoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/repository/ExtensionPrestamoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoExtension, ExtensionPrestamo).

Exports:
- interface: ExtensionPrestamoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.repository.

Elementos internos:

#### Interface: ExtensionPrestamoRepository

Interface del archivo ExtensionPrestamoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/repository/PrestamoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamo, Prestamo).

Exports:
- interface: PrestamoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.repository.

Elementos internos:

#### Interface: PrestamoRepository

Interface del archivo PrestamoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/repository/ReporteDanoRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (ReporteDano).

Exports:
- interface: ReporteDanoRepository

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.repository.

Elementos internos:

#### Interface: ReporteDanoRepository

Interface del archivo ReporteDanoRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamo/service/PrestamoServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona el ciclo de vida de prestamos de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoCondicion, EstadoPrestamo, EstadoReserva, TipoUsoEquipo, OperacionNoPermitidaException).
- Spring: soporte de web, seguridad, DI o persistencia mediante Scheduled, Service, Transactional.
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, Map, List, Function, Collectors).

Exports:
- class: PrestamoServicio

Notas:
- Paquete Java: co.edu.sena.sigea.prestamo.service.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoServicio

Class del archivo PrestamoServicio.java.

Campos/props principales:
- prestamoRepository: PrestamoRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- notificacionServicio: NotificacionServicio. Opcional: no.
- observacionEquipoRepository: ObservacionEquipoRepository. Opcional: no.

Miembros principales:
- PrestamoServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - prestamoRepository: PrestamoRepository. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.; - equipoRepository: EquipoRepository. Opcional: no.; - reservaRepository: ReservaRepository. Opcional: no.; - notificacionServicio: NotificacionServicio. Opcional: no.; - observacionEquipoRepository: ObservacionEquipoRepository. Opcional: no.
  Retorno: PrestamoServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.PrestamoServicio(dato, dato, dato, dato, dato, dato);
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - dto: PrestamoCrearDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.solicitar(dato, "valor");
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoAdmin: String. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.aprobar(1, "valor");
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoAdmin: String. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.rechazar(1, "valor");
- eliminar (method)
  Proposito: Elimina un préstamo solo si está en estado SOLICITADO (aún no aprobado).
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.eliminar(1);
  Anotaciones/metadata: @Transactional
- registrarSalida (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: Long. Opcional: no.; - correoAdmin: String. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.registrarSalida(1, "valor");
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: Long. Opcional: no.; - dto: PrestamoDevolucionDTO. Opcional: no.; - correoAdmin: String. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.registrarDevolucion(1, dato, "valor");
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.cancelar(1, "valor");
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<PrestamoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: EstadoPrestamo. Opcional: no.
  Retorno: List<PrestamoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: Long. Opcional: no.
  Retorno: List<PrestamoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMisPrestamos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - correoUsuario: String. Opcional: no.
  Retorno: List<PrestamoRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarMisPrestamos("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarEntidadPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: Prestamo.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarEntidadPorId(1);
- mapearPrestamo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - prestamo: Prestamo. Opcional: no.
  Retorno: PrestamoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapearPrestamo(dato);
- mapearDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - detalle: DetallePrestamo. Opcional: no.
  Retorno: DetallePrestamoRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapearDetalle(dato);
- mapearEstadoCondicion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoServicio.java.
  Parametros: - estadoDevolucion: Integer. Opcional: no.
  Retorno: EstadoCondicion.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapearEstadoCondicion(1);
- expirarPrestamosEnMora (method)
  Proposito: Pasa préstamos ACTIVOS a EN_MORA cuando la fecha de devolución ya pasó.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.expirarPrestamosEnMora();
  Anotaciones/metadata: @Scheduled(cron = "0 */15 * * * *"), @Transactional

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/controller/PrestamoAmbienteControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, AuthenticationPrincipal, UserDetails.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (PrestamoAmbienteDevolucionDTO, PrestamoAmbienteRespuestaDTO, PrestamoAmbienteSolicitudDTO, EstadoPrestamoAmbiente, PrestamoAmbienteServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).
- Otros: dependencias auxiliares (RequiredArgsConstructor).

Exports:
- class: PrestamoAmbienteControlador

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.controller.

Elementos internos:

#### Class: PrestamoAmbienteControlador

Class del archivo PrestamoAmbienteControlador.java.

Campos/props principales:
- servicio: PrestamoAmbienteServicio. Opcional: no.

Miembros principales:
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteControlador.java.
  Parametros: - dto: @Valid @RequestBody PrestamoAmbienteSolicitudDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.solicitar(dato, dato);
- listar (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoAmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listar(dato);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoAmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorAmbiente(1);
- misSolicitudes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteControlador.java.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoAmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.misSolicitudes(dato);
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: @PathVariable EstadoPrestamoAmbiente. Opcional: no.
  Retorno: ResponseEntity<List<PrestamoAmbienteRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.aprobar(1, dato);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - motivo: @RequestParam String. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.rechazar(1, "valor", dato);
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody PrestamoAmbienteDevolucionDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.registrarDevolucion(1, dato, dato);
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cancelar(1, dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/dto/PrestamoAmbienteDevolucionDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Max, Min, NotBlank, NotNull).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: PrestamoAmbienteDevolucionDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoAmbienteDevolucionDTO

Class del archivo PrestamoAmbienteDevolucionDTO.java.

Campos/props principales:
- observacionesDevolucion: String. Opcional: no.
- estadoDevolucionAmbiente: Integer. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/dto/PrestamoAmbienteRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalDateTime, LocalTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamoAmbiente, TipoActividad).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: PrestamoAmbienteRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoAmbienteRespuestaDTO

Class del archivo PrestamoAmbienteRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- ambienteId: Long. Opcional: no.
- ambienteNombre: String. Opcional: no.
- solicitanteId: Long. Opcional: no.
- solicitanteNombre: String. Opcional: no.
- propietarioAmbienteId: Long. Opcional: no.
- propietarioAmbienteNombre: String. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- horaInicio: LocalTime. Opcional: no.
- horaFin: LocalTime. Opcional: no.
- proposito: String. Opcional: no.
- numeroParticipantes: Integer. Opcional: no.
- tipoActividad: TipoActividad. Opcional: no.
- observacionesSolicitud: String. Opcional: no.
- estado: EstadoPrestamoAmbiente. Opcional: no.
- observacionesDevolucion: String. Opcional: no.
- estadoDevolucionAmbiente: Integer. Opcional: no.
- fechaSolicitud: LocalDateTime. Opcional: no.
- fechaAprobacion: LocalDateTime. Opcional: no.
- fechaDevolucion: LocalDateTime. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/dto/PrestamoAmbienteSolicitudDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoActividad).
- Jakarta: validaciones y contratos web/JPA (FutureOrPresent, Min, NotBlank, NotNull).
- Otros: dependencias auxiliares (Getter, Setter).

Exports:
- class: PrestamoAmbienteSolicitudDTO

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoAmbienteSolicitudDTO

Class del archivo PrestamoAmbienteSolicitudDTO.java.

Campos/props principales:
- ambienteId: Long. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- horaInicio: LocalTime. Opcional: no.
- horaFin: LocalTime. Opcional: no.
- proposito: String. Opcional: no.
- numeroParticipantes: Integer. Opcional: no.
- tipoActividad: TipoActividad. Opcional: no.
- observacionesSolicitud: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/entity/PrestamoAmbiente.java

Descripcion: Modela datos persistentes del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalDateTime, LocalTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, EntidadBase, EstadoPrestamoAmbiente, TipoActividad, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: PrestamoAmbiente

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: PrestamoAmbiente

Class del archivo PrestamoAmbiente.java.

Campos/props principales:
- ambiente: Ambiente. Opcional: no.
- solicitante: Usuario. Opcional: no.
- propietarioAmbiente: Usuario. Opcional: no.
- fechaInicio: LocalDate. Opcional: no.
- fechaFin: LocalDate. Opcional: no.
- horaInicio: LocalTime. Opcional: no.
- horaFin: LocalTime. Opcional: no.
- proposito: String. Opcional: no.
- numeroParticipantes: Integer. Opcional: no.
- tipoActividad: TipoActividad. Opcional: no.
- observacionesSolicitud: String. Opcional: no.
- estado: EstadoPrestamoAmbiente. Opcional: no.
- observacionesDevolucion: String. Opcional: no.
- estadoDevolucionAmbiente: Integer. Opcional: no.
- fechaSolicitud: LocalDateTime. Opcional: no.
- fechaAprobacion: LocalDateTime. Opcional: no.
- fechaDevolucion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/enums/EstadoPrestamoAmbiente.java

Descripcion: Gestiona prestamos o reservas de ambientes.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: EstadoPrestamoAmbiente

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.enums.

Elementos internos:

#### Enum: EstadoPrestamoAmbiente

Enum del archivo EstadoPrestamoAmbiente.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/enums/TipoActividad.java

Descripcion: Gestiona prestamos o reservas de ambientes.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- enum: TipoActividad

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.enums.

Elementos internos:

#### Enum: TipoActividad

Enum del archivo TipoActividad.java.

Valores expuestos:
- CLASE, TALLER, EVALUACION, REUNION, OTRO

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/repository/PrestamoAmbienteRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Param, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (PrestamoAmbiente, EstadoPrestamoAmbiente).

Exports:
- interface: PrestamoAmbienteRepository

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.repository.

Elementos internos:

#### Interface: PrestamoAmbienteRepository

Interface del archivo PrestamoAmbienteRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/prestamoambiente/service/PrestamoAmbienteServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona prestamos o reservas de ambientes.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, AmbienteRepository, Rol, OperacionNoPermitidaException, RecursoNoEncontradoException).
- Otros: dependencias auxiliares (RequiredArgsConstructor).

Exports:
- class: PrestamoAmbienteServicio

Notas:
- Paquete Java: co.edu.sena.sigea.prestamoambiente.service.

Elementos internos:

#### Class: PrestamoAmbienteServicio

Class del archivo PrestamoAmbienteServicio.java.

Campos/props principales:
- prestamoAmbienteRepository: PrestamoAmbienteRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- notificacionServicio: NotificacionServicio. Opcional: no.

Miembros principales:
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - dto: PrestamoAmbienteSolicitudDTO. Opcional: no.; - correoSolicitante: String. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.solicitar(dato, "valor");
  Anotaciones/metadata: @Transactional
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoAprobador: String. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.aprobar(1, "valor");
  Anotaciones/metadata: @Transactional
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoAprobador: String. Opcional: no.; - motivo: String. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.rechazar(1, "valor", "valor");
  Anotaciones/metadata: @Transactional
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: Long. Opcional: no.; - dto: PrestamoAmbienteDevolucionDTO. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.registrarDevolucion(1, dato, "valor");
  Anotaciones/metadata: @Transactional
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - id: Long. Opcional: no.; - correoSolicitante: String. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.cancelar(1, "valor");
  Anotaciones/metadata: @Transactional
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarVisiblesParaUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - correoUsuario: String. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarVisiblesParaUsuario("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarMisSolicitudes (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - correoSolicitante: String. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.listarMisSolicitudes("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorSolicitante (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - solicitanteId: Long. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorSolicitante(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: Long. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorAmbiente(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorPropietario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - propietarioId: Long. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorPropietario(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: List<PrestamoAmbienteRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- obtenerPrestamoOException (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: - id: Long. Opcional: no.
  Retorno: PrestamoAmbiente.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.obtenerPrestamoOException(1);
- validarEsPropietarioOAdmin (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - prestamo: PrestamoAmbiente. Opcional: no.; - correoUsuario: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.validarEsPropietarioOAdmin(dato, "valor");
- resolverPropietarioAmbiente (method)
  Proposito: Resuelve el propietario/responsable del ambiente.
  Parametros: - ambiente: Ambiente. Opcional: no.
  Retorno: Usuario.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.resolverPropietarioAmbiente(dato);
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de PrestamoAmbienteServicio.java.
  Parametros: - p: PrestamoAmbiente. Opcional: no.
  Retorno: PrestamoAmbienteRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/reporte/controller/ReporteControlador.java

Descripcion: Expone endpoints REST del dominio. Genera reportes exportables en Excel y PDF.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, DateTimeFormatter, DateTimeParseException).
- Spring: soporte de web, seguridad, DI o persistencia mediante ByteArrayResource, HttpHeaders, MediaType, ResponseEntity, PreAuthorize.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, EstadoPrestamo, ReporteServicio).

Exports:
- class: ReporteControlador

Notas:
- Paquete Java: co.edu.sena.sigea.reporte.controller.

Elementos internos:

#### Class: ReporteControlador

Class del archivo ReporteControlador.java.

Campos/props principales:
- reporteServicio: ReporteServicio. Opcional: no.
- FECHA_HORA: DateTimeFormatter. Opcional: no.
- FECHA: DateTimeFormatter. Opcional: no.

Miembros principales:
- ReporteControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de ReporteControlador.java.
  Parametros: - reporteServicio: ReporteServicio. Opcional: no.
  Retorno: ReporteControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.ReporteControlador(dato);
- parseFechaHora (method)
  Proposito: RF-REP-01 + RF-REP-05/06: Reporte de inventario con filtros opcionales.
  Parametros: - valor: String. Opcional: no.
  Retorno: LocalDateTime.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.parseFechaHora("valor");
- responderConArchivo (method)
  Proposito: Construye la respuesta HTTP con el archivo para descarga.
  Parametros: - contenido: byte[]. Opcional: no.; - nombreArchivo: String. Opcional: no.; - extension: String. Opcional: no.
  Retorno: ResponseEntity<ByteArrayResource>.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.responderConArchivo([], "valor", "valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/reporte/service/ReporteExcelServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Genera reportes exportables en Excel y PDF.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (ByteArrayOutputStream, InputStream, DateTimeFormatter, List).
- Otros: dependencias auxiliares (BorderStyle, Cell, CellStyle, ClientAnchor, Drawing).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Equipo, Prestamo, Usuario).

Exports:
- class: ReporteExcelServicio

Notas:
- Paquete Java: co.edu.sena.sigea.reporte.service.

Elementos internos:

#### Class: ReporteExcelServicio

Class del archivo ReporteExcelServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- FECHA_HORA: DateTimeFormatter. Opcional: no.

Miembros principales:
- insertarLogo (method)
  Proposito: Inserta el logo SENA en la celda A1 de la hoja si el archivo está disponible.
  Parametros: - wb: Workbook. Opcional: no.; - sheet: Sheet. Opcional: no.
  Retorno: int.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.insertarLogo(dato, dato);
- generarReporteInventario (method)
  Proposito: RF-REP-01: Reporte de inventario en XLSX.
  Parametros: - equipos: List<Equipo>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteInventario([]);
- generarReportePrestamos (method)
  Proposito: RF-REP-02: Historial de préstamos en XLSX.
  Parametros: - prestamos: List<Prestamo>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReportePrestamos([]);
- generarReporteEquiposMasSolicitados (method)
  Proposito: RF-REP-03: Equipos más solicitados en XLSX.
  Parametros: - equipos: List<Equipo>. Opcional: no.; - cantidades: List<Long>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteEquiposMasSolicitados([], 1);
- generarReporteUsuariosEnMora (method)
  Proposito: RF-REP-04: Usuarios con préstamos pendientes o vencidos en XLSX.
  Parametros: - usuarios: List<Usuario>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteUsuariosEnMora([]);
- crearEstiloEncabezado (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - wb: Workbook. Opcional: no.
  Retorno: CellStyle.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearEstiloEncabezado(dato);
- crearEstiloCelda (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - wb: Workbook. Opcional: no.
  Retorno: CellStyle.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearEstiloCelda(dato);
- crearCelda (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - row: Row. Opcional: no.; - column: int. Opcional: no.; - value: String. Opcional: no.; - style: CellStyle. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearCelda(dato, 1, "valor", dato);
- crearCelda (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - row: Row. Opcional: no.; - column: int. Opcional: no.; - value: double. Opcional: no.; - style: CellStyle. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearCelda(dato, 1, dato, dato);
- crearCelda (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - row: Row. Opcional: no.; - column: int. Opcional: no.; - value: long. Opcional: no.; - style: CellStyle. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearCelda(dato, 1, 1, dato);
- workbookToBytes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReporteExcelServicio.java.
  Parametros: - wb: Workbook. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: Exception
  Ejemplo de uso: var resultado = servicio.workbookToBytes(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/reporte/service/ReportePdfServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Genera reportes exportables en Excel y PDF.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (ByteArrayOutputStream, InputStream, DateTimeFormatter, List).
- Otros: dependencias auxiliares (Document, DocumentException, Element, Font, FontFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Equipo, Prestamo, Usuario).

Exports:
- class: ReportePdfServicio

Notas:
- Paquete Java: co.edu.sena.sigea.reporte.service.

Elementos internos:

#### Class: ReportePdfServicio

Class del archivo ReportePdfServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- FECHA_HORA: DateTimeFormatter. Opcional: no.
- FONT_TITULO: Font. Opcional: no.
- FONT_HEADER: Font. Opcional: no.
- FONT_NORMAL: Font. Opcional: no.

Miembros principales:
- agregarLogo (method)
  Proposito: Agrega el logo SENA al inicio del documento si está disponible en el classpath.
  Parametros: - document: Document. Opcional: no.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.agregarLogo(dato);
- generarReporteInventario (method)
  Proposito: RF-REP-01: Reporte de inventario en PDF.
  Parametros: - equipos: List<Equipo>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteInventario([]);
- generarReportePrestamos (method)
  Proposito: RF-REP-02: Historial de préstamos en PDF.
  Parametros: - prestamos: List<Prestamo>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReportePrestamos([]);
- generarReporteEquiposMasSolicitados (method)
  Proposito: RF-REP-03: Equipos más solicitados en PDF.
  Parametros: - equipos: List<Equipo>. Opcional: no.; - cantidades: List<Long>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteEquiposMasSolicitados([], 1);
- generarReporteUsuariosEnMora (method)
  Proposito: RF-REP-04: Usuarios con préstamos activos o en mora en PDF.
  Parametros: - usuarios: List<Usuario>. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: RuntimeException
  Ejemplo de uso: var resultado = servicio.generarReporteUsuariosEnMora([]);
- agregarCeldaHeader (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReportePdfServicio.java.
  Parametros: - table: PdfPTable. Opcional: no.; - texto: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.agregarCeldaHeader(dato, "valor");
- agregarCelda (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReportePdfServicio.java.
  Parametros: - table: PdfPTable. Opcional: no.; - texto: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.agregarCelda(dato, "valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/reporte/service/ReporteServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Genera reportes exportables en Excel y PDF.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, ArrayList, List, Collectors).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoEquipo, EstadoPrestamo, Equipo, EquipoRepository, Prestamo).

Exports:
- class: ReporteServicio

Notas:
- Paquete Java: co.edu.sena.sigea.reporte.service.

Elementos internos:

#### Class: ReporteServicio

Class del archivo ReporteServicio.java.

Campos/props principales:
- equipoRepository: EquipoRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- detallePrestamoRepository: DetallePrestamoRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- reporteExcelServicio: ReporteExcelServicio. Opcional: no.
- reportePdfServicio: ReportePdfServicio. Opcional: no.

Miembros principales:
- ReporteServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de ReporteServicio.java.
  Parametros: - equipoRepository: EquipoRepository. Opcional: no.; - prestamoRepository: PrestamoRepository. Opcional: no.; - detallePrestamoRepository: DetallePrestamoRepository. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.; - reporteExcelServicio: ReporteExcelServicio. Opcional: no.; - reportePdfServicio: ReportePdfServicio. Opcional: no.
  Retorno: ReporteServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.ReporteServicio(dato, dato, dato, dato, dato, dato);
- generarReporteInventario (method)
  Proposito: RF-REP-01: Reporte de inventario general con filtros opcionales.
  Parametros: - formato: String. Opcional: no.; - inventarioInstructorId: Long. Opcional: no.; - categoriaId: Long. Opcional: no.; - estado: EstadoEquipo. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarReporteInventario("valor", 1, 1, dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- generarReportePrestamos (method)
  Proposito: RF-REP-02: Historial de préstamos con filtros opcionales.
  Parametros: - formato: String. Opcional: no.; - usuarioId: Long. Opcional: no.; - equipoId: Long. Opcional: no.; - desde: LocalDateTime. Opcional: no.; - hasta: LocalDateTime. Opcional: no.; - estado: EstadoPrestamo. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarReportePrestamos("valor", 1, 1, fecha, fecha, dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- generarReporteEquiposMasSolicitados (method)
  Proposito: RF-REP-03: Reporte de equipos más solicitados (ranking).
  Parametros: - formato: String. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarReporteEquiposMasSolicitados("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- generarReporteUsuariosEnMora (method)
  Proposito: RF-REP-04: Reporte de usuarios con préstamos pendientes o vencidos (ACTIVO o EN_MORA).
  Parametros: - formato: String. Opcional: no.
  Retorno: byte[].
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarReporteUsuariosEnMora("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/controller/ReservaControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, GrantedAuthority, PreAuthorize, AuthenticationPrincipal.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoReserva, ReservaCrearDTO, ReservaEquipoRecogidoDTO, ReservaRespuestaDTO, ReservaServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: ReservaControlador

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.controller.

Elementos internos:

#### Class: ReservaControlador

Class del archivo ReservaControlador.java.

Campos/props principales:
- reservaServicio: ReservaServicio. Opcional: no.

Miembros principales:
- ReservaControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de ReservaControlador.java.
  Parametros: - reservaServicio: ReservaServicio. Opcional: no.
  Retorno: ReservaControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.ReservaControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody ReservaCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<ReservaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato, dato);
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<ReservaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos(dato);
- listarMisReservas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<List<ReservaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarMisReservas(dato);
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: @PathVariable EstadoReserva. Opcional: no.
  Retorno: ResponseEntity<List<ReservaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEstado(dato);
- listarPorUsuario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - usuarioId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<ReservaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorUsuario(1);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<ReservaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReservaControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cancelar(1, dato);
- marcarEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReservaControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody ReservaEquipoRecogidoDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<ReservaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.marcarEquipoRecogido(1, dato, dato);
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReservaControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.eliminar(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/dto/ReservaCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Jakarta: validaciones y contratos web/JPA (Future, Min, NotNull).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: ReservaCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ReservaCrearDTO

Class del archivo ReservaCrearDTO.java.

Campos/props principales:
- equipoId: Long. Opcional: no.
- cantidad: Integer. Opcional: no.
- fechaHoraInicio: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/dto/ReservaEquipoRecogidoDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Jakarta: validaciones y contratos web/JPA (Future, NotNull).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: ReservaEquipoRecogidoDTO

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ReservaEquipoRecogidoDTO

DTO para marcar una reserva como "equipo recogido" y registrar la hora de devolución.

Campos/props principales:
- fechaHoraDevolucion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/dto/ReservaRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoReserva, TipoUsoEquipo).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: ReservaRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: ReservaRespuestaDTO

Class del archivo ReservaRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- usuarioId: Long. Opcional: no.
- nombreUsuario: String. Opcional: no.
- correoUsuario: String. Opcional: no.
- equipoId: Long. Opcional: no.
- nombreEquipo: String. Opcional: no.
- codigoEquipo: String. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- cantidad: Integer. Opcional: no.
- fechaHoraInicio: LocalDateTime. Opcional: no.
- fechaHoraFin: LocalDateTime. Opcional: no.
- estado: EstadoReserva. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/entity/Reserva.java

Descripcion: Modela datos persistentes del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoReserva, Equipo, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, FetchType).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: Reserva

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Reserva

Class del archivo Reserva.java.

Campos/props principales:
- usuario: Usuario. Opcional: no.
- equipo: Equipo. Opcional: no.
- cantidad: Integer. Opcional: no.
- fechaHoraInicio: LocalDateTime. Opcional: no.
- fechaHoraFin: LocalDateTime. Opcional: no.
- estado: EstadoReserva. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/repository/ReservaRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Query, Param, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoReserva, Reserva).

Exports:
- interface: ReservaRepository

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.repository.

Elementos internos:

#### Interface: ReservaRepository

Interface del archivo ReservaRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/reserva/service/ReservaServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona reservas anticipadas de equipos.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, LocalDateTime, List).
- Otros: dependencias auxiliares (Logger, LoggerFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Scheduled, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoCondicion, EstadoPrestamo, EstadoReserva, OperacionNoPermitidaException, RecursoNoEncontradoException).

Exports:
- class: ReservaServicio

Notas:
- Paquete Java: co.edu.sena.sigea.reserva.service.

Elementos internos:

#### Class: ReservaServicio

Class del archivo ReservaServicio.java.

Campos/props principales:
- log: Logger. Opcional: no.
- MAX_DIAS_HABILES_ANTICIPACION: int. Opcional: no.
- HORAS_VENTANA_RECOGIDA: int. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- notificacionServicio: NotificacionServicio. Opcional: no.

Miembros principales:
- eliminar (method)
  Proposito: RF-RES-01: máximo días hábiles de anticipación para la fecha de inicio.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.eliminar(1);
- buscarEntidadPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: Reserva.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarEntidadPorId(1);
- mapearReserva (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ReservaServicio.java.
  Parametros: - r: Reserva. Opcional: no.
  Retorno: ReservaRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapearReserva(dato);

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/config/SecurityConfig.java

Descripcion: Declara configuracion transversal del backend o frontend.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (ArrayList, Arrays, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, Bean, Configuration, AuthenticationManager, AuthenticationConfiguration.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (JwtFiltroAutenticacion).
- Jakarta: validaciones y contratos web/JPA (HttpServletRequest).

Exports:
- class: SecurityConfig

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.config.

Elementos internos:

#### Class: SecurityConfig

Class del archivo SecurityConfig.java.

Campos/props principales:
- jwtFiltroAutenticacion: JwtFiltroAutenticacion. Opcional: no.
- appUrl: String. Opcional: no.

Miembros principales:
- permitAuthPath (method)
  Proposito: URL(s) del frontend/app en producción.
  Parametros: Sin parametros.
  Retorno: RequestMatcher.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.permitAuthPath();
- passwordEncoder (method)
  Proposito: Resuelve una responsabilidad puntual dentro de SecurityConfig.java.
  Parametros: Sin parametros.
  Retorno: PasswordEncoder.
  Efectos secundarios: Transforma credenciales o datos sensibles antes de persistirlos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.passwordEncoder();
  Anotaciones/metadata: @Bean
- authenticationManager (method)
  Proposito: Resuelve una responsabilidad puntual dentro de SecurityConfig.java.
  Parametros: - authenticationConfiguration: AuthenticationConfiguration. Opcional: no.
  Retorno: AuthenticationManager.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: Exception
  Ejemplo de uso: var resultado = servicio.authenticationManager(dato);
  Anotaciones/metadata: @Bean
- corsConfigurationSource (method)
  Proposito: Resuelve una responsabilidad puntual dentro de SecurityConfig.java.
  Parametros: Sin parametros.
  Retorno: CorsConfigurationSource.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.corsConfigurationSource();
  Anotaciones/metadata: @Bean

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/controller/AuthControlador.java

Descripcion: Expone endpoints REST del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PostMapping, RequestBody, RequestMapping.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (LoginDTO, LoginRespuestaDTO, RestablecerContrasenaDTO, RegistroDTO, SolicitarRecuperacionDTO).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: AuthControlador

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.controller.

Elementos internos:

#### Class: AuthControlador

Class del archivo AuthControlador.java.

Campos/props principales:
- autenticacionServicio: AutenticacionServicio. Opcional: no.
- verificacionEmailServicio: VerificacionEmailServicio. Opcional: no.

Miembros principales:
- AuthControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de AuthControlador.java.
  Parametros: - autenticacionServicio: AutenticacionServicio. Opcional: no.; - verificacionEmailServicio: VerificacionEmailServicio. Opcional: no.
  Retorno: AuthControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.AuthControlador(dato, dato);
- login (method)
  Proposito: Atiende una operacion HTTP POST para login.
  Parametros: - loginDTO: @Valid @RequestBody LoginDTO. Opcional: no.
  Retorno: ResponseEntity<LoginRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping("/login") -> login(...)
  Anotaciones/metadata: @PostMapping("/login")
- registro (method)
  Proposito: Atiende una operacion HTTP POST para registro.
  Parametros: - registroDTO: @Valid @RequestBody RegistroDTO. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping("/registro") -> registro(...)
  Anotaciones/metadata: @PostMapping("/registro")
- verificarEmailPorCodigo (method)
  Proposito: Verificación de email por código de 6 dígitos enviado al correo.
  Parametros: - dto: @Valid @RequestBody VerificarCodigoDTO. Opcional: no.
  Retorno: ResponseEntity<java.util.Map<String, String>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping("/verificar-email") -> verificarEmailPorCodigo(...)
  Anotaciones/metadata: @PostMapping("/verificar-email")
- solicitarRecuperacion (method)
  Proposito: Atiende una operacion HTTP POST para solicitarRecuperacion.
  Parametros: - dto: @Valid @RequestBody SolicitarRecuperacionDTO. Opcional: no.
  Retorno: ResponseEntity<java.util.Map<String, String>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping("/recuperar-contrasena") -> solicitarRecuperacion(...)
  Anotaciones/metadata: @PostMapping("/recuperar-contrasena")
- restablecerContrasena (method)
  Proposito: Atiende una operacion HTTP POST para restablecerContrasena.
  Parametros: - dto: @Valid @RequestBody RestablecerContrasenaDTO. Opcional: no.
  Retorno: ResponseEntity<java.util.Map<String, String>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PostMapping("/restablecer-contrasena") -> restablecerContrasena(...)
  Anotaciones/metadata: @PostMapping("/restablecer-contrasena")

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/LoginDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: LoginDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: LoginDTO

Class del archivo LoginDTO.java.

Campos/props principales:
- numeroDocumento: String. Opcional: no.
- contrasena: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/LoginRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, Setter).

Exports:
- class: LoginRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: LoginRespuestaDTO

Class del archivo LoginRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- token: String. Opcional: no.
- tipo: String. Opcional: no.
- nombreCompleto: String. Opcional: no.
- rol: String. Opcional: no.
- correoElectronico: String. Opcional: no.
- esSuperAdmin: Boolean. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/RegistroDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoDocumento).
- Jakarta: validaciones y contratos web/JPA (Email, NotBlank, NotNull, Pattern, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: RegistroDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: RegistroDTO

Class del archivo RegistroDTO.java.

Campos/props principales:
- nombre: String. Opcional: no.
- tipoDocumento: TipoDocumento. Opcional: no.
- numeroDocumento: String. Opcional: no.
- correoElectronico: String. Opcional: no.
- programaFormacion: String. Opcional: no.
- telefono: String. Opcional: no.
- numeroFicha: String. Opcional: no.
- contrasena: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/RestablecerContrasenaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Email, NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: RestablecerContrasenaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: RestablecerContrasenaDTO

Class del archivo RestablecerContrasenaDTO.java.

Campos/props principales:
- correo: String. Opcional: no.
- codigo: String. Opcional: no.
- nuevaContrasena: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/SolicitarRecuperacionDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (Email, NotBlank).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: SolicitarRecuperacionDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: SolicitarRecuperacionDTO

Class del archivo SolicitarRecuperacionDTO.java.

Campos/props principales:
- correo: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/dto/VerificarCodigoDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: VerificarCodigoDTO

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: VerificarCodigoDTO

DTO para verificar el correo con el código de 6 dígitos enviado por email.

Campos/props principales:
- correo: String. Opcional: no.
- codigo: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/jwt/JwtFiltroAutenticacion.java

Descripcion: Implementa generacion o validacion de tokens JWT y su integracion con Spring Security.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (IOException).
- Spring: soporte de web, seguridad, DI o persistencia mediante UsernamePasswordAuthenticationToken, SecurityContextHolder, UserDetails, WebAuthenticationDetailsSource, Component.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (UsuarioDetallesServicio).
- Jakarta: validaciones y contratos web/JPA (FilterChain, ServletException, HttpServletRequest, HttpServletResponse).

Exports:
- class: JwtFiltroAutenticacion

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.jwt.

Elementos internos:

#### Class: JwtFiltroAutenticacion

Class del archivo JwtFiltroAutenticacion.java.

Campos/props principales:
- jwtProveedor: JwtProveedor. Opcional: no.
- usuarioDetallesServicio: UsuarioDetallesServicio. Opcional: no.

Miembros principales:
- JwtFiltroAutenticacion (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de JwtFiltroAutenticacion.java.
  Parametros: - jwtProveedor: JwtProveedor. Opcional: no.; - usuarioDetallesServicio: UsuarioDetallesServicio. Opcional: no.
  Retorno: JwtFiltroAutenticacion.
  Efectos secundarios: Genera o valida tokens de autenticacion.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.JwtFiltroAutenticacion(dato, dato);
- doFilterInternal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de JwtFiltroAutenticacion.java.
  Parametros: - request: HttpServletRequest. Opcional: no.; - response: HttpServletResponse. Opcional: no.; - filterChain: FilterChain. Opcional: no.
  Retorno: void.
  Efectos secundarios: Genera o valida tokens de autenticacion.
  Errores: ServletException, IOException
  Ejemplo de uso: var resultado = servicio.doFilterInternal(dato, dato, dato);
  Anotaciones/metadata: @Override

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/jwt/JwtProveedor.java

Descripcion: Implementa generacion o validacion de tokens JWT y su integracion con Spring Security.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (StandardCharsets, Date).
- Otros: dependencias auxiliares (SecretKey, Claims, Jwts, Keys).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, Component.

Exports:
- class: JwtProveedor

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.jwt.

Elementos internos:

#### Class: JwtProveedor

Class del archivo JwtProveedor.java.

Campos/props principales:
- claveSecreta: SecretKey. Opcional: no.
- tiempoExpiracionMs: long. Opcional: no.

Miembros principales:
- generarToken (method)
  Proposito: Construye una salida derivada, reporte o token.
  Parametros: - correo: String. Opcional: no.; - rol: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: Genera o valida tokens de autenticacion.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarToken("valor", "valor");
- validarToken (method)
  Proposito: Resuelve una responsabilidad puntual dentro de JwtProveedor.java.
  Parametros: - token: String. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: Genera o valida tokens de autenticacion.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.validarToken("valor");
- obtenerCorreoDelToken (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: - token: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.obtenerCorreoDelToken("valor");
- obtenerRolDelToken (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: - token: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.obtenerRolDelToken("valor");
- extraerClaims (method)
  Proposito: Resuelve una responsabilidad puntual dentro de JwtProveedor.java.
  Parametros: - token: String. Opcional: no.
  Retorno: Claims.
  Efectos secundarios: Genera o valida tokens de autenticacion.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.extraerClaims("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/service/AutenticacionServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Spring: soporte de web, seguridad, DI o persistencia mediante Value, AuthenticationManager, PasswordEncoder, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoAprobacion, OperacionNoPermitidaException, CorreoServicio, LoginDTO, LoginRespuestaDTO).

Exports:
- class: AutenticacionServicio

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.service.

Elementos internos:

#### Class: AutenticacionServicio

Class del archivo AutenticacionServicio.java.

Campos/props principales:
- authenticationManager: AuthenticationManager. Opcional: no.
- jwtProveedor: JwtProveedor. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- passwordEncoder: PasswordEncoder. Opcional: no.
- verificacionEmailServicio: VerificacionEmailServicio. Opcional: no.
- correoServicio: CorreoServicio. Opcional: no.
- requireEmailVerification: boolean. Opcional: no.
- MAX_INTENTOS_FALLIDOS: int. Opcional: no.
- MINUTOS_BLOQUEO_PRIMERO: int. Opcional: no.
- MINUTOS_BLOQUEO_SEGUNDO: int. Opcional: no.
- MINUTOS_VALIDEZ_RECUPERACION: int. Opcional: no.

Miembros principales:
- login (method)
  Proposito: Autentica al usuario y entrega el contexto de sesion.
  Parametros: - loginDTO: LoginDTO. Opcional: no.
  Retorno: LoginRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Genera o valida tokens de autenticacion.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.login(dato);
  Anotaciones/metadata: @Transactional
- manejarLoginFallido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AutenticacionServicio.java.
  Parametros: - usuario: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.manejarLoginFallido(dato);
- registrar (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - registroDTO: RegistroDTO. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Transforma credenciales o datos sensibles antes de persistirlos., Genera o valida tokens de autenticacion.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.registrar(dato);
  Anotaciones/metadata: @Transactional
- solicitarRecuperacionContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AutenticacionServicio.java.
  Parametros: - correo: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Genera o valida tokens de autenticacion., Invoca canales de notificacion externos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.solicitarRecuperacionContrasena("valor");
  Anotaciones/metadata: @Transactional
- restablecerContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AutenticacionServicio.java.
  Parametros: - correo: String. Opcional: no.; - codigo: String. Opcional: no.; - nuevaContrasena: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Transforma credenciales o datos sensibles antes de persistirlos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.restablecerContrasena("valor", "valor", "valor");
  Anotaciones/metadata: @Transactional
- validarContrasenaSegura (method)
  Proposito: Resuelve una responsabilidad puntual dentro de AutenticacionServicio.java.
  Parametros: - contrasena: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.validarContrasenaSegura("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/service/UsuarioDetallesServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (Collections).
- Spring: soporte de web, seguridad, DI o persistencia mediante SimpleGrantedAuthority, UserDetails, UserDetailsService, UsernameNotFoundException, Service.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Usuario, UsuarioRepository).

Exports:
- class: UsuarioDetallesServicio

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.service.

Elementos internos:

#### Class: UsuarioDetallesServicio

Class del archivo UsuarioDetallesServicio.java.

Campos/props principales:
- usuarioRepository: UsuarioRepository. Opcional: no.

Miembros principales:
- UsuarioDetallesServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioDetallesServicio.java.
  Parametros: - usuarioRepository: UsuarioRepository. Opcional: no.
  Retorno: UsuarioDetallesServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.UsuarioDetallesServicio(dato);
- loadUserByUsername (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: - correo: String. Opcional: no.
  Retorno: UserDetails.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: UsernameNotFoundException
  Ejemplo de uso: var resultado = servicio.loadUserByUsername("valor");
  Anotaciones/metadata: @Override

### sigea-backend/src/main/java/co/edu/sena/sigea/seguridad/service/VerificacionEmailServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Resuelve autenticacion, autorizacion y JWT.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, Map, ThreadLocalRandom).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (OperacionNoPermitidaException, CorreoServicio, WhatsAppServicio, Usuario, UsuarioRepository).

Exports:
- class: VerificacionEmailServicio

Notas:
- Paquete Java: co.edu.sena.sigea.seguridad.service.

Elementos internos:

#### Class: VerificacionEmailServicio

Servicio para verificación de email en registro: genera un código de 6 dígitos, lo envía por correo y valida el código cuando el usuario lo ingresa en la app.

Campos/props principales:
- HORAS_VALIDEZ_CODIGO: int. Opcional: no.
- DIGITOS_CODIGO: int. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- correoServicio: CorreoServicio. Opcional: no.
- whatsAppServicio: WhatsAppServicio. Opcional: no.

Miembros principales:
- enviarEmailVerificacion (method)
  Proposito: Envía el correo con el código de verificación (no enlace).
  Parametros: - usuario: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: Invoca canales de notificacion externos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.enviarEmailVerificacion(dato);
- verificarCodigo (method)
  Proposito: Verifica el código enviado al correo y marca el correo como verificado.
  Parametros: - correo: String. Opcional: no.; - codigo: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.verificarCodigo("valor", "valor");
  Anotaciones/metadata: @Transactional

### sigea-backend/src/main/java/co/edu/sena/sigea/SigeaBackendApplication.java

Descripcion: Punto de entrada del backend Spring Boot y habilitacion de tareas programadas.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante SpringApplication, SpringBootApplication, EnableScheduling.

Exports:
- class: SigeaBackendApplication

Notas:
- Paquete Java: co.edu.sena.sigea.

Elementos internos:

#### Class: SigeaBackendApplication

Class del archivo SigeaBackendApplication.java.

Miembros principales:
- main (method)
  Proposito: Resuelve una responsabilidad puntual dentro de SigeaBackendApplication.java.
  Parametros: - args: String[]. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.main("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/controller/TransferenciaControlador.java

Descripcion: Expone endpoints REST del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, AuthenticationPrincipal, UserDetails.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TransferenciaCrearDTO, TransferenciaRespuestaDTO, TransferenciaServicio).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: TransferenciaControlador

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.controller.

Elementos internos:

#### Class: TransferenciaControlador

Class del archivo TransferenciaControlador.java.

Campos/props principales:
- transferenciaServicio: TransferenciaServicio. Opcional: no.

Miembros principales:
- TransferenciaControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaControlador.java.
  Parametros: - transferenciaServicio: TransferenciaServicio. Opcional: no.
  Retorno: TransferenciaControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.TransferenciaControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody TransferenciaCrearDTO. Opcional: no.; - userDetails: @AuthenticationPrincipal UserDetails. Opcional: no.
  Retorno: ResponseEntity<TransferenciaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato, dato);
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<TransferenciaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<TransferenciaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
- listarPorInstructorOrigen (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<TransferenciaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructorOrigen(1);
- listarPorInstructorDestino (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<List<TransferenciaRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructorDestino(1);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<TransferenciaRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/dto/TransferenciaCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Jakarta: validaciones y contratos web/JPA (Min, NotNull).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: TransferenciaCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: TransferenciaCrearDTO

Class del archivo TransferenciaCrearDTO.java.

Campos/props principales:
- equipoId: Long. Opcional: no.
- instructorDestinoId: Long. Opcional: no.
- ubicacionDestinoId: Long. Opcional: no.
- cantidad: Integer. Opcional: no.
- motivo: String. Opcional: no.
- fechaTransferencia: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/dto/TransferenciaRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: TransferenciaRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: TransferenciaRespuestaDTO

Class del archivo TransferenciaRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- equipoId: Long. Opcional: no.
- nombreEquipo: String. Opcional: no.
- codigoEquipo: String. Opcional: no.
- inventarioOrigenInstructorId: Long. Opcional: no.
- nombreInventarioOrigenInstructor: String. Opcional: no.
- inventarioDestinoInstructorId: Long. Opcional: no.
- nombreInventarioDestinoInstructor: String. Opcional: no.
- propietarioEquipoId: Long. Opcional: no.
- nombrePropietarioEquipo: String. Opcional: no.
- ubicacionDestinoId: Long. Opcional: no.
- nombreUbicacionDestino: String. Opcional: no.
- cantidad: Integer. Opcional: no.
- administradorAutorizaId: Long. Opcional: no.
- nombreAdministrador: String. Opcional: no.
- motivo: String. Opcional: no.
- fechaTransferencia: LocalDateTime. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/entity/Transferencia.java

Descripcion: Modela datos persistentes del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, EntidadBase, Equipo, Usuario).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, FetchType, JoinColumn, ManyToOne).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: Transferencia

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Transferencia

Class del archivo Transferencia.java.

Campos/props principales:
- equipo: Equipo. Opcional: no.
- inventarioOrigenInstructor: Usuario. Opcional: no.
- inventarioDestinoInstructor: Usuario. Opcional: no.
- propietarioEquipo: Usuario. Opcional: no.
- ubicacionDestino: Ambiente. Opcional: no.
- cantidad: Integer. Opcional: no.
- administradorAutoriza: Usuario. Opcional: no.
- motivo: String. Opcional: no.
- fechaTransferencia: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/repository/TransferenciaRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Transferencia).

Exports:
- interface: TransferenciaRepository

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.repository.

Elementos internos:

#### Interface: TransferenciaRepository

Interface del archivo TransferenciaRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/transferencia/service/TransferenciaServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List, UUID).
- Spring: soporte de web, seguridad, DI o persistencia mediante Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Rol, OperacionNoPermitidaException, RecursoNoEncontradoException, Ambiente, AmbienteRepository).

Exports:
- class: TransferenciaServicio

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.service.

Elementos internos:

#### Class: TransferenciaServicio

Class del archivo TransferenciaServicio.java.

Campos/props principales:
- transferenciaRepository: TransferenciaRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.

Miembros principales:
- TransferenciaServicio (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicio.java.
  Parametros: - transferenciaRepository: TransferenciaRepository. Opcional: no.; - equipoRepository: EquipoRepository. Opcional: no.; - ambienteRepository: AmbienteRepository. Opcional: no.; - usuarioRepository: UsuarioRepository. Opcional: no.
  Retorno: TransferenciaServicio.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.TransferenciaServicio(dato, dato, dato, dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: TransferenciaCrearDTO. Opcional: no.; - correoAdministrador: String. Opcional: no.
  Retorno: TransferenciaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.crear(dato, "valor");
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<TransferenciaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: Long. Opcional: no.
  Retorno: List<TransferenciaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorEquipo(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorInstructorOrigen (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: Long. Opcional: no.
  Retorno: List<TransferenciaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructorOrigen(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorInstructorDestino (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: Long. Opcional: no.
  Retorno: List<TransferenciaRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorInstructorDestino(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: TransferenciaRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- mapear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicio.java.
  Parametros: - t: Transferencia. Opcional: no.
  Retorno: TransferenciaRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.mapear(dato);
- esAdministrador (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicio.java.
  Parametros: - usuario: Usuario. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.esAdministrador(dato);
- crearEquipoTransferidoParcial (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - origen: Equipo. Opcional: no.; - cantidad: int. Opcional: no.; - inventarioDestino: Usuario. Opcional: no.; - ubicacionDestino: Ambiente. Opcional: no.
  Retorno: Equipo.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crearEquipoTransferidoParcial(dato, 1, dato, dato);
- generarCodigoTransferido (method)
  Proposito: Construye una salida derivada, reporte o token.
  Parametros: - codigoBase: String. Opcional: no.
  Retorno: String.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.generarCodigoTransferido("valor");

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/controller/UsuarioControlador.java

Descripcion: Expone endpoints REST del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (List).
- Spring: soporte de web, seguridad, DI o persistencia mediante HttpStatus, ResponseEntity, PreAuthorize, Authentication, SecurityContextHolder.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Rol, UsuarioActualizadoDTO, UsuarioCambiarContrasenaDTO, UsuarioCambiarRolDTO, UsuarioCrearDTO).
- Jakarta: validaciones y contratos web/JPA (Valid).

Exports:
- class: UsuarioControlador

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.controller.

Elementos internos:

#### Class: UsuarioControlador

Class del archivo UsuarioControlador.java.

Campos/props principales:
- usuarioServicio: UsuarioService. Opcional: no.

Miembros principales:
- UsuarioControlador (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioControlador.java.
  Parametros: - usuarioServicio: UsuarioService. Opcional: no.
  Retorno: UsuarioControlador.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.UsuarioControlador(dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: @Valid @RequestBody UsuarioCrearDTO. Opcional: no.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.crear(dato);
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<UsuarioRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<UsuarioRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
- listarPorRol (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - rol: @PathVariable Rol. Opcional: no.
  Retorno: ResponseEntity<List<UsuarioRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorRol(dato);
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
- obtenerPerfil (method)
  Proposito: Atiende una consulta HTTP GET para la operacion obtenerPerfil.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @GetMapping("/perfil") -> obtenerPerfil(...)
  Anotaciones/metadata: @GetMapping("/perfil")
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody UsuarioActualizadoDTO. Opcional: no.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
- cambiarContrasena (method)
  Proposito: Atiende una actualizacion parcial HTTP PATCH relacionada con cambiarContrasena.
  Parametros: - dto: @Valid @RequestBody UsuarioCambiarContrasenaDTO. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: HTTP -> @PatchMapping("/cambiar-contrasena") -> cambiarContrasena(...)
  Anotaciones/metadata: @PatchMapping("/cambiar-contrasena")
- cambiarRol (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody UsuarioCambiarRolDTO. Opcional: no.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cambiarRol(1, dato);
- restablecerContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.; - dto: @Valid @RequestBody UsuarioRestablecerContrasenaDTO. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.restablecerContrasena(1, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.activar(1);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.desactivar(1);
- desbloquearCuenta (method)
  Proposito: Restablece condiciones de acceso o bloqueo.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.desbloquearCuenta(1);
- listarPendientes (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: ResponseEntity<List<UsuarioRespuestaDTO>>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPendientes();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<UsuarioRespuestaDTO>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.aprobar(1);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.rechazar(1);
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioControlador.java.
  Parametros: - id: @PathVariable Long. Opcional: no.
  Retorno: ResponseEntity<Void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.eliminar(1);

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioActualizadoDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoDocumento).
- Jakarta: validaciones y contratos web/JPA (Email, NotBlank, NotNull, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioActualizadoDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioActualizadoDTO

Class del archivo UsuarioActualizadoDTO.java.

Campos/props principales:
- nombreCompleto: String. Opcional: no.
- tipoDocumento: TipoDocumento. Opcional: no.
- numeroDocumento: String. Opcional: no.
- correoElectronico: String. Opcional: no.
- numeroTelefono: String. Opcional: no.
- programaFormacion: String. Opcional: no.
- numeroFicha: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioCambiarContrasenaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioCambiarContrasenaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioCambiarContrasenaDTO

Class del archivo UsuarioCambiarContrasenaDTO.java.

Campos/props principales:
- contrasenaActual: String. Opcional: no.
- nuevaContrasena: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioCambiarRolDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Rol).
- Jakarta: validaciones y contratos web/JPA (NotNull).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioCambiarRolDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioCambiarRolDTO

Class del archivo UsuarioCambiarRolDTO.java.

Campos/props principales:
- nuevoRol: Rol. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioCrearDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Rol, TipoDocumento).
- Jakarta: validaciones y contratos web/JPA (Email, NotBlank, NotNull, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioCrearDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioCrearDTO

Class del archivo UsuarioCrearDTO.java.

Campos/props principales:
- nombreCompleto: String. Opcional: no.
- tipoDocumento: TipoDocumento. Opcional: no.
- numeroDocumento: String. Opcional: no.
- correoElectronico: String. Opcional: no.
- programaFormacion: String. Opcional: no.
- numeroFicha: String. Opcional: no.
- telefono: String. Opcional: no.
- contrasena: String. Opcional: no.
- rol: Rol. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioRespuestaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioRespuestaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioRespuestaDTO

Class del archivo UsuarioRespuestaDTO.java.

Campos/props principales:
- id: Long. Opcional: no.
- nombreCompleto: String. Opcional: no.
- tipoDocumento: String. Opcional: no.
- numeroDocumento: String. Opcional: no.
- correoElectronico: String. Opcional: no.
- telefono: String. Opcional: no.
- programaFormacion: String. Opcional: no.
- ficha: String. Opcional: no.
- rol: String. Opcional: no.
- esSuperAdmin: Boolean. Opcional: no.
- activo: Boolean. Opcional: no.
- estadoAprobacion: String. Opcional: no.
- fechaCreacion: LocalDateTime. Opcional: no.
- fechaActualizacion: LocalDateTime. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/dto/UsuarioRestablecerContrasenaDTO.java

Descripcion: Define contratos de entrada/salida del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Jakarta: validaciones y contratos web/JPA (NotBlank, Size).
- Otros: dependencias auxiliares (AllArgsConstructor, Getter, NoArgsConstructor, Setter).

Exports:
- class: UsuarioRestablecerContrasenaDTO

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.dto.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: UsuarioRestablecerContrasenaDTO

Class del archivo UsuarioRestablecerContrasenaDTO.java.

Campos/props principales:
- nuevaContrasena: String. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/entity/Usuario.java

Descripcion: Modela datos persistentes del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EntidadBase, EstadoAprobacion, Rol, TipoDocumento).
- Jakarta: validaciones y contratos web/JPA (Column, Entity, EnumType, Enumerated, Table).
- Otros: dependencias auxiliares (AllArgsConstructor, Builder, Getter, NoArgsConstructor, Setter).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime).

Exports:
- class: Usuario

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.entity.

Advertencias:
- ⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.

Elementos internos:

#### Class: Usuario

Class del archivo Usuario.java.

Campos/props principales:
- nombreCompleto: String. Opcional: no.
- numeroDocumento: String. Opcional: no.
- tipoDocumento: TipoDocumento. Opcional: no.
- correoElectronico: String. Opcional: no.
- telefono: String. Opcional: no.
- programaFormacion: String. Opcional: no.
- ficha: String. Opcional: no.
- contrasenaHash: String. Opcional: no.
- rol: Rol. Opcional: no.
- esSuperAdmin: Boolean. Opcional: no.
- activo: Boolean. Opcional: no.
- intentosFallidos: Integer. Opcional: no.
- cuentaBloqueadaHasta: LocalDateTime. Opcional: no.
- emailVerificado: Boolean. Opcional: no.
- tokenVerificacion: String. Opcional: no.
- tokenVerificacionExpira: LocalDateTime. Opcional: no.
- estadoAprobacion: EstadoAprobacion. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/repository/UsuarioRepository.java

Descripcion: Encapsula acceso a datos del dominio mediante Spring Data/JPA. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List, Optional).
- Spring: soporte de web, seguridad, DI o persistencia mediante JpaRepository, Repository.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoAprobacion, Rol, Usuario).

Exports:
- interface: UsuarioRepository

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.repository.

Elementos internos:

#### Interface: UsuarioRepository

Interface del archivo UsuarioRepository.java.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/runner/CrearAdminRunner.java

Descripcion: Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante CommandLineRunner, Profile, Order, Component.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Rol, TipoDocumento, UsuarioCrearDTO, UsuarioService).

Exports:
- class: CrearAdminRunner

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.runner.

Elementos internos:

#### Class: CrearAdminRunner

Crea un usuario administrador por defecto (admin2@sigea.local / password) cuando se arranca la app con el perfil "crear-admin".

Campos/props principales:
- CORREO_ADMIN: String. Opcional: no.
- PASSWORD_TEMPORAL: String. Opcional: no.
- usuarioService: UsuarioService. Opcional: no.

Miembros principales:
- CrearAdminRunner (constructor)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - usuarioService: UsuarioService. Opcional: no.
  Retorno: CrearAdminRunner.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.CrearAdminRunner(dato);
- run (method)
  Proposito: Resuelve una responsabilidad puntual dentro de CrearAdminRunner.java.
  Parametros: - args: String.... Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.run("valor");
  Anotaciones/metadata: @Override

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/service/LimpiezaUsuariosPendientesServicio.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Otros: dependencias auxiliares (Logger, LoggerFactory).
- Spring: soporte de web, seguridad, DI o persistencia mediante Scheduled, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (AmbienteRepository, EstadoAprobacion, NotificacionRepository, ReservaRepository, Usuario).

Exports:
- class: LimpiezaUsuariosPendientesServicio

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.service.

Elementos internos:

#### Class: LimpiezaUsuariosPendientesServicio

Tarea programada que elimina automáticamente los registros de usuarios que llevan más de 24 horas en estado PENDIENTE sin que el administrador haya tomado ninguna acción (aprobar o rechazar).

Campos/props principales:
- log: Logger. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- notificacionRepository: NotificacionRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.

### sigea-backend/src/main/java/co/edu/sena/sigea/usuario/service/UsuarioService.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Gestiona usuarios, roles, aprobaciones y credenciales.

Dependencias:
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List).
- Spring: soporte de web, seguridad, DI o persistencia mediante PasswordEncoder, Service, Transactional.
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoAprobacion, Rol, OperacionNoPermitidaException, RecursoDuplicadoException, RecursoNoEncontradoException).

Exports:
- class: UsuarioService

Notas:
- Paquete Java: co.edu.sena.sigea.usuario.service.

Elementos internos:

#### Class: UsuarioService

Class del archivo UsuarioService.java.

Campos/props principales:
- usuarioRepository: UsuarioRepository. Opcional: no.
- passwordEncoder: PasswordEncoder. Opcional: no.
- notificacionRepository: NotificacionRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.

Miembros principales:
- UsuarioService (constructor)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - usuarioRepository: UsuarioRepository. Opcional: no.; - passwordEncoder: PasswordEncoder. Opcional: no.; - notificacionRepository: NotificacionRepository. Opcional: no.; - reservaRepository: ReservaRepository. Opcional: no.; - prestamoRepository: PrestamoRepository. Opcional: no.; - ambienteRepository: AmbienteRepository. Opcional: no.
  Retorno: UsuarioService.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.UsuarioService(dato, dato, dato, dato, dato, dato);
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: UsuarioCrearDTO. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Transforma credenciales o datos sensibles antes de persistirlos.
  Errores: RecursoDuplicadoException
  Ejemplo de uso: var resultado = servicio.crear(dato);
  Anotaciones/metadata: @Transactional
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<UsuarioRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarActivos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<UsuarioRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodos();
  Anotaciones/metadata: @Transactional(readOnly = true)
- listarPorRol (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - rol: Rol. Opcional: no.
  Retorno: List<UsuarioRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPorRol(dato);
  Anotaciones/metadata: @Transactional(readOnly = true)
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: Long. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.buscarPorId(1);
  Anotaciones/metadata: @Transactional(readOnly = true)
- obtenerPerfil (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: - correoElectronico: String. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.obtenerPerfil("valor");
  Anotaciones/metadata: @Transactional(readOnly = true)
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: long. Opcional: no.; - dto: UsuarioActualizadoDTO. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: RecursoDuplicadoException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.actualizar(1, dato);
  Anotaciones/metadata: @Transactional
- cambiarContrasena (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - correoElectronico: String. Opcional: no.; - dto: UsuarioCambiarContrasenaDTO. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Transforma credenciales o datos sensibles antes de persistirlos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.cambiarContrasena("valor", dato);
  Anotaciones/metadata: @Transactional
- restablecerContrasenaAdministrativa (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - correoAdministrador: String. Opcional: no.; - id: Long. Opcional: no.; - dto: UsuarioRestablecerContrasenaDTO. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos., Transforma credenciales o datos sensibles antes de persistirlos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.restablecerContrasenaAdministrativa("valor", 1, dato);
  Anotaciones/metadata: @Transactional
- cambiarRol (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: long. Opcional: no.; - dto: UsuarioCambiarRolDTO. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.cambiarRol(1, dato);
  Anotaciones/metadata: @Transactional
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.desactivar(1);
  Anotaciones/metadata: @Transactional
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.activar(1);
  Anotaciones/metadata: @Transactional
- desbloquearCuenta (method)
  Proposito: Restablece condiciones de acceso o bloqueo.
  Parametros: - id: long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.desbloquearCuenta(1);
  Anotaciones/metadata: @Transactional
- listarPendientes (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: List<UsuarioRespuestaDTO>.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarPendientes();
  Anotaciones/metadata: @Transactional(readOnly = true)
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - id: Long. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.aprobar(1);
  Anotaciones/metadata: @Transactional
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.rechazar(1);
  Anotaciones/metadata: @Transactional
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - id: Long. Opcional: no.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: OperacionNoPermitidaException, RecursoNoEncontradoException
  Ejemplo de uso: var resultado = servicio.eliminar(1);
  Anotaciones/metadata: @Transactional
- convertirADTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - usuario: Usuario. Opcional: no.
  Retorno: UsuarioRespuestaDTO.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.convertirADTO(dato);
- validarContrasenaSegura (method)
  Proposito: Resuelve una responsabilidad puntual dentro de UsuarioService.java.
  Parametros: - contrasena: String. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: OperacionNoPermitidaException
  Ejemplo de uso: var resultado = servicio.validarContrasenaSegura("valor");

### sigea-backend/src/main/resources/application-altport.properties

Descripcion: Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: application-altport.properties

Notas:
- Claves destacadas: server.port.

Elementos internos:

#### Properties: application-altport.properties

Configura 1 claves de Spring/entorno.

### sigea-backend/src/main/resources/application-altport2.properties

Descripcion: Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: application-altport2.properties

Notas:
- Claves destacadas: server.port, spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password, spring.mail.properties.mail.smtp.auth, spring.mail.properties.mail.smtp.starttls.enable.

Elementos internos:

#### Properties: application-altport2.properties

Configura 7 claves de Spring/entorno.

### sigea-backend/src/main/resources/application-prod.properties

Descripcion: Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: application-prod.properties

Notas:
- Claves destacadas: sigea.auth.require-email-verification, server.port, spring.datasource.url, spring.datasource.username, spring.datasource.password, spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password, spring.mail.properties.mail.smtp.auth, spring.mail.properties.mail.smtp.starttls.enable, spring.mail.properties.mail.smtp.starttls.required, ...

Elementos internos:

#### Properties: application-prod.properties

Configura 21 claves de Spring/entorno.

### sigea-backend/src/main/resources/application.properties

Descripcion: Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: application.properties

Notas:
- Claves destacadas: spring.application.name, spring.config.import, spring.datasource.url, spring.datasource.username, spring.datasource.password, spring.datasource.driver-class-name, spring.datasource.hikari.pool-name, spring.datasource.hikari.maximum-pool-size, spring.datasource.hikari.minimum-idle, spring.datasource.hikari.connection-timeout, spring.datasource.hikari.idle-timeout, spring.datasource.hikari.max-lifetime, ...

Elementos internos:

#### Properties: application.properties

Configura 56 claves de Spring/entorno.

### sigea-backend/src/main/resources/db/migration/V10__alimentador_sub_ubicaciones_indices.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V10__alimentador_sub_ubicaciones_indices.sql

Notas:
- Modifica tabla ambiente.
- Modifica tabla equipo.

Elementos internos:

#### Sql: V10__alimentador_sub_ubicaciones_indices.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V11__crear_tabla_marcas.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V11__crear_tabla_marcas.sql

Notas:
- Crea tabla marca.

Elementos internos:

#### Sql: V11__crear_tabla_marcas.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V12__campos_equipo_placa_serial_modelo_marca.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V12__campos_equipo_placa_serial_modelo_marca.sql

Notas:
- Modifica tabla equipo.
- Actualiza datos en CASCADE.

Elementos internos:

#### Sql: V12__campos_equipo_placa_serial_modelo_marca.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V13__observacion_equipo.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V13__observacion_equipo.sql

Notas:
- Crea tabla observacion_equipo.

Elementos internos:

#### Sql: V13__observacion_equipo.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V14__prestamo_ambiente.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V14__prestamo_ambiente.sql

Notas:
- Crea tabla prestamo_ambiente.

Elementos internos:

#### Sql: V14__prestamo_ambiente.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V15__ambiente_propietario.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V15__ambiente_propietario.sql

Notas:
- Modifica tabla ambiente.
- Actualiza datos en ambiente.

Elementos internos:

#### Sql: V15__ambiente_propietario.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V1__crear_tablas.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V1__crear_tablas.sql

Notas:
- Crea tabla usuario.
- Crea tabla categoria.
- Crea tabla ambiente.
- Crea tabla equipo.
- Crea tabla foto_equipo.
- Crea tabla prestamo.
- Crea tabla detalle_prestamo.
- Crea tabla extension_prestamo.
- Crea tabla reporte_dano.
- Crea tabla reserva.
- Crea tabla transferencia.
- Crea tabla mantenimiento.

Elementos internos:

#### Sql: V1__crear_tablas.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V2__corregir_columnas_prestamo.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V2__corregir_columnas_prestamo.sql

Notas:
- Modifica tabla prestamo.

Elementos internos:

#### Sql: V2__corregir_columnas_prestamo.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V3__insertar_categorias_default.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V3__insertar_categorias_default.sql

Notas:
- ⚠️ No se pudieron inferir operaciones principales solo por analisis estatico; revisar sentencias manualmente si se usa en produccion.

Elementos internos:

#### Sql: V3__insertar_categorias_default.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V4__usuario_email_verificado_y_token.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V4__usuario_email_verificado_y_token.sql

Notas:
- Modifica tabla usuario.

Elementos internos:

#### Sql: V4__usuario_email_verificado_y_token.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V5__prestamo_reserva_id.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V5__prestamo_reserva_id.sql

Notas:
- Modifica tabla prestamo.

Elementos internos:

#### Sql: V5__prestamo_reserva_id.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V6__ambiente_ruta_foto.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V6__ambiente_ruta_foto.sql

Notas:
- Modifica tabla ambiente.

Elementos internos:

#### Sql: V6__ambiente_ruta_foto.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V7__inventario_por_instructor_y_transferencia.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V7__inventario_por_instructor_y_transferencia.sql

Notas:
- Crea tabla transferencia.
- Modifica tabla ambiente.
- Modifica tabla equipo.
- Actualiza datos en equipo.

Elementos internos:

#### Sql: V7__inventario_por_instructor_y_transferencia.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V8__estado_aprobacion_usuario.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V8__estado_aprobacion_usuario.sql

Notas:
- Modifica tabla usuario.
- Actualiza datos en adicional.

Elementos internos:

#### Sql: V8__estado_aprobacion_usuario.sql

Script SQL ejecutable.

### sigea-backend/src/main/resources/db/migration/V9__tipo_uso_equipo.sql

Descripcion: Migracion Flyway versionada; cambia estructura o datos base de la base de datos.

Dependencias:
- Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL.

Exports:
- sql: V9__tipo_uso_equipo.sql

Notas:
- Modifica tabla equipo.
- Actualiza datos en equipo.

Elementos internos:

#### Sql: V9__tipo_uso_equipo.sql

Script SQL ejecutable.

### sigea-backend/src/test/java/co/edu/sena/sigea/ApiIntegracionTest.java

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Otros: dependencias auxiliares (user, get, jsonPath, status, DisplayName).
- Spring: soporte de web, seguridad, DI o persistencia mediante Autowired, AutoConfigureMockMvc, SpringBootTest, MediaType, ActiveProfiles.

Exports:
- class: ApiIntegracionTest

Notas:
- Paquete Java: co.edu.sena.sigea.

Elementos internos:

#### Class: ApiIntegracionTest

Test de integración: verifica que el contexto Spring carga, que Actuator responde y que un endpoint protegido (dashboard) responde correctamente con un usuario ADMINISTRADOR mockeado.

Campos/props principales:
- mockMvc: MockMvc. Opcional: no.

Miembros principales:
- actuatorHealthRespondeOk (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ApiIntegracionTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: Exception
  Ejemplo de uso: var resultado = servicio.actuatorHealthRespondeOk();
  Anotaciones/metadata: @Test, @DisplayName("actuator health responde 200")
- dashboardSinAuthRetornaNoAutorizado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ApiIntegracionTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: Exception
  Ejemplo de uso: var resultado = servicio.dashboardSinAuthRetornaNoAutorizado();
- dashboardConAdminRetornaEstadisticas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ApiIntegracionTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: Exception
  Ejemplo de uso: var resultado = servicio.dashboardConAdminRetornaEstadisticas();
  Anotaciones/metadata: @Test, @DisplayName("dashboard estadísticas retorna 200 y JSON con rol ADMINISTRADOR")

### sigea-backend/src/test/java/co/edu/sena/sigea/dashboard/service/DashboardServicioTest.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Expone estadisticas para el tablero principal.

Dependencias:
- Otros: dependencias auxiliares (assertThat, when, DisplayName, Test, ExtendWith).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (EstadoPrestamo, EstadoReserva, AmbienteRepository, CategoriaRepository, EquipoRepository).

Exports:
- class: DashboardServicioTest

Notas:
- Paquete Java: co.edu.sena.sigea.dashboard.service.

Elementos internos:

#### Class: DashboardServicioTest

Class del archivo DashboardServicioTest.java.

Campos/props principales:
- equipoRepository: EquipoRepository. Opcional: no.
- categoriaRepository: CategoriaRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- prestamoRepository: PrestamoRepository. Opcional: no.
- reservaRepository: ReservaRepository. Opcional: no.
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- transferenciaRepository: TransferenciaRepository. Opcional: no.
- servicio: DashboardServicio. Opcional: no.

Miembros principales:
- obtenerEstadisticasRetornaConteos (method)
  Proposito: Recupera informacion consolidada del dominio.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.obtenerEstadisticasRetornaConteos();
  Anotaciones/metadata: @Test, @DisplayName("obtenerEstadisticas retorna DTO con conteos desde repositorios")

### sigea-backend/src/test/java/co/edu/sena/sigea/mantenimiento/service/MantenimientoServicioTest.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Registra mantenimientos preventivos/correctivos de equipos.

Dependencias:
- Otros: dependencias auxiliares (assertThat, assertThatThrownBy, any, verify, when).
- Java SE: estructuras base y utilidades de lenguaje (LocalDate, List, Optional).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (TipoMantenimiento, OperacionNoPermitidaException, RecursoNoEncontradoException, Equipo, EquipoRepository).

Exports:
- class: MantenimientoServicioTest
- class: Crear
- class: Cerrar

Notas:
- Paquete Java: co.edu.sena.sigea.mantenimiento.service.

Elementos internos:

#### Class: MantenimientoServicioTest

Class del archivo MantenimientoServicioTest.java.

Campos/props principales:
- mantenimientoRepository: MantenimientoRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- servicio: MantenimientoServicio. Opcional: no.
- equipo: Equipo. Opcional: no.
- crearDTO: MantenimientoCrearDTO. Opcional: no.

Miembros principales:
- setUp (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.setUp();
  Anotaciones/metadata: @BeforeEach
- lanzaCuandoEquipoNoExiste (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoEquipoNoExiste();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando equipo no existe")
- lanzaCuandoEquipoInactivo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoEquipoInactivo();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando equipo no está activo")
- lanzaCuandoFechaFinAnterior (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoFechaFinAnterior();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando fecha fin es anterior a fecha inicio")
- guardaYRetornaDTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.guardaYRetornaDTO();
  Anotaciones/metadata: @Test, @DisplayName("guarda mantenimiento y retorna DTO cuando datos válidos")
- lanzaCuandoNoExiste (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoNoExiste();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando mantenimiento no existe")
- lanzaCuandoYaCerrado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoYaCerrado();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando ya está cerrado")
- cierraCorrectamente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de MantenimientoServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.cierraCorrectamente();
  Anotaciones/metadata: @Test, @DisplayName("actualiza fecha fin y retorna DTO cuando en curso")
- listarTodosVacio (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodosVacio();
  Anotaciones/metadata: @Test, @DisplayName("listarTodos retorna lista vacía cuando no hay datos")

#### Class: Crear

Class del archivo MantenimientoServicioTest.java.

#### Class: Cerrar

Class del archivo MantenimientoServicioTest.java.

### sigea-backend/src/test/java/co/edu/sena/sigea/SigeaBackendApplicationTests.java

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Otros: dependencias auxiliares (Test).
- Spring: soporte de web, seguridad, DI o persistencia mediante SpringBootTest, ActiveProfiles.

Exports:
- class: SigeaBackendApplicationTests

Notas:
- Paquete Java: co.edu.sena.sigea.

Elementos internos:

#### Class: SigeaBackendApplicationTests

Class del archivo SigeaBackendApplicationTests.java.

Miembros principales:
- contextLoads (method)
  Proposito: Resuelve una responsabilidad puntual dentro de SigeaBackendApplicationTests.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.contextLoads();
  Anotaciones/metadata: @Test

### sigea-backend/src/test/java/co/edu/sena/sigea/TestConfig.java

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Spring: soporte de web, seguridad, DI o persistencia mediante Bean, Configuration, Profile, JavaMailSender.
- Otros: dependencias auxiliares (mock).

Exports:
- class: TestConfig

Notas:
- Paquete Java: co.edu.sena.sigea.

Elementos internos:

#### Class: TestConfig

Configuración para tests: proporciona beans mock cuando no hay servidor de correo.

Miembros principales:
- javaMailSender (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TestConfig.java.
  Parametros: Sin parametros.
  Retorno: JavaMailSender.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.javaMailSender();
  Anotaciones/metadata: @Bean

### sigea-backend/src/test/java/co/edu/sena/sigea/transferencia/service/TransferenciaServicioTest.java

Descripcion: Implementa reglas de negocio y orquestacion del dominio. Registra traspasos de equipos entre responsables o ubicaciones.

Dependencias:
- Otros: dependencias auxiliares (assertThat, assertThatThrownBy, any, verify, when).
- Java SE: estructuras base y utilidades de lenguaje (LocalDateTime, List, Optional).
- SIGEA interno: reutiliza contratos o servicios del propio dominio (Ambiente, AmbienteRepository, OperacionNoPermitidaException, RecursoNoEncontradoException, Equipo).

Exports:
- class: TransferenciaServicioTest
- class: Crear
- class: ListarYBuscar

Notas:
- Paquete Java: co.edu.sena.sigea.transferencia.service.

Elementos internos:

#### Class: TransferenciaServicioTest

Class del archivo TransferenciaServicioTest.java.

Campos/props principales:
- transferenciaRepository: TransferenciaRepository. Opcional: no.
- equipoRepository: EquipoRepository. Opcional: no.
- ambienteRepository: AmbienteRepository. Opcional: no.
- usuarioRepository: UsuarioRepository. Opcional: no.
- servicio: TransferenciaServicio. Opcional: no.
- admin: Usuario. Opcional: no.
- instructorOrigen: Usuario. Opcional: no.
- instructorDestino: Usuario. Opcional: no.
- equipo: Equipo. Opcional: no.
- ubicacionOrigen: Ambiente. Opcional: no.
- ubicacionDestino: Ambiente. Opcional: no.
- dto: TransferenciaCrearDTO. Opcional: no.

Miembros principales:
- setUp (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.setUp();
  Anotaciones/metadata: @BeforeEach
- lanzaCuandoOrigenYDestinoIguales (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoOrigenYDestinoIguales();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando origen y destino son iguales")
- lanzaCuandoAdminNoExiste (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoAdminNoExiste();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando usuario admin no existe")
- lanzaCuandoEquipoNoExiste (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoEquipoNoExiste();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando equipo no existe")
- lanzaCuandoEquipoInactivo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoEquipoInactivo();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando equipo no está activo")
- lanzaCuandoCantidadSuperaDisponible (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.lanzaCuandoCantidadSuperaDisponible();
  Anotaciones/metadata: @Test, @DisplayName("lanza cuando cantidad supera disponible")
- guardaYRetornaDTO (method)
  Proposito: Resuelve una responsabilidad puntual dentro de TransferenciaServicioTest.java.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.guardaYRetornaDTO();
  Anotaciones/metadata: @Test, @DisplayName("guarda transferencia y retorna DTO cuando datos válidos")
- listarTodosVacio (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.listarTodosVacio();
  Anotaciones/metadata: @Test, @DisplayName("listarTodos retorna lista vacía cuando no hay datos")
- buscarPorIdNoExiste (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Consulta o modifica datos persistidos en la base de datos.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: var resultado = servicio.buscarPorIdNoExiste();
  Anotaciones/metadata: @Test, @DisplayName("buscarPorId lanza cuando no existe")

#### Class: Crear

Class del archivo TransferenciaServicioTest.java.

#### Class: ListarYBuscar

Class del archivo TransferenciaServicioTest.java.

### sigea-backend/src/test/resources/application-test.properties

Descripcion: Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend.

Dependencias:
- Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente.

Exports:
- properties: application-test.properties

Notas:
- Claves destacadas: spring.application.name, spring.datasource.url, spring.datasource.driver-class-name, spring.datasource.username, spring.datasource.password, spring.jpa.hibernate.ddl-auto, spring.jpa.show-sql, spring.jpa.properties.hibernate.dialect, spring.flyway.enabled, sigea.jwt.secret, sigea.jwt.expiration-ms, spring.mail.host, ...

Elementos internos:

#### Properties: application-test.properties

Configura 15 claves de Spring/entorno.

## Modulo: sigea-frontend/.editorconfig

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/.editorconfig

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: .editorconfig

Elementos internos:

#### Archivo: .editorconfig

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-frontend/.vscode

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/.vscode/extensions.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: extensions.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: extensions.json

Configuracion declarativa sin exports ejecutables.

### sigea-frontend/.vscode/launch.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: launch.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: launch.json

Configuracion declarativa sin exports ejecutables.

### sigea-frontend/.vscode/tasks.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: tasks.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: tasks.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/Dockerfile

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/Dockerfile

Descripcion: Artefacto de despliegue para empaquetado y entrega del servicio.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: Dockerfile

Elementos internos:

#### Archivo: Dockerfile

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-frontend/README.md

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/README.md

Descripcion: Documento auxiliar del repositorio con instrucciones o contexto funcional.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: README.md

Elementos internos:

#### Archivo: README.md

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-frontend/angular.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/angular.json

Descripcion: Configura el workspace Angular, builds, budgets y assets.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: angular.json

Notas:
- Claves de nivel superior: $schema, version, newProjectRoot, projects.

Elementos internos:

#### Json: angular.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/nginx.conf

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/nginx.conf

Descripcion: Artefacto de despliegue para empaquetado y entrega del servicio.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: nginx.conf

Elementos internos:

#### Archivo: nginx.conf

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-frontend/package-lock.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/package-lock.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: package-lock.json

Notas:
- Claves de nivel superior: name, version, lockfileVersion, requires, packages.

Elementos internos:

#### Json: package-lock.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/package.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/package.json

Descripcion: Declara dependencias npm y scripts operativos del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: package.json

Notas:
- Claves de nivel superior: name, version, scripts, private, dependencies, devDependencies.

Elementos internos:

#### Json: package.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/proxy.conf.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/proxy.conf.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: proxy.conf.json

Notas:
- Claves de nivel superior: /api.

Elementos internos:

#### Json: proxy.conf.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/src

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/src/app/app.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: app.component.html

Elementos internos:

#### Archivo: app.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/app.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: app.component.scss

Elementos internos:

#### Archivo: app.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/app.component.ts

Descripcion: Describe una pieza de configuracion o soporte del proyecto.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (component).

Exports:
- class: AppComponent

Elementos internos:

#### Class: AppComponent

Clase/Componente Angular definido en app.component.ts.

Campos/props principales:
- title: inferido. Opcional: no.

### sigea-frontend/src/app/app.config.ts

Descripcion: Bootstrap de proveedores globales de Angular: router, HttpClient e interceptor JWT.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router, @angular/common/http.
- Otros: dependencias auxiliares (routes, interceptor).

Exports:
- const: appConfig

Elementos internos:

#### Const: appConfig

Constante exportada por app.config.ts.

### sigea-frontend/src/app/app.routes.ts

Descripcion: Declara la navegacion principal, lazy loading y guards por ruta.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/router.
- Otros: dependencias auxiliares (guard, guard, guard, guard, guard).

Exports:
- const: routes

Elementos internos:

#### Const: routes

Constante exportada por app.routes.ts.

### sigea-frontend/src/app/core/guards/admin-or-instructor.guard.ts

Descripcion: Restringe navegacion frontend en funcion de autenticacion o rol activo.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (service).

Exports:
- const: adminOrInstructorGuard

Elementos internos:

#### Const: adminOrInstructorGuard

Constante exportada por admin-or-instructor.guard.ts.

### sigea-frontend/src/app/core/guards/admin.guard.ts

Descripcion: Restringe navegacion frontend en funcion de autenticacion o rol activo.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (service).

Exports:
- const: adminGuard

Elementos internos:

#### Const: adminGuard

Constante exportada por admin.guard.ts.

### sigea-frontend/src/app/core/guards/alimentador.guard.ts

Descripcion: Restringe navegacion frontend en funcion de autenticacion o rol activo.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (service).

Exports:
- const: alimentadorGuard

Elementos internos:

#### Const: alimentadorGuard

Constante exportada por alimentador.guard.ts.

### sigea-frontend/src/app/core/guards/auth.guard.ts

Descripcion: Restringe navegacion frontend en funcion de autenticacion o rol activo.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (service).

Exports:
- const: authGuard

Elementos internos:

#### Const: authGuard

Constante exportada por auth.guard.ts.

### sigea-frontend/src/app/core/guards/operativo.guard.ts

Descripcion: Restringe navegacion frontend en funcion de autenticacion o rol activo.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router.
- Otros: dependencias auxiliares (service).

Exports:
- const: operativoGuard

Elementos internos:

#### Const: operativoGuard

Constante exportada por operativo.guard.ts.

### sigea-frontend/src/app/core/interceptors/jwt.interceptor.ts

Descripcion: Intercepta solicitudes HTTP del frontend para adjuntar JWT o centralizar errores.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/common/http, @angular/core.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (service).

Exports:
- const: jwtInterceptor

Elementos internos:

#### Const: jwtInterceptor

Constante exportada por jwt.interceptor.ts.

### sigea-frontend/src/app/core/models/ambiente.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: SubUbicacionResumen
- interface: Ambiente
- interface: AmbienteCrear

Elementos internos:

#### Interface: SubUbicacionResumen

Contrato TypeScript exportado por ambiente.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombre: string. Opcional: no.
- ubicacion: string. Opcional: si.
- descripcion: string. Opcional: si.
- activo: boolean. Opcional: no.

#### Interface: Ambiente

Contrato TypeScript exportado por ambiente.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombre: string. Opcional: no.
- ubicacion: string. Opcional: si.
- descripcion: string. Opcional: si.
- direccion: string. Opcional: si.
- instructorResponsableId: number. Opcional: no.
- instructorResponsableNombre: string. Opcional: no.
- propietarioId: number. Opcional: si.
- propietarioNombre: string. Opcional: si.
- activo: boolean. Opcional: no.
- rutaFoto: string. Opcional: si.
- padreId: number. Opcional: si.
- padreNombre: string. Opcional: si.
- subUbicaciones: SubUbicacionResumen[]. Opcional: si.
- fechaCreacion: string. Opcional: si.
- fechaActualizacion: string. Opcional: si.

#### Interface: AmbienteCrear

Contrato TypeScript exportado por ambiente.model.ts.

Campos/props principales:
- nombre: string. Opcional: no.
- ubicacion: string. Opcional: si.
- descripcion: string. Opcional: si.
- direccion: string. Opcional: si.
- idInstructorResponsable: number | null. Opcional: no.
- padreId: number | null. Opcional: si.

### sigea-frontend/src/app/core/models/auth.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: LoginRequest
- interface: LoginResponse
- interface: UserSession
- interface: RegisterRequest
- interface: PasswordRecoveryRequest
- interface: PasswordResetRequest

Elementos internos:

#### Interface: LoginRequest

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- numeroDocumento: string. Opcional: no.
- contrasena: string. Opcional: no.

#### Interface: LoginResponse

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- token: string. Opcional: no.
- tipo: string. Opcional: no.
- nombreCompleto: string. Opcional: no.
- correoElectronico: string. Opcional: no.
- rol: string. Opcional: no.
- esSuperAdmin: boolean. Opcional: no.

#### Interface: UserSession

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombreCompleto: string. Opcional: no.
- correoElectronico: string. Opcional: no.
- numeroDocumento: string. Opcional: si.
- rol: string. Opcional: no.
- token: string. Opcional: no.
- esSuperAdmin: boolean. Opcional: no.

#### Interface: RegisterRequest

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- nombre: string. Opcional: no.
- tipoDocumento: string. Opcional: no.
- numeroDocumento: string. Opcional: no.
- correoElectronico: string. Opcional: no.
- programaFormacion: string. Opcional: si.
- telefono: string. Opcional: si.
- numeroFicha: string. Opcional: si.
- contrasena: string. Opcional: no.

#### Interface: PasswordRecoveryRequest

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- correo: string. Opcional: no.

#### Interface: PasswordResetRequest

Contrato TypeScript exportado por auth.model.ts.

Campos/props principales:
- correo: string. Opcional: no.
- codigo: string. Opcional: no.
- nuevaContrasena: string. Opcional: no.

### sigea-frontend/src/app/core/models/categoria.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Categoria

Elementos internos:

#### Interface: Categoria

Contrato TypeScript exportado por categoria.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombre: string. Opcional: no.
- descripcion: string. Opcional: si.
- activo: boolean. Opcional: no.
- fechaCreacion: string. Opcional: si.
- fechaActualizacion: string. Opcional: si.

### sigea-frontend/src/app/core/models/equipo.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Equipo
- interface: EquipoCrear
- type: EstadoEquipo
- type: TipoUsoEquipo

Elementos internos:

#### Interface: Equipo

Contrato TypeScript exportado por equipo.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombre: string. Opcional: no.
- descripcion: string. Opcional: si.
- codigoUnico: string. Opcional: no.
- placa: string. Opcional: si.
- serial: string. Opcional: si.
- modelo: string. Opcional: si.
- marcaId: number. Opcional: si.
- marcaNombre: string. Opcional: si.
- estadoEquipoEscala: number. Opcional: si.
- categoriaId: number. Opcional: no.
- categoriaNombre: string. Opcional: no.
- ambienteId: number. Opcional: no.
- ambienteNombre: string. Opcional: no.
- subUbicacionId: number. Opcional: si.
- subUbicacionNombre: string. Opcional: si.
- propietarioId: number. Opcional: si.
- propietarioNombre: string. Opcional: si.
- inventarioActualInstructorId: number. Opcional: si.
- inventarioActualInstructorNombre: string. Opcional: si.
- estado: EstadoEquipo. Opcional: no.
- cantidadTotal: number. Opcional: no.
- cantidadDisponible: number. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- umbralMinimo: number. Opcional: no.
- activo: boolean. Opcional: no.
- fotos: { id: number; rutaArchivo: string; nombreArchivo?: string }[]. Opcional: si.
- fechaCreacion: string. Opcional: si.
- fechaActualizacion: string. Opcional: si.

#### Interface: EquipoCrear

Contrato TypeScript exportado por equipo.model.ts.

Campos/props principales:
- nombre: string. Opcional: no.
- descripcion: string. Opcional: si.
- codigoUnico: string. Opcional: no.
- placa: string. Opcional: si.
- serial: string. Opcional: si.
- modelo: string. Opcional: si.
- marcaId: number | null. Opcional: si.
- categoriaId: number. Opcional: no.
- ambienteId: number. Opcional: no.
- subUbicacionId: number | null. Opcional: si.
- propietarioId: number | null. Opcional: si.
- cantidadTotal: number. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: no.
- umbralMinimo: number. Opcional: no.

#### Type: EstadoEquipo

Alias de tipo: | 'ACTIVO'
  | 'EN_MANTENIMIENTO'
  | 'DISPONIBLE'
  | 'EN_PRESTAMO'
  | 'DADO_DE_BAJA'.

#### Type: TipoUsoEquipo

Alias de tipo: 'CONSUMIBLE' | 'NO_CONSUMIBLE'.

### sigea-frontend/src/app/core/models/mantenimiento.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Mantenimiento
- interface: MantenimientoCrear
- interface: MantenimientoCerrar
- type: TipoMantenimiento

Elementos internos:

#### Interface: Mantenimiento

Contrato TypeScript exportado por mantenimiento.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- equipoId: number. Opcional: no.
- nombreEquipo: string. Opcional: no.
- codigoEquipo: string. Opcional: no.
- tipo: TipoMantenimiento. Opcional: no.
- descripcion: string. Opcional: no.
- fechaInicio: string. Opcional: no.
- fechaFin: string. Opcional: si.
- responsable: string. Opcional: no.
- observaciones: string. Opcional: si.
- fechaCreacion: string. Opcional: si.

#### Interface: MantenimientoCrear

Contrato TypeScript exportado por mantenimiento.model.ts.

Campos/props principales:
- equipoId: number. Opcional: no.
- tipo: TipoMantenimiento. Opcional: no.
- descripcion: string. Opcional: no.
- fechaInicio: string. Opcional: no.
- fechaFin: string. Opcional: si.
- responsable: string. Opcional: no.
- observaciones: string. Opcional: si.

#### Interface: MantenimientoCerrar

Contrato TypeScript exportado por mantenimiento.model.ts.

Campos/props principales:
- fechaFin: string. Opcional: no.
- observaciones: string. Opcional: si.

#### Type: TipoMantenimiento

Alias de tipo: 'PREVENTIVO' | 'CORRECTIVO'.

### sigea-frontend/src/app/core/models/marca.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Marca
- interface: MarcaCrear

Elementos internos:

#### Interface: Marca

Contrato TypeScript exportado por marca.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombre: string. Opcional: no.
- descripcion: string. Opcional: si.
- activo: boolean. Opcional: no.
- fechaCreacion: string. Opcional: si.
- fechaActualizacion: string. Opcional: si.

#### Interface: MarcaCrear

Contrato TypeScript exportado por marca.model.ts.

Campos/props principales:
- nombre: string. Opcional: no.
- descripcion: string. Opcional: si.

### sigea-frontend/src/app/core/models/notificacion.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Notificacion

Elementos internos:

#### Interface: Notificacion

Contrato TypeScript exportado por notificacion.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- usuarioDestinoId: number. Opcional: no.
- nombreUsuarioDestino: string. Opcional: no.
- tipoNotificacion: string. Opcional: no.
- mensaje: string. Opcional: no.
- titulo: string. Opcional: no.
- medioEnvio: string. Opcional: no.
- estadoEnvio: string. Opcional: no.
- leida: boolean. Opcional: no.
- fechaEnvio: string. Opcional: no.
- fechaCreacion: string. Opcional: si.

### sigea-frontend/src/app/core/models/prestamo-ambiente.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: PrestamoAmbiente
- interface: PrestamoAmbienteSolicitud
- interface: PrestamoAmbienteDevolucion
- type: EstadoPrestamoAmbiente
- type: TipoActividad

Elementos internos:

#### Interface: PrestamoAmbiente

Contrato TypeScript exportado por prestamo-ambiente.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- ambienteId: number. Opcional: no.
- ambienteNombre: string. Opcional: no.
- solicitanteId: number. Opcional: no.
- solicitanteNombre: string. Opcional: no.
- propietarioAmbienteId: number. Opcional: si.
- propietarioAmbienteNombre: string. Opcional: si.
- fechaInicio: string. Opcional: no.
- fechaFin: string. Opcional: no.
- horaInicio: string. Opcional: no.
- horaFin: string. Opcional: no.
- proposito: string. Opcional: no.
- numeroParticipantes: number. Opcional: si.
- tipoActividad: TipoActividad. Opcional: si.
- observacionesSolicitud: string. Opcional: si.
- estado: EstadoPrestamoAmbiente. Opcional: no.
- observacionesDevolucion: string. Opcional: si.
- estadoDevolucionAmbiente: number. Opcional: si.
- fechaSolicitud: string. Opcional: si.
- fechaAprobacion: string. Opcional: si.
- fechaDevolucion: string. Opcional: si.

#### Interface: PrestamoAmbienteSolicitud

Contrato TypeScript exportado por prestamo-ambiente.model.ts.

Campos/props principales:
- ambienteId: number. Opcional: no.
- fechaInicio: string. Opcional: no.
- fechaFin: string. Opcional: no.
- horaInicio: string. Opcional: no.
- horaFin: string. Opcional: no.
- proposito: string. Opcional: no.
- numeroParticipantes: number. Opcional: si.
- tipoActividad: TipoActividad. Opcional: si.
- observacionesSolicitud: string. Opcional: si.

#### Interface: PrestamoAmbienteDevolucion

Contrato TypeScript exportado por prestamo-ambiente.model.ts.

Campos/props principales:
- observacionesDevolucion: string. Opcional: si.
- estadoDevolucionAmbiente: number. Opcional: si.

#### Type: EstadoPrestamoAmbiente

Alias de tipo: | 'SOLICITADO'
  | 'APROBADO'
  | 'RECHAZADO'
  | 'ACTIVO'
  | 'DEVUELTO'
  | 'CANCELADO'.

#### Type: TipoActividad

Alias de tipo: 'CLASE' | 'TALLER' | 'EVALUACION' | 'REUNION' | 'OTRO'.

### sigea-frontend/src/app/core/models/prestamo.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Otros: dependencias auxiliares (model).

Exports:
- interface: DetallePrestamoRespuesta
- interface: Prestamo
- interface: DetallePrestamoCrear
- interface: PrestamoCrear
- interface: PrestamoDevolucionDetalle
- interface: PrestamoDevolucion
- type: EstadoPrestamo
- type: EstadoCondicion

Elementos internos:

#### Interface: DetallePrestamoRespuesta

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- equipoId: number. Opcional: no.
- nombreEquipo: string. Opcional: no.
- codigoEquipo: string. Opcional: no.
- cantidad: number. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: si.
- cantidadDevuelta: number. Opcional: si.
- estadoEquipoEntrega: EstadoCondicion. Opcional: si.
- observacionesEntrega: string. Opcional: si.
- estadoEquipoDevolucion: EstadoCondicion. Opcional: si.
- observacionesDevolucion: string. Opcional: si.
- devuelto: boolean. Opcional: si.

#### Interface: Prestamo

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- usuarioSolicitanteId: number. Opcional: no.
- nombreUsuarioSolicitante: string. Opcional: no.
- correoUsuarioSolicitante: string. Opcional: no.
- nombreAdministradorAprueba: string. Opcional: si.
- nombreAdministradorRecibe: string. Opcional: si.
- fechaHoraSolicitud: string. Opcional: no.
- fechaHoraAprobacion: string. Opcional: si.
- fechaHoraSalida: string. Opcional: si.
- fechaHoraDevolucionEstimada: string. Opcional: no.
- fechaHoraDevolucionReal: string. Opcional: si.
- estado: EstadoPrestamo. Opcional: no.
- observacionesGenerales: string. Opcional: si.
- extensionesRealizadas: number. Opcional: no.
- detalles: DetallePrestamoRespuesta[]. Opcional: no.

#### Interface: DetallePrestamoCrear

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- equipoId: number. Opcional: no.
- cantidad: number. Opcional: no.

#### Interface: PrestamoCrear

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- fechaHoraDevolucionEstimada: string. Opcional: no.
- observacionesGenerales: string. Opcional: si.
- detalles: DetallePrestamoCrear[]. Opcional: no.

#### Interface: PrestamoDevolucionDetalle

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- detalleId: number. Opcional: no.
- observacionesDevolucion: string. Opcional: no.
- estadoDevolucion: number. Opcional: no.

#### Interface: PrestamoDevolucion

Contrato TypeScript exportado por prestamo.model.ts.

Campos/props principales:
- detalles: PrestamoDevolucionDetalle[]. Opcional: no.

#### Type: EstadoPrestamo

Alias de tipo: 'SOLICITADO' | 'APROBADO' | 'ACTIVO' | 'DEVUELTO' | 'RECHAZADO' | 'EN_MORA'.

#### Type: EstadoCondicion

Alias de tipo: 'EXCELENTE' | 'BUENO' | 'REGULAR' | 'MALO'.

### sigea-frontend/src/app/core/models/reserva.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Otros: dependencias auxiliares (model).

Exports:
- interface: Reserva
- interface: ReservaCrear
- type: EstadoReserva

Elementos internos:

#### Interface: Reserva

Contrato TypeScript exportado por reserva.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- usuarioId: number. Opcional: no.
- nombreUsuario: string. Opcional: no.
- correoUsuario: string. Opcional: no.
- equipoId: number. Opcional: no.
- nombreEquipo: string. Opcional: no.
- codigoEquipo: string. Opcional: no.
- tipoUso: TipoUsoEquipo. Opcional: si.
- cantidad: number. Opcional: no.
- fechaHoraInicio: string. Opcional: no.
- fechaHoraFin: string. Opcional: no.
- estado: EstadoReserva. Opcional: no.
- fechaCreacion: string. Opcional: si.

#### Interface: ReservaCrear

Contrato TypeScript exportado por reserva.model.ts.

Campos/props principales:
- equipoId: number. Opcional: no.
- cantidad: number. Opcional: no.
- fechaHoraInicio: string. Opcional: no.

#### Type: EstadoReserva

Alias de tipo: 'ACTIVA' | 'CANCELADA' | 'COMPLETADA' | 'CUMPLIDA' | 'VENCIDA' | 'EXPIRADA' | 'PRESTADO'.

### sigea-frontend/src/app/core/models/transferencia.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Transferencia
- interface: TransferenciaCrear

Elementos internos:

#### Interface: Transferencia

Contrato TypeScript exportado por transferencia.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- equipoId: number. Opcional: no.
- nombreEquipo: string. Opcional: no.
- codigoEquipo: string. Opcional: no.
- inventarioOrigenInstructorId: number. Opcional: no.
- nombreInventarioOrigenInstructor: string. Opcional: no.
- inventarioDestinoInstructorId: number. Opcional: no.
- nombreInventarioDestinoInstructor: string. Opcional: no.
- propietarioEquipoId: number. Opcional: si.
- nombrePropietarioEquipo: string. Opcional: si.
- ubicacionDestinoId: number. Opcional: si.
- nombreUbicacionDestino: string. Opcional: si.
- cantidad: number. Opcional: no.
- administradorAutorizaId: number. Opcional: no.
- nombreAdministrador: string. Opcional: no.
- motivo: string. Opcional: si.
- fechaTransferencia: string. Opcional: no.
- fechaCreacion: string. Opcional: si.

#### Interface: TransferenciaCrear

Contrato TypeScript exportado por transferencia.model.ts.

Campos/props principales:
- equipoId: number. Opcional: no.
- instructorDestinoId: number. Opcional: no.
- ubicacionDestinoId: number. Opcional: si.
- cantidad: number. Opcional: no.
- motivo: string. Opcional: si.
- fechaTransferencia: string. Opcional: no.

### sigea-frontend/src/app/core/models/usuario.model.ts

Descripcion: Declara interfaces y tipos usados por servicios y componentes del frontend.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- interface: Usuario
- interface: UsuarioCrear
- type: Rol
- type: TipoDocumento
- type: EstadoAprobacion

Elementos internos:

#### Interface: Usuario

Contrato TypeScript exportado por usuario.model.ts.

Campos/props principales:
- id: number. Opcional: no.
- nombreCompleto: string. Opcional: no.
- tipoDocumento: string. Opcional: no.
- numeroDocumento: string. Opcional: no.
- correoElectronico: string. Opcional: no.
- telefono: string. Opcional: si.
- programaFormacion: string. Opcional: si.
- ficha: string. Opcional: si.
- rol: string. Opcional: no.
- esSuperAdmin: boolean. Opcional: no.
- activo: boolean. Opcional: no.
- estadoAprobacion: EstadoAprobacion. Opcional: si.
- fechaCreacion: string. Opcional: si.
- fechaActualizacion: string. Opcional: si.

#### Interface: UsuarioCrear

Contrato TypeScript exportado por usuario.model.ts.

Campos/props principales:
- nombreCompleto: string. Opcional: no.
- tipoDocumento: TipoDocumento. Opcional: no.
- numeroDocumento: string. Opcional: no.
- correoElectronico: string. Opcional: no.
- programaFormacion: string. Opcional: si.
- numeroFicha: string. Opcional: si.
- telefono: string. Opcional: si.
- contrasena: string. Opcional: no.
- rol: Rol. Opcional: no.

#### Type: Rol

Alias de tipo: 'ADMINISTRADOR' | 'INSTRUCTOR' | 'ALIMENTADOR_EQUIPOS' | 'APRENDIZ' | 'FUNCIONARIO' | 'USUARIO_ESTANDAR'.

#### Type: TipoDocumento

Alias de tipo: 'CC' | 'TI' | 'CE' | 'PP' | 'PEP'.

#### Type: EstadoAprobacion

Alias de tipo: 'PENDIENTE' | 'APROBADO'.

### sigea-frontend/src/app/core/services/ambiente.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: AmbienteService

Elementos internos:

#### Class: AmbienteService

Clase/Componente Angular definido en ambiente.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listar (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Ambiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listar().subscribe();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Ambiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- listarMiAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Ambiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMiAmbiente().subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Ambiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: AmbienteCrear. Opcional: no.; - archivo: File. Opcional: no.
  Retorno: Observable<Ambiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato, dato).subscribe();
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: number. Opcional: no.; - dto: AmbienteCrear. Opcional: no.
  Retorno: Observable<Ambiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.actualizar(1, dato).subscribe();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.activar(1).subscribe();
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.desactivar(1).subscribe();
- crearSinFoto (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: AmbienteCrear. Opcional: no.
  Retorno: Observable<Ambiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crearSinFoto(dato).subscribe();
- listarSubUbicaciones (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - padreId: number. Opcional: no.
  Retorno: Observable<SubUbicacionResumen[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarSubUbicaciones(1).subscribe();
- crearSubUbicacion (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - padreId: number. Opcional: no.; - dto: AmbienteCrear. Opcional: no.
  Retorno: Observable<Ambiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crearSubUbicacion(1, dato).subscribe();

### sigea-frontend/src/app/core/services/auth.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http, @angular/router.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment).

Exports:
- class: AuthService

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: AuthService

Clase/Componente Angular definido en auth.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.
- currentUser: inferido. Opcional: no.
- sessionExpired: inferido. Opcional: no.
- isLoggedIn: inferido. Opcional: no.
- user: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- isInstructor: inferido. Opcional: no.
- isAlimentadorEquipos: inferido. Opcional: no.
- isSuperAdmin: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.
- isOperativo: inferido. Opcional: no.

Miembros principales:
- login (method)
  Proposito: Autentica al usuario y entrega el contexto de sesion.
  Parametros: - credentials: LoginRequest. Opcional: no.
  Retorno: Observable<LoginResponse>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API., Lee o escribe datos de sesion en localStorage., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.login(dato).subscribe();
- logout (method)
  Proposito: Finaliza la sesion local del usuario.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Lee o escribe datos de sesion en localStorage., Dispara navegacion de rutas en el frontend., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.logout();
- markSessionExpired (method)
  Proposito: Marca un estado local o remoto como procesado.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Lee o escribe datos de sesion en localStorage., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.markSessionExpired();
- clearSessionExpired (method)
  Proposito: Resuelve una responsabilidad puntual dentro de auth.service.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.clearSessionExpired();
- getToken (method)
  Proposito: Resuelve una responsabilidad puntual dentro de auth.service.ts.
  Parametros: Sin parametros.
  Retorno: string | null.
  Efectos secundarios: Lee o escribe datos de sesion en localStorage.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.getToken();
- register (method)
  Proposito: Resuelve una responsabilidad puntual dentro de auth.service.ts.
  Parametros: - data: RegisterRequest. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.register(dato).subscribe();
- recuperarContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de auth.service.ts.
  Parametros: - data: PasswordRecoveryRequest. Opcional: no.
  Retorno: Observable<string>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.recuperarContrasena(dato).subscribe();
- restablecerContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de auth.service.ts.
  Parametros: - data: PasswordResetRequest. Opcional: no.
  Retorno: Observable<string>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.restablecerContrasena(dato).subscribe();
- verificarCodigo (method)
  Proposito: Valida un codigo, token o condicion del negocio.
  Parametros: - correo: string. Opcional: no.; - codigo: string. Opcional: no.
  Retorno: Observable<string>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.verificarCodigo("valor", "valor").subscribe();
- loadStoredUser (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: UserSession | null.
  Efectos secundarios: Lee o escribe datos de sesion en localStorage.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.loadStoredUser();

### sigea-frontend/src/app/core/services/categoria.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: CategoriaService

Elementos internos:

#### Class: CategoriaService

Clase/Componente Angular definido en categoria.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarActivas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Categoria[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarActivas().subscribe();
- listarTodas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Categoria[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodas().subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Categoria>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();

### sigea-frontend/src/app/core/services/dashboard.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment).

Exports:
- interface: DashboardEstadisticas
- interface: PrestamosPorMes
- interface: EquiposPorCategoria
- class: DashboardService

Elementos internos:

#### Interface: DashboardEstadisticas

Contrato TypeScript exportado por dashboard.service.ts.

Campos/props principales:
- totalEquipos: number. Opcional: no.
- equiposActivos: number. Opcional: no.
- totalCategorias: number. Opcional: no.
- totalAmbientes: number. Opcional: no.
- totalUsuarios: number. Opcional: no.
- prestamosSolicitados: number. Opcional: no.
- prestamosActivos: number. Opcional: no.
- prestamosEnMora: number. Opcional: no.
- prestamosDevueltos: number. Opcional: no.
- reservasActivas: number. Opcional: no.
- mantenimientosEnCurso: number. Opcional: no.
- totalTransferencias: number. Opcional: no.
- equiposStockBajo: number. Opcional: no.

#### Interface: PrestamosPorMes

Contrato TypeScript exportado por dashboard.service.ts.

Campos/props principales:
- mes: string. Opcional: no.
- cantidad: number. Opcional: no.

#### Interface: EquiposPorCategoria

Contrato TypeScript exportado por dashboard.service.ts.

Campos/props principales:
- categoriaNombre: string. Opcional: no.
- cantidad: number. Opcional: no.

#### Class: DashboardService

Clase/Componente Angular definido en dashboard.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- getEstadisticas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.service.ts.
  Parametros: Sin parametros.
  Retorno: Observable<DashboardEstadisticas>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.getEstadisticas().subscribe();
- getPrestamosPorMes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.service.ts.
  Parametros: Sin parametros.
  Retorno: Observable<PrestamosPorMes[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.getPrestamosPorMes().subscribe();
- getEquiposPorCategoria (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.service.ts.
  Parametros: Sin parametros.
  Retorno: Observable<EquiposPorCategoria[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.getEquiposPorCategoria().subscribe();

### sigea-frontend/src/app/core/services/equipo.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- interface: ObservacionEquipo
- class: EquipoService

Elementos internos:

#### Interface: ObservacionEquipo

Contrato TypeScript exportado por equipo.service.ts.

Campos/props principales:
- id: number. Opcional: no.
- equipoId: number. Opcional: no.
- prestamoId: number. Opcional: no.
- observaciones: string. Opcional: si.
- estadoDevolucion: number. Opcional: no.
- fechaRegistro: string. Opcional: no.
- usuarioRegistradorNombre: string. Opcional: si.

#### Class: EquipoService

Clase/Componente Angular definido en equipo.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarActivos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarActivos().subscribe();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- listarPorCategoria (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - categoriaId: number. Opcional: no.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorCategoria(1).subscribe();
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: number. Opcional: no.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorAmbiente(1).subscribe();
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: string. Opcional: no.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorEstado("valor").subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Equipo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: EquipoCrear. Opcional: no.
  Retorno: Observable<Equipo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: number. Opcional: no.; - dto: EquipoCrear. Opcional: no.
  Retorno: Observable<Equipo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.actualizar(1, dato).subscribe();
- cambiarEstado (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: number. Opcional: no.; - nuevoEstado: string. Opcional: no.
  Retorno: Observable<Equipo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.cambiarEstado(1, "valor").subscribe();
- darDeBaja (method)
  Proposito: Resuelve una responsabilidad puntual dentro de equipo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.darDeBaja(1).subscribe();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.activar(1).subscribe();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de equipo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminar(1).subscribe();
- subirFoto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de equipo.service.ts.
  Parametros: - equipoId: number. Opcional: no.; - archivo: File. Opcional: no.
  Retorno: Observable<.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.subirFoto(1, dato).subscribe();
- eliminarFoto (method)
  Proposito: Resuelve una responsabilidad puntual dentro de equipo.service.ts.
  Parametros: - equipoId: number. Opcional: no.; - fotoId: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminarFoto(1, 1).subscribe();
- listarMiInventario (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMiInventario().subscribe();
- listarMisEquipos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Equipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMisEquipos().subscribe();
- recuperarEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de equipo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Equipo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.recuperarEquipo(1).subscribe();
- listarObservaciones (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: number. Opcional: no.
  Retorno: Observable<ObservacionEquipo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarObservaciones(1).subscribe();

### sigea-frontend/src/app/core/services/mantenimiento.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: MantenimientoService

Elementos internos:

#### Class: MantenimientoService

Clase/Componente Angular definido en mantenimiento.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listar (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Mantenimiento[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listar().subscribe();
- listarPorEquipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - equipoId: number. Opcional: no.
  Retorno: Observable<Mantenimiento[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorEquipo(1).subscribe();
- listarPorTipo (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - tipo: string. Opcional: no.
  Retorno: Observable<Mantenimiento[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorTipo("valor").subscribe();
- listarEnCurso (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Mantenimiento[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarEnCurso().subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Mantenimiento>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: MantenimientoCrear. Opcional: no.
  Retorno: Observable<Mantenimiento>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- cerrar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimiento.service.ts.
  Parametros: - id: number. Opcional: no.; - dto: MantenimientoCerrar. Opcional: no.
  Retorno: Observable<Mantenimiento>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.cerrar(1, dato).subscribe();
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: number. Opcional: no.; - dto: MantenimientoCrear. Opcional: no.
  Retorno: Observable<Mantenimiento>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.actualizar(1, dato).subscribe();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimiento.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminar(1).subscribe();

### sigea-frontend/src/app/core/services/marca.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: MarcaService

Elementos internos:

#### Class: MarcaService

Clase/Componente Angular definido en marca.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarActivas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Marca[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarActivas().subscribe();
- listarTodas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Marca[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodas().subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Marca>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: MarcaCrear. Opcional: no.
  Retorno: Observable<Marca>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: number. Opcional: no.; - dto: MarcaCrear. Opcional: no.
  Retorno: Observable<Marca>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.actualizar(1, dato).subscribe();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.activar(1).subscribe();
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.desactivar(1).subscribe();

### sigea-frontend/src/app/core/services/notificacion.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: NotificacionService

Elementos internos:

#### Class: NotificacionService

Clase/Componente Angular definido en notificacion.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarMisNotificaciones (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Notificacion[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMisNotificaciones().subscribe();
- listarNoLeidas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Notificacion[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarNoLeidas().subscribe();
- contadorNoLeidas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de notificacion.service.ts.
  Parametros: Sin parametros.
  Retorno: Observable<.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.contadorNoLeidas().subscribe();
- marcarLeida (method)
  Proposito: Resuelve una responsabilidad puntual dentro de notificacion.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.marcarLeida(1).subscribe();
- marcarTodasLeidas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de notificacion.service.ts.
  Parametros: - ids: number[]. Opcional: no.
  Retorno: Observable<void[]>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.marcarTodasLeidas(1).subscribe();

### sigea-frontend/src/app/core/services/prestamo-ambiente.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment).

Exports:
- class: PrestamoAmbienteService

Elementos internos:

#### Class: PrestamoAmbienteService

Clase/Componente Angular definido en prestamo-ambiente.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<PrestamoAmbiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo-ambiente.service.ts.
  Parametros: - dto: PrestamoAmbienteSolicitud. Opcional: no.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.solicitar(dato).subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- listarMisSolicitudes (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<PrestamoAmbiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMisSolicitudes().subscribe();
- listarPorAmbiente (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - ambienteId: number. Opcional: no.
  Retorno: Observable<PrestamoAmbiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorAmbiente(1).subscribe();
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: Observable<PrestamoAmbiente[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorEstado(dato).subscribe();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo-ambiente.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.aprobar(1).subscribe();
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo-ambiente.service.ts.
  Parametros: - id: number. Opcional: no.; - motivo: string. Opcional: si.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.rechazar(1, "valor").subscribe();
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: number. Opcional: no.; - dto: PrestamoAmbienteDevolucion. Opcional: no.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.registrarDevolucion(1, dato).subscribe();
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo-ambiente.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<PrestamoAmbiente>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.cancelar(1).subscribe();

### sigea-frontend/src/app/core/services/prestamo.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: PrestamoService

Elementos internos:

#### Class: PrestamoService

Clase/Componente Angular definido en prestamo.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Prestamo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- listarMisPrestamos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Prestamo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMisPrestamos().subscribe();
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: string. Opcional: no.
  Retorno: Observable<Prestamo[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorEstado("valor").subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- solicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo.service.ts.
  Parametros: - dto: PrestamoCrear. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.solicitar(dato).subscribe();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.aprobar(1).subscribe();
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.rechazar(1).subscribe();
- registrarSalida (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.registrarSalida(1).subscribe();
- registrarDevolucion (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - id: number. Opcional: no.; - dto: PrestamoDevolucion. Opcional: no.
  Retorno: Observable<Prestamo>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.registrarDevolucion(1, dato).subscribe();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamo.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminar(1).subscribe();

### sigea-frontend/src/app/core/services/reporte.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment).

Exports:
- class: ReporteService

Elementos internos:

#### Class: ReporteService

Clase/Componente Angular definido en reporte.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- descargar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reporte.service.ts.
  Parametros: - url: string. Opcional: no.; - params: HttpParams. Opcional: no.; - nombreArchivo: string. Opcional: no.
  Retorno: void.
  Efectos secundarios: Realiza llamadas HTTP hacia la API., Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.descargar("valor", dato, "valor");
- reporteInventario (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reporte.service.ts.
  Parametros: - formato: 'xlsx' | 'pdf' = 'xlsx'. Opcional: no.; - inventarioInstructorId: number. Opcional: si.; - categoriaId: number. Opcional: si.; - estado: string. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.reporteInventario(dato, 1, 1, "valor");
- reportePrestamos (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reporte.service.ts.
  Parametros: - formato: 'xlsx' | 'pdf' = 'xlsx'. Opcional: no.; - usuarioId: number. Opcional: si.; - equipoId: number. Opcional: si.; - desde: string. Opcional: si.; - hasta: string. Opcional: si.; - estado: string. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.reportePrestamos(dato, 1, 1, "valor", "valor", "valor");
- reporteEquiposMasSolicitados (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reporte.service.ts.
  Parametros: - formato: 'xlsx' | 'pdf' = 'xlsx'. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.reporteEquiposMasSolicitados(dato);
- reporteUsuariosEnMora (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reporte.service.ts.
  Parametros: - formato: 'xlsx' | 'pdf' = 'xlsx'. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.reporteUsuariosEnMora(dato);

### sigea-frontend/src/app/core/services/reserva.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: ReservaService

Elementos internos:

#### Class: ReservaService

Clase/Componente Angular definido en reserva.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Reserva[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- listarMisReservas (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Reserva[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarMisReservas().subscribe();
- listarPorEstado (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - estado: string. Opcional: no.
  Retorno: Observable<Reserva[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorEstado("valor").subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Reserva>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: ReservaCrear. Opcional: no.
  Retorno: Observable<Reserva>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reserva.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.cancelar(1).subscribe();
- marcarEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reserva.service.ts.
  Parametros: - id: number. Opcional: no.; - fechaHoraDevolucion: string. Opcional: no.
  Retorno: Observable<Reserva>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.marcarEquipoRecogido(1, "valor").subscribe();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reserva.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminar(1).subscribe();

### sigea-frontend/src/app/core/services/transferencia.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: TransferenciaService

Elementos internos:

#### Class: TransferenciaService

Clase/Componente Angular definido en transferencia.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listar (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Transferencia[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listar().subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Transferencia>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: TransferenciaCrear. Opcional: no.
  Retorno: Observable<Transferencia>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- listarPorInstructorOrigen (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: number. Opcional: no.
  Retorno: Observable<Transferencia[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorInstructorOrigen(1).subscribe();
- listarPorInstructorDestino (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - instructorId: number. Opcional: no.
  Retorno: Observable<Transferencia[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorInstructorDestino(1).subscribe();

### sigea-frontend/src/app/core/services/ui-feedback.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core.

Exports:
- interface: UiToast
- interface: UiDialogOptions
- type: UiToastTone
- type: UiDialogTone
- class: UiFeedbackService

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Interface: UiToast

Contrato TypeScript exportado por ui-feedback.service.ts.

Campos/props principales:
- id: number. Opcional: no.
- tone: UiToastTone. Opcional: no.
- title: string. Opcional: no.
- message: string. Opcional: no.
- duration: number. Opcional: no.

#### Interface: UiDialogOptions

Contrato TypeScript exportado por ui-feedback.service.ts.

Campos/props principales:
- title: string. Opcional: no.
- message: string. Opcional: no.
- confirmText: string. Opcional: no.
- cancelText: string. Opcional: no.
- tone: UiDialogTone. Opcional: no.
- placeholder: string. Opcional: si.
- defaultValue: string. Opcional: si.

#### Type: UiToastTone

Alias de tipo: 'success' | 'error' | 'warning' | 'info'.

#### Type: UiDialogTone

Alias de tipo: 'success' | 'danger' | 'warning' | 'info'.

#### Class: UiFeedbackService

Clase/Componente Angular definido en ui-feedback.service.ts.

Campos/props principales:
- toasts: inferido. Opcional: no.
- dialog: inferido. Opcional: no.
- promptValue: inferido. Opcional: no.
- nextToastId: inferido. Opcional: no.
- dialogResolver: ((value: boolean | string | null). Opcional: no.

Miembros principales:
- alert (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - title = 'Notificación': any. Opcional: no.; - tone: UiToastTone = 'info'. Opcional: no.
  Retorno: Promise<void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.alert("valor", dato, dato);
- success (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - title = 'Operación completada': any. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.success("valor", dato);
- error (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - title = 'No se pudo completar': any. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.error("valor", dato);
- warning (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - title = 'Atención': any. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.warning("valor", dato);
- info (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - title = 'Información': any. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.info("valor", dato);
- confirm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - options: Partial<UiDialogOptions> = {}. Opcional: no.
  Retorno: Promise<boolean>.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.confirm("valor", dato);
- prompt (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - message: string. Opcional: no.; - defaultValue = '': any. Opcional: no.; - options: Partial<UiDialogOptions> = {}. Opcional: no.
  Retorno: Promise<string | null>.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.prompt("valor", dato, dato);
- setPromptValue (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - value: string. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.setPromptValue("valor");
- dismissToast (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.dismissToast(1);
- resolveDialog (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - result: boolean | string | null. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.resolveDialog("valor");
- showToast (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback.service.ts.
  Parametros: - tone: UiToastTone. Opcional: no.; - message: string. Opcional: no.; - title: string. Opcional: no.; - duration = 4000: any. Opcional: no.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.showToast(dato, "valor", "valor", dato);

### sigea-frontend/src/app/core/services/usuario.service.ts

Descripcion: Encapsula llamadas HTTP y reglas cliente del dominio en el frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common/http.
- RxJS: manejo reactivo de flujos HTTP y estado asincrono con rxjs.
- Otros: dependencias auxiliares (/environments/environment, model).

Exports:
- class: UsuarioService

Elementos internos:

#### Class: UsuarioService

Clase/Componente Angular definido en usuario.service.ts.

Campos/props principales:
- apiUrl: inferido. Opcional: no.

Miembros principales:
- listar (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Usuario[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listar().subscribe();
- listarTodos (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Usuario[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarTodos().subscribe();
- listarPorRol (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: - rol: string. Opcional: no.
  Retorno: Observable<Usuario[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPorRol("valor").subscribe();
- buscarPorId (method)
  Proposito: Busca un recurso puntual por criterio o identificador.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Usuario>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.buscarPorId(1).subscribe();
- crear (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: - dto: UsuarioCrear. Opcional: no.
  Retorno: Observable<Usuario>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.crear(dato).subscribe();
- actualizar (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - id: number. Opcional: no.; - dto: Partial<Usuario>. Opcional: no.
  Retorno: Observable<Usuario>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.actualizar(1, dato).subscribe();
- cambiarRol (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - id: number. Opcional: no.; - rol: string. Opcional: no.
  Retorno: Observable<Usuario>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.cambiarRol(1, "valor").subscribe();
- restablecerContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuario.service.ts.
  Parametros: - id: number. Opcional: no.; - nuevaContrasena: string. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.restablecerContrasena(1, "valor").subscribe();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.activar(1).subscribe();
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.desactivar(1).subscribe();
- desbloquear (method)
  Proposito: Restablece condiciones de acceso o bloqueo.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.desbloquear(1).subscribe();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuario.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.eliminar(1).subscribe();
- listarPendientes (method)
  Proposito: Obtiene una coleccion filtrada de registros.
  Parametros: Sin parametros.
  Retorno: Observable<Usuario[]>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.listarPendientes().subscribe();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuario.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<Usuario>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.aprobar(1).subscribe();
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuario.service.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Observable<void>.
  Efectos secundarios: Realiza llamadas HTTP hacia la API.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: this.servicio.rechazar(1).subscribe();

### sigea-frontend/src/app/layout/main-layout/main-layout.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: main-layout.component.html

Elementos internos:

#### Archivo: main-layout.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/layout/main-layout/main-layout.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: main-layout.component.scss

Elementos internos:

#### Archivo: main-layout.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/layout/main-layout/main-layout.component.ts

Descripcion: Controla el shell principal del frontend: sidebar, navbar, router-outlet y utilidades de sesion.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router, @angular/common.
- Otros: dependencias auxiliares (service, service, model, service).

Exports:
- class: MainLayoutComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: MainLayoutComponent

Clase/Componente Angular definido en main-layout.component.ts.

Campos/props principales:
- router: inferido. Opcional: no.
- notificacionService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- auth: inferido. Opcional: no.
- sidebarOpen: inferido. Opcional: no.
- notificationOpen: inferido. Opcional: no.
- userMenuOpen: inferido. Opcional: no.
- darkMode: inferido. Opcional: no.
- notificaciones: inferido. Opcional: no.
- contadorNoLeidas: inferido. Opcional: no.
- notificationsLoading: inferido. Opcional: no.
- markAllLoading: inferido. Opcional: no.
- user: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- isInstructor: inferido. Opcional: no.
- isOperativo: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadContador (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadContador();
- loadNotificaciones (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadNotificaciones();
- toggleNotifications (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: - event: Event. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleNotifications(dato);
- marcarLeida (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: - n: Notificacion. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.marcarLeida(dato);
- toggleSidebar (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: - event: Event. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleSidebar(dato);
- toggleUserMenu (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: - event: Event. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleUserMenu(dato);
- toggleTheme (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleTheme();
- logout (method)
  Proposito: Finaliza la sesion local del usuario.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.logout();
- goToLogin (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.goToLogin();
- getBreadcrumb (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getBreadcrumb();
- getRolLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: - rol: string. Opcional: si.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getRolLabel("valor");
- closeDropdowns (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeDropdowns();
- marcarTodasComoLeidas (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.marcarTodasComoLeidas();
- verTodasNotificaciones (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.verTodasNotificaciones();
- getNotificationTone (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: - tipo: string. Opcional: no.
  Retorno: 'success' | 'info' | 'warning' | 'danger'.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getNotificationTone("valor");
- getNotificationIcon (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: - tipo: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getNotificationIcon("valor");
- relativeTime (method)
  Proposito: Transforma datos para visualizacion en interfaz.
  Parametros: - value: string. Opcional: si.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.relativeTime("valor");
- onWindowResize (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onWindowResize();
- onDocumentClick (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: - event: MouseEvent. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onDocumentClick(dato);
- isMobileViewport (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: boolean.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isMobileViewport();
- syncResponsiveSidebar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de main-layout.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.syncResponsiveSidebar();

### sigea-frontend/src/app/pages/alimentador/alimentador.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: alimentador.component.html

Elementos internos:

#### Archivo: alimentador.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/alimentador/alimentador.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: alimentador.component.scss

Elementos internos:

#### Archivo: alimentador.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/alimentador/alimentador.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms, @angular/router.
- Otros: dependencias auxiliares (service, service, service, service, model).

Exports:
- class: AlimentadorComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: AlimentadorComponent

Clase/Componente Angular definido en alimentador.component.ts.

Campos/props principales:
- fb: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- authService: inferido. Opcional: no.
- router: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- instructores: inferido. Opcional: no.
- cargando: inferido. Opcional: no.
- mensajeExito: inferido. Opcional: no.
- mensajeError: inferido. Opcional: no.
- tabActiva: inferido. Opcional: no.
- equipoForm: FormGroup. Opcional: no.
- ambienteForm: FormGroup. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de alimentador.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- seleccionarTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de alimentador.component.ts.
  Parametros: - tab: 'equipo' | 'ambiente'. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.seleccionarTab(dato);
- crearEquipo (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.crearEquipo();
- crearAmbiente (method)
  Proposito: Crea un recurso o registro del dominio.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.crearAmbiente();
- cerrarSesion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de alimentador.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cerrarSesion();

### sigea-frontend/src/app/pages/ambientes/ambientes.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: ambientes.component.html

Elementos internos:

#### Archivo: ambientes.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/ambientes/ambientes.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: ambientes.component.scss

Elementos internos:

#### Archivo: ambientes.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/ambientes/ambientes.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, service, service).

Exports:
- class: AmbientesComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: AmbientesComponent

Clase/Componente Angular definido en ambientes.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.
- isInstructor: inferido. Opcional: no.
- canCreateAmbientes: inferido. Opcional: no.
- currentUser: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- usuarios: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- modalEquiposOpen: inferido. Opcional: no.
- editingId: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- equiposAmbiente: inferido. Opcional: no.
- ambienteEquiposNombre: inferido. Opcional: no.
- loadingEquipos: inferido. Opcional: no.
- formSaving: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- selectedAmbiente: inferido. Opcional: no.
- fotoArchivo: File | null. Opcional: no.
- modalSubUbicacionesOpen: inferido. Opcional: no.
- ambientePadreSeleccionado: inferido. Opcional: no.
- subUbicaciones: inferido. Opcional: no.
- loadingSubUbicaciones: inferido. Opcional: no.
- modalCrearSubOpen: inferido. Opcional: no.
- formSubSaving: inferido. Opcional: no.
- formSub: AmbienteCrear. Opcional: no.
- selectedSubUbicacion: inferido. Opcional: no.
- subEquipos: inferido. Opcional: no.
- loadingSubEquipos: inferido. Opcional: no.
- subUbicacionesExpanded: inferido. Opcional: no.
- form: AmbienteCrear. Opcional: no.
- filteredAmbientes: inferido. Opcional: no.
- equiposFiltradosPorSub: inferido. Opcional: no.
- instructoresParaSelect: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadAmbientes (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadAmbientes();
- openCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCreate();
- openEdit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEdit(dato);
- closeModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeModal();
- onFotoSelected (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - event: Event. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onFotoSelected(dato);
- submitForm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitForm();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.activar(dato);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.desactivar(dato);
- getInitials (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - nombre: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getInitials("valor");
- canManageAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - ambiente: Ambiente. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.canManageAmbiente(dato);
- getPropietarioLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - ambiente: Ambiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getPropietarioLabel(dato);
- openEquipos (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEquipos(dato);
- closeEquiposModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeEquiposModal();
- selectSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - sub: SubUbicacionResumen | null. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.selectSubUbicacion(dato);
- getAmbienteFotoUrl (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getAmbienteFotoUrl(dato);
- getEquipoFotoUrl (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: string.
  Efectos secundarios: Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getEquipoFotoUrl(dato);
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);
- openSubUbicaciones (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - padre: Ambiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openSubUbicaciones(dato);
- closeSubUbicaciones (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeSubUbicaciones();
- toggleSubUbicacionCard (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: - subId: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleSubUbicacionCard(1);
- getEquiposSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - subId: number. Opcional: no.
  Retorno: Equipo[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getEquiposSubUbicacion(1);
- getEquiposSubUbicacionCount (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - subId: number. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getEquiposSubUbicacionCount(1);
- perteneceASubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: - equipo: Equipo. Opcional: no.; - subId: number. Opcional: no.; - subNombre: string. Opcional: si.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.perteneceASubUbicacion(dato, 1, "valor");
- loadSubUbicaciones (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: - padreId: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadSubUbicaciones(1);
- openCrearSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCrearSubUbicacion();
- closeCrearSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeCrearSubUbicacion();
- submitSubUbicacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitSubUbicacion();

### sigea-frontend/src/app/pages/dashboard/dashboard.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: dashboard.component.html

Elementos internos:

#### Archivo: dashboard.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/dashboard/dashboard.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: dashboard.component.scss

Elementos internos:

#### Archivo: dashboard.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/dashboard/dashboard.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Expone estadisticas para el tablero principal.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/router.
- Otros: dependencias auxiliares (service, service, model, js).

Exports:
- class: DashboardComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: DashboardComponent

Clase/Componente Angular definido en dashboard.component.ts.

Campos/props principales:
- dashboardService: inferido. Opcional: no.
- prestamoService: inferido. Opcional: no.
- auth: inferido. Opcional: no.
- stats: inferido. Opcional: no.
- prestamosPorMes: inferido. Opcional: no.
- equiposPorCategoria: inferido. Opcional: no.
- ultimosPrestamos: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- lastUpdated: inferido. Opcional: no.
- chartPrestamos: Chart | null. Opcional: no.
- chartCategoria: Chart | null. Opcional: no.
- today: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- ngOnDestroy (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnDestroy();
- load (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.load();
- loadGraficos (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadGraficos();
- loadUltimosPrestamos (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadUltimosPrestamos();
- renderBarChart (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.renderBarChart();
- renderDoughnutChart (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.renderDoughnutChart();
- estadoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: - estado: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoLabel("valor");
- getGreeting (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getGreeting();
- getFirstName (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getFirstName();
- getRolLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de dashboard.component.ts.
  Parametros: - rol: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getRolLabel("valor");

### sigea-frontend/src/app/pages/inventario/inventario.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: inventario.component.html

Elementos internos:

#### Archivo: inventario.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/inventario/inventario.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: inventario.component.scss

Elementos internos:

#### Archivo: inventario.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/inventario/inventario.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms, @angular/router.
- Otros: dependencias auxiliares (service, service, service, service, service).

Exports:
- class: InventarioComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: InventarioComponent

Clase/Componente Angular definido en inventario.component.ts.

Campos/props principales:
- equipoService: inferido. Opcional: no.
- categoriaService: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- marcaService: inferido. Opcional: no.
- reporteService: inferido. Opcional: no.
- auth: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- router: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- equipos: inferido. Opcional: no.
- categorias: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- marcas: inferido. Opcional: no.
- usuarios: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- editingId: inferido. Opcional: no.
- savingForm: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- observaciones: inferido. Opcional: no.
- loadingObs: inferido. Opcional: no.
- detailTab: inferido. Opcional: no.
- filterCategoria: inferido. Opcional: no.
- filterEstado: inferido. Opcional: no.
- filterAmbiente: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- incluirDadosDeBaja: inferido. Opcional: no.
- vistaActual: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- isOperativo: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.
- isAlimentadorEquipos: inferido. Opcional: no.
- isSuperAdmin: inferido. Opcional: no.
- selectedEquipo: inferido. Opcional: no.
- fotoArchivo: File | null. Opcional: no.
- subUbicacionesAmbiente: inferido. Opcional: no.
- form: EquipoCrear. Opcional: no.
- canAssignOwner: inferido. Opcional: no.
- usuariosDisponibles: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadEquipos (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadEquipos();
- cambiarVista (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - vista: 'todos' | 'mi-inventario' | 'mis-equipos'. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cambiarVista(dato);
- openCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCreate();
- openEdit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEdit(dato);
- closeModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeModal();
- onAmbienteChanged (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - id: number | string. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onAmbienteChanged("valor");
- getNombreAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getNombreAmbiente(1);
- onFotoSelected (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - event: Event. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onFotoSelected(dato);
- submitForm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitForm();
- getNombrePropietarioSeleccionado (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getNombrePropietarioSeleccionado();
- getFotoUrl (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getFotoUrl(dato);
- exportar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - formato: 'xlsx' | 'pdf'. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.exportar(dato);
- darDeBaja (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.darDeBaja(dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.activar(dato);
- eliminarEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.eliminarEquipo(dato);
- estadoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - estado: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoLabel("valor");
- tipoUsoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - tipoUso: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tipoUsoLabel("valor");
- solicitarEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.solicitarEquipo(dato);
- openDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openDetalle(dato);
- closeDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeDetalle();
- switchDetailTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - tab: 'info' | 'observaciones'. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.switchDetailTab(dato);
- irAReserva (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.irAReserva(dato);
- recuperarEquipo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.recuperarEquipo(dato);
- estaTransferido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - e: Equipo. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estaTransferido(dato);
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);
- normalizarFormulario (method)
  Proposito: Resuelve una responsabilidad puntual dentro de inventario.component.ts.
  Parametros: Sin parametros.
  Retorno: EquipoCrear.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.normalizarFormulario();

### sigea-frontend/src/app/pages/login/login.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: login.component.html

Elementos internos:

#### Archivo: login.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/login/login.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: login.component.scss

Elementos internos:

#### Archivo: login.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/login/login.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/router, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, model).

Exports:
- class: LoginComponent

Elementos internos:

#### Class: LoginComponent

Clase/Componente Angular definido en login.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- router: inferido. Opcional: no.
- route: inferido. Opcional: no.
- activeTab: 'login' | 'register'. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- successMsg: inferido. Opcional: no.
- correoPendienteVerificar: inferido. Opcional: no.
- codigoVerificacion: inferido. Opcional: no.
- showVerificacionCodigo: inferido. Opcional: no.
- recoveryStep: 'request' | 'reset' | null. Opcional: no.
- recoveryCorreo: inferido. Opcional: no.
- recoveryCodigo: inferido. Opcional: no.
- recoveryNuevaContrasena: inferido. Opcional: no.
- recoveryConfirmPassword: inferido. Opcional: no.
- showRecoveryPassword: inferido. Opcional: no.
- numeroDocumento: inferido. Opcional: no.
- contrasena: inferido. Opcional: no.
- showPassword: inferido. Opcional: no.
- reg: RegisterRequest. Opcional: no.
- regConfirmPassword: inferido. Opcional: no.
- showRegPassword: inferido. Opcional: no.
- TIPOS_DOC: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- switchTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: - tab: 'login' | 'register'. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.switchTab(dato);
- onCodigoInput (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onCodigoInput();
- onRecoveryCodigoInput (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onRecoveryCodigoInput();
- openRecovery (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openRecovery();
- cancelRecovery (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cancelRecovery();
- solicitarRecuperacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.solicitarRecuperacion();
- restablecerContrasena (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.restablecerContrasena();
- enviarCodigoVerificacion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.enviarCodigoVerificacion();
- login (method)
  Proposito: Autentica al usuario y entrega el contexto de sesion.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.login();
- register (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.register();
- getPasswordStrength (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: - password: string. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getPasswordStrength("valor");
- resetRecoveryState (method)
  Proposito: Resuelve una responsabilidad puntual dentro de login.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.resetRecoveryState();

### sigea-frontend/src/app/pages/mantenimientos/mantenimientos.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mantenimientos.component.html

Elementos internos:

#### Archivo: mantenimientos.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/mantenimientos/mantenimientos.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mantenimientos.component.scss

Elementos internos:

#### Archivo: mantenimientos.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/mantenimientos/mantenimientos.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, model).

Exports:
- class: MantenimientosComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: MantenimientosComponent

Clase/Componente Angular definido en mantenimientos.component.ts.

Campos/props principales:
- mantenimientoService: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- mantenimientos: inferido. Opcional: no.
- equipos: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalCrear: inferido. Opcional: no.
- modalCerrar: inferido. Opcional: no.
- editingId: inferido. Opcional: no.
- selectedMantenimiento: inferido. Opcional: no.
- activeTab: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- savingForm: inferido. Opcional: no.
- closingForm: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- form: MantenimientoCrear. Opcional: no.
- cerrarForm: MantenimientoCerrar. Opcional: no.
- TIPOS: inferido. Opcional: no.
- TABS: inferido. Opcional: no.
- tabFilteredMantenimientos: inferido. Opcional: no.
- filteredMantenimientos: inferido. Opcional: no.

Miembros principales:
- tabCount (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - key: TabKey. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tabCount(dato);
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadMantenimientos (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadMantenimientos();
- setTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - tab: TabKey. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.setTab(dato);
- openCrear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCrear();
- openEditar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - m: Mantenimiento. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEditar(dato);
- submitCrear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitCrear();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - m: Mantenimiento. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.eliminar(dato);
- openCerrar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - m: Mantenimiento. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCerrar(dato);
- submitCerrar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitCerrar();
- formatDate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - s: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.formatDate("valor");
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mantenimientos.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);

### sigea-frontend/src/app/pages/marcas/marcas.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: marcas.component.html

Elementos internos:

#### Archivo: marcas.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/marcas/marcas.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: marcas.component.scss

Elementos internos:

#### Archivo: marcas.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/marcas/marcas.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, model).

Exports:
- class: MarcasComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: MarcasComponent

Clase/Componente Angular definido en marcas.component.ts.

Campos/props principales:
- marcaService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- marcas: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- saving: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- editingId: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- filterEstado: inferido. Opcional: no.
- form: MarcaCrear. Opcional: no.
- totalActivas: inferido. Opcional: no.
- totalInactivas: inferido. Opcional: no.
- filteredMarcas: inferido. Opcional: no.
- requestFactory: (). Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- cargar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cargar();
- openCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCreate();
- openEdit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: - marca: Marca. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEdit(dato);
- closeModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeModal();
- submitForm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitForm();
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - marca: Marca. Opcional: no.
  Retorno: Promise<void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.activar(dato);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - marca: Marca. Opcional: no.
  Retorno: Promise<void>.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.desactivar(dato);
- estadoBadgeClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de marcas.component.ts.
  Parametros: - activo: boolean. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoBadgeClass(true);

### sigea-frontend/src/app/pages/mi-ambiente/mi-ambiente.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mi-ambiente.component.html

Elementos internos:

#### Archivo: mi-ambiente.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/mi-ambiente/mi-ambiente.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: mi-ambiente.component.scss

Elementos internos:

#### Archivo: mi-ambiente.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/mi-ambiente/mi-ambiente.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/router.
- Otros: dependencias auxiliares (service, service, model).

Exports:
- class: MiAmbienteComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: MiAmbienteComponent

Clase/Componente Angular definido en mi-ambiente.component.ts.

Campos/props principales:
- ambienteService: inferido. Opcional: no.
- auth: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- isInstructor: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de mi-ambiente.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();

### sigea-frontend/src/app/pages/prestamos/prestamos.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: prestamos.component.html

Elementos internos:

#### Archivo: prestamos.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/prestamos/prestamos.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: prestamos.component.scss

Elementos internos:

#### Archivo: prestamos.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/prestamos/prestamos.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms, @angular/router, @angular/core.
- Otros: dependencias auxiliares (service, service, service, service, model).

Exports:
- class: DateFormatPipe
- class: PrestamosComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: DateFormatPipe

Clase/Componente Angular definido en prestamos.component.ts.

Miembros principales:
- transform (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - value: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.transform("valor");

#### Class: PrestamosComponent

Clase/Componente Angular definido en prestamos.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- prestamoService: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- router: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- prestamos: inferido. Opcional: no.
- equipos: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalSolicitar: inferido. Opcional: no.
- modalDevolucion: inferido. Opcional: no.
- activeTab: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- submitSaving: inferido. Opcional: no.
- devolucionSaving: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- devolucionDetalles: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- form: PrestamoCrear. Opcional: no.
- detalleEquipoId: inferido. Opcional: no.
- detalleCantidad: inferido. Opcional: no.
- tabs: inferido. Opcional: no.
- stats: inferido. Opcional: no.
- filteredPrestamos: inferido. Opcional: no.
- tabFilteredPrestamos: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Dispara navegacion de rutas en el frontend., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadPrestamos (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Transforma credenciales o datos sensibles antes de persistirlos., Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadPrestamos();
- updateTabCounts (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - list: Prestamo[]. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.updateTabCounts([]);
- setTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - tab: TabKey. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.setTab(dato);
- openSolicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openSolicitar();
- fechaMinDevolucion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.fechaMinDevolucion();
- agregarDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.agregarDetalle();
- quitarDetalle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - i: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.quitarDetalle(1);
- getEquipoName (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getEquipoName(1);
- getEquipoDisponible (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getEquipoDisponible(1);
- getCantidadSolicitada (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getCantidadSolicitada(1);
- submitSolicitud (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitSolicitud();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.aprobar(dato);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.rechazar(dato);
- registrarSalida (method)
  Proposito: Registra un evento o cambio de estado en el dominio.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.registrarSalida(dato);
- abrirDevolucion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.abrirDevolucion(dato);
- cerrarDevolucion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cerrarDevolucion();
- actualizarDetalleDevolucion (method)
  Proposito: Actualiza datos persistidos o estado del dominio.
  Parametros: - index: number. Opcional: no.; - patch: Partial<PrestamoDevolucionDetalle>. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.actualizarDetalleDevolucion(1, dato);
- detalleDevolucionNombre (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - detalleId: number. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.detalleDevolucionNombre(1);
- detalleDevolucionCantidad (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - detalleId: number. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.detalleDevolucionCantidad(1);
- submitDevolucion (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitDevolucion();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.eliminar(dato);
- estadoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - estado: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoLabel("valor");
- tipoUsoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - tipoUso: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tipoUsoLabel("valor");
- estadoCondicionLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - estado: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoCondicionLabel("valor");
- estadoCondicionClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - estado: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoCondicionClass("valor");
- isOverdue (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - p: Prestamo. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isOverdue(dato);
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);

### sigea-frontend/src/app/pages/prestamos-ambientes/prestamos-ambientes.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: prestamos-ambientes.component.html

Elementos internos:

#### Archivo: prestamos-ambientes.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/prestamos-ambientes/prestamos-ambientes.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: prestamos-ambientes.component.scss

Elementos internos:

#### Archivo: prestamos-ambientes.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/prestamos-ambientes/prestamos-ambientes.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, service, model).

Exports:
- class: PrestamosAmbientesComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: PrestamosAmbientesComponent

Clase/Componente Angular definido en prestamos-ambientes.component.ts.

Campos/props principales:
- svc: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- auth: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- prestamos: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- savingForm: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- vista: inferido. Opcional: no.
- filterEstado: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.
- form: PrestamoAmbienteSolicitud. Opcional: no.
- minFecha: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- cargar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cargar();
- openSolicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openSolicitar();
- closeModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeModal();
- submitSolicitar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitSolicitar();
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - p: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.aprobar(dato);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - p: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.rechazar(dato);
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - p: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cancelar(dato);
- estadoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoLabel(dato);
- estadoBadgeClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoBadgeClass(dato);
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de prestamos-ambientes.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);

### sigea-frontend/src/app/pages/reportes/reportes.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: reportes.component.html

Elementos internos:

#### Archivo: reportes.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/reportes/reportes.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: reportes.component.scss

Elementos internos:

#### Archivo: reportes.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/reportes/reportes.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, model, model).

Exports:
- class: ReportesComponent

Elementos internos:

#### Class: ReportesComponent

Clase/Componente Angular definido en reportes.component.ts.

Campos/props principales:
- reporteService: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- categoriaService: inferido. Opcional: no.
- instructores: inferido. Opcional: no.
- categorias: inferido. Opcional: no.
- selectedReport: string | null. Opcional: no.
- params: inferido. Opcional: no.
- reportTypes: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reportes.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- selectReport (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reportes.component.ts.
  Parametros: - key: string. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.selectReport("valor");
- getSelectedTitle (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reportes.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getSelectedTitle();
- exportar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reportes.component.ts.
  Parametros: - formato: 'xlsx' | 'pdf'. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.exportar(dato);
- quickExport (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reportes.component.ts.
  Parametros: - key: string. Opcional: no.; - formato: 'xlsx' | 'pdf'. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.quickExport("valor", dato);

### sigea-frontend/src/app/pages/reservas/reservas.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: reservas.component.html

Elementos internos:

#### Archivo: reservas.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/reservas/reservas.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: reservas.component.scss

Elementos internos:

#### Archivo: reservas.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/reservas/reservas.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms, @angular/router.
- Otros: dependencias auxiliares (service, service, service, service, service).

Exports:
- class: ReservasComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: ReservasComponent

Clase/Componente Angular definido en reservas.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- reservaService: inferido. Opcional: no.
- prestamoAmbienteService: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- router: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- reservas: inferido. Opcional: no.
- reservasAmbientes: inferido. Opcional: no.
- equipos: inferido. Opcional: no.
- ambientes: inferido. Opcional: no.
- agendaAmbiente: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- loadingAmbientes: inferido. Opcional: no.
- loadingUbicaciones: inferido. Opcional: no.
- loadingAgendaAmbiente: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalCrear: inferido. Opcional: no.
- modalCrearAmbiente: inferido. Opcional: no.
- modalEquipoRecogido: inferido. Opcional: no.
- modalDevolucionAmbiente: inferido. Opcional: no.
- fechaHoraDevolucion: inferido. Opcional: no.
- activeTab: inferido. Opcional: no.
- activeSection: inferido. Opcional: no.
- activeTabAmbientes: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- searchTermAmbientes: inferido. Opcional: no.
- createSaving: inferido. Opcional: no.
- createAmbienteSaving: inferido. Opcional: no.
- pickupSaving: inferido. Opcional: no.
- devolucionAmbienteSaving: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- agendaFecha: inferido. Opcional: no.
- calendarioMes: inferido. Opcional: no.
- mostrarCalendarioAmbiente: inferido. Opcional: no.
- isAdmin: inferido. Opcional: no.
- isAdminOrInstructor: inferido. Opcional: no.
- currentUser: inferido. Opcional: no.
- selectedAmbienteShellRef: inferido. Opcional: no.
- agendaCalendarCardRef: inferido. Opcional: no.

Miembros principales:
- tabCount (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - key: TabKey. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tabCount(dato);
- tabCountAmbientes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - key: TabAmbientes. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tabCountAmbientes(dato);
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadReservas (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadReservas();
- loadReservasAmbientes (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadReservasAmbientes();
- loadAmbientes (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadAmbientes();
- ensureEquiposLoaded (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - preselectedId: number. Opcional: si.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ensureEquiposLoaded(1);
- setTab (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - tab: TabKey. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.setTab(dato);
- setSection (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - section: SeccionReserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.setSection(dato);
- setTabAmbientes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - tab: TabAmbientes. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.setTabAmbientes(dato);
- fechaMinInicio (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.fechaMinInicio();
- fechaMinAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.fechaMinAmbiente();
- openCrear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCrear();
- openCrearAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCrearAmbiente();
- submitCrear (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitCrear();
- onSelectAmbienteReserva (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - ambienteId: number | string. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onSelectAmbienteReserva("valor");
- selectAmbienteCard (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - ambiente: Ambiente. Opcional: no.; - abrirCalendario = true: any. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.selectAmbienteCard(dato, dato);
- toggleCalendarioAmbiente (method)
  Proposito: Alterna un estado visual o de interfaz.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toggleCalendarioAmbiente();
- moverMes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - offsetMeses: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.moverMes(1);
- seleccionarDiaAgenda (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - iso: string. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.seleccionarDiaAgenda("valor");
- submitCrearAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitCrearAmbiente();
- cargarAgendaAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - ambienteId: number. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cargarAgendaAmbiente(1);
- cancelar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cancelar(dato);
- eliminarReserva (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.eliminarReserva(dato);
- aprobarAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.aprobarAmbiente(dato);
- rechazarAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.rechazarAmbiente(dato);
- openDevolucionAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openDevolucionAmbiente(dato);
- closeDevolucionAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeDevolucionAmbiente();
- submitDevolucionAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitDevolucionAmbiente();
- cancelarAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cancelarAmbiente(dato);
- estadoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - estado: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoLabel("valor");
- estadoAmbienteLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoAmbienteLabel(dato);
- estadoAmbienteBadgeClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - estado: EstadoPrestamoAmbiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoAmbienteBadgeClass(dato);
- tipoUsoLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - tipoUso: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.tipoUsoLabel("valor");
- openEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: Reserva. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEquipoRecogido(dato);
- closeEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeEquipoRecogido();
- submitEquipoRecogido (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitEquipoRecogido();
- isVencida (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: Reserva. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isVencida(dato);
- formatDate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - s: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.formatDate("valor");
- formatDateOnly (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - s: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.formatDateOnly("valor");
- getAmbienteInfo (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - id: number. Opcional: no.
  Retorno: Ambiente | undefined.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getAmbienteInfo(1);
- getAmbienteFotoUrl (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: Lee o escribe archivos en el almacenamiento local del servidor.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getAmbienteFotoUrl(dato);
- getTituloCalendarioMes (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: Sin parametros.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getTituloCalendarioMes();
- ambienteLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - a: Ambiente. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ambienteLabel(dato);
- puedeCancelarAmbiente (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.puedeCancelarAmbiente(dato);
- canManageAmbienteReservation (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - r: PrestamoAmbiente. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.canManageAmbienteReservation(dato);
- getInitials (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - nombre: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getInitials("valor");
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);
- toIsoDate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - date: Date. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toIsoDate(fecha);
- reservaOcurreEnFecha (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - reserva: PrestamoAmbiente. Opcional: no.; - fechaIso: string. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.reservaOcurreEnFecha(dato, "valor");
- scrollToReservaSection (method)
  Proposito: Resuelve una responsabilidad puntual dentro de reservas.component.ts.
  Parametros: - target: 'selected' | 'calendar'. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.scrollToReservaSection(dato);

### sigea-frontend/src/app/pages/transferencias/transferencias.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: transferencias.component.html

Elementos internos:

#### Archivo: transferencias.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/transferencias/transferencias.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: transferencias.component.scss

Elementos internos:

#### Archivo: transferencias.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/transferencias/transferencias.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, service, service).

Exports:
- class: TransferenciasComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: TransferenciasComponent

Clase/Componente Angular definido en transferencias.component.ts.

Campos/props principales:
- transferenciaService: inferido. Opcional: no.
- equipoService: inferido. Opcional: no.
- ambienteService: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- transferencias: inferido. Opcional: no.
- equipos: inferido. Opcional: no.
- ubicaciones: inferido. Opcional: no.
- instructores: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- saving: inferido. Opcional: no.
- form: TransferenciaCrear. Opcional: no.
- filteredTransferencias: inferido. Opcional: no.
- totalUnidades: inferido. Opcional: no.
- totalDestinos: inferido. Opcional: no.
- totalPropietarios: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de transferencias.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadTransferencias (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadTransferencias();
- openCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de transferencias.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCreate();
- submitForm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de transferencias.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitForm();
- formatDate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de transferencias.component.ts.
  Parametros: - s: string | undefined. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.formatDate("valor");

### sigea-frontend/src/app/pages/usuarios/usuarios.component.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: usuarios.component.html

Elementos internos:

#### Archivo: usuarios.component.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/usuarios/usuarios.component.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: usuarios.component.scss

Elementos internos:

#### Archivo: usuarios.component.scss

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/app/pages/usuarios/usuarios.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/forms.
- Otros: dependencias auxiliares (service, service, service, service, model).

Exports:
- class: UsuariosComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: UsuariosComponent

Clase/Componente Angular definido en usuarios.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- usuarioService: inferido. Opcional: no.
- prestamoService: inferido. Opcional: no.
- ui: inferido. Opcional: no.
- usuarios: inferido. Opcional: no.
- usuariosPendientes: inferido. Opcional: no.
- vistaActual: inferido. Opcional: no.
- prestamosCounts: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- error: inferido. Opcional: no.
- modalOpen: inferido. Opcional: no.
- modalEditOpen: inferido. Opcional: no.
- modalPasswordOpen: inferido. Opcional: no.
- editingUser: inferido. Opcional: no.
- passwordUser: inferido. Opcional: no.
- createSaving: inferido. Opcional: no.
- editSaving: inferido. Opcional: no.
- passwordSaving: inferido. Opcional: no.
- actionPending: inferido. Opcional: no.
- filterRol: inferido. Opcional: no.
- filterEstado: inferido. Opcional: no.
- searchTerm: inferido. Opcional: no.
- showResetPassword: inferido. Opcional: no.
- form: UsuarioCrear. Opcional: no.
- passwordForm: inferido. Opcional: no.
- showPassword: inferido. Opcional: no.
- isSuperAdmin: inferido. Opcional: no.
- ROLES: inferido. Opcional: no.
- TIPOS_DOC: inferido. Opcional: no.
- ROLES_VALIDOS: inferido. Opcional: no.
- filteredUsuarios: inferido. Opcional: no.

Miembros principales:
- ngOnInit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.ngOnInit();
- loadPendientes (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadPendientes();
- loadUsuarios (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadUsuarios();
- cambiarVista (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - vista: VistaUsuarios. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cambiarVista(dato);
- aprobar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.aprobar(dato);
- rechazar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.rechazar(dato);
- loadPrestamosCounts (method)
  Proposito: Carga datos desde el backend o desde almacenamiento local.
  Parametros: - usuarios: Usuario[]. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.loadPrestamosCounts([]);
- openCreate (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openCreate();
- closeModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeModal();
- submitForm (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitForm();
- cambiarRol (method)
  Proposito: Modifica un atributo relevante del recurso.
  Parametros: - u: Usuario. Opcional: no.; - event: Event. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cambiarRol(dato, dato);
- activar (method)
  Proposito: Rehabilita un recurso previamente inactivado.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.activar(dato);
- desactivar (method)
  Proposito: Marca un recurso como inactivo sin borrado fisico.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.desactivar(dato);
- openEdit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openEdit(dato);
- closeEditModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closeEditModal();
- openPasswordReset (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.openPasswordReset(dato);
- closePasswordResetModal (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.closePasswordResetModal();
- submitPasswordReset (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitPasswordReset();
- submitEdit (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitEdit();
- eliminar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - u: Usuario. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.eliminar(dato);
- getInitials (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - nombre: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getInitials("valor");
- getRolColor (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - rol: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getRolColor("valor");
- getRolLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - rol: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getRolLabel("valor");
- getPrestamoCount (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - userId: number. Opcional: no.
  Retorno: number.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getPrestamoCount(1);
- estadoAprobacionLabel (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - estado: string. Opcional: si.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoAprobacionLabel("valor");
- estadoAprobacionBadgeClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - estado: string. Opcional: si.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.estadoAprobacionBadgeClass("valor");
- isActionPending (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - action: string. Opcional: no.; - id: number. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.isActionPending("valor", 1);
- passwordEsSegura (method)
  Proposito: Resuelve una responsabilidad puntual dentro de usuarios.component.ts.
  Parametros: - password: string. Opcional: no.
  Retorno: boolean.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.passwordEsSegura("valor");

### sigea-frontend/src/app/pages/verificar-email/verificar-email.component.ts

Descripcion: Implementa una pantalla funcional del frontend. Agrupa pantallas funcionales cargadas por rutas.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/core, @angular/common, @angular/router, @angular/forms.
- Otros: dependencias auxiliares (service).

Exports:
- class: VerificarEmailComponent

Advertencias:
- ⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.

Elementos internos:

#### Class: VerificarEmailComponent

Clase/Componente Angular definido en verificar-email.component.ts.

Campos/props principales:
- auth: inferido. Opcional: no.
- correo: inferido. Opcional: no.
- codigo: inferido. Opcional: no.
- loading: inferido. Opcional: no.
- mensaje: inferido. Opcional: no.
- error: inferido. Opcional: no.

Miembros principales:
- onCodigoInput (method)
  Proposito: Resuelve una responsabilidad puntual dentro de verificar-email.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onCodigoInput();
- enviar (method)
  Proposito: Resuelve una responsabilidad puntual dentro de verificar-email.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: Actualiza estado reactivo local del frontend.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.enviar();

### sigea-frontend/src/app/shared/ui-feedback-host.component.ts

Descripcion: Componentes y utilidades reutilizables del frontend.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/common, @angular/core, @angular/forms.
- Otros: dependencias auxiliares (service).

Exports:
- class: UiFeedbackHostComponent

Elementos internos:

#### Class: UiFeedbackHostComponent

Clase/Componente Angular definido en ui-feedback-host.component.ts.

Campos/props principales:
- ui: inferido. Opcional: no.

Miembros principales:
- onKeydown (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: - event: KeyboardEvent. Opcional: no.
  Retorno: void.
  Efectos secundarios: Interactua con el DOM o con APIs del navegador.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.onKeydown(dato);
- toastIcon (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: - tone: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.toastIcon("valor");
- dialogIcon (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: - tone: string. Opcional: no.; - type: 'confirm' | 'prompt'. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.dialogIcon("valor", dato);
- primaryButtonClass (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: - tone: string. Opcional: no.
  Retorno: string.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.primaryButtonClass("valor");
- cancelDialog (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.cancelDialog();
- confirmDialog (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: Sin parametros.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.confirmDialog();
- submitPrompt (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: - event: Event. Opcional: no.
  Retorno: void.
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.submitPrompt(dato);
- getFocusableElements (method)
  Proposito: Resuelve una responsabilidad puntual dentro de ui-feedback-host.component.ts.
  Parametros: Sin parametros.
  Retorno: HTMLElement[].
  Efectos secundarios: No se detectan efectos secundarios relevantes fuera del retorno.
  Errores: No se infieren excepciones propias mas alla de errores del framework o validacion.
  Ejemplo de uso: component.getFocusableElements();

### sigea-frontend/src/environments/environment.prod.ts

Descripcion: Define configuracion de build y base URL de API.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- const: environment

Elementos internos:

#### Const: environment

Constante exportada por environment.prod.ts.

### sigea-frontend/src/environments/environment.ts

Descripcion: Define configuracion de build y base URL de API.

Dependencias:
- Sin dependencias explicitas detectadas.

Exports:
- const: environment

Elementos internos:

#### Const: environment

Constante exportada por environment.ts.

### sigea-frontend/src/index.html

Descripcion: Template asociado a un componente Angular; define estructura visual y bindings.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: index.html

Elementos internos:

#### Archivo: index.html

Archivo no ejecutable o de soporte visual/configuracional.

### sigea-frontend/src/main.ts

Descripcion: Punto de entrada del frontend que inicia la aplicacion standalone.

Dependencias:
- Angular: bootstrap, componentes, router o HttpClient mediante @angular/platform-browser.
- Otros: dependencias auxiliares (config, component).

Exports:
- archivo: main.ts

Elementos internos:

#### Archivo: main.ts

Archivo de soporte sin exports TypeScript detectados.

### sigea-frontend/src/styles.scss

Descripcion: Hoja de estilos del componente o layout asociado.

Dependencias:
- Consumido por herramientas, runtime o por desarrolladores como apoyo operativo.

Exports:
- archivo: styles.scss

Elementos internos:

#### Archivo: styles.scss

Archivo no ejecutable o de soporte visual/configuracional.

## Modulo: sigea-frontend/tsconfig.app.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/tsconfig.app.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: tsconfig.app.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: tsconfig.app.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/tsconfig.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/tsconfig.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: tsconfig.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: tsconfig.json

Configuracion declarativa sin exports ejecutables.

## Modulo: sigea-frontend/tsconfig.spec.json

Describe una pieza de configuracion o soporte del proyecto.

### sigea-frontend/tsconfig.spec.json

Descripcion: Archivo de configuracion del tooling, build o entorno del frontend.

Dependencias:
- Consumido por el tooling del frontend, VS Code, Docker o Angular CLI.

Exports:
- json: tsconfig.spec.json

Notas:
- ⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.

Elementos internos:

#### Json: tsconfig.spec.json

Configuracion declarativa sin exports ejecutables.

## 9. API REST

Se agrega esta seccion al documento existente para cubrir el catalogo funcional de endpoints REST del backend Spring Boot.

### Dominio: Ambiente

### [GET] https://api.sigea.com/api/v1/ambientes
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/ambientes
- **Descripcion**: Crear ambiente sin foto (JSON). Disponible para ALIMENTADOR_EQUIPOS, ADMIN e INSTRUCTOR.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud max 100.",
  "ubicacion": "String — Ubicacion fisica o logica asociada al recurso. Validaciones: longitud max 200.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: longitud max 500.",
  "direccion": "String — Direccion o referencia de localizacion adicional. Validaciones: longitud max 250.",
  "padreId": "Long — Identificador del ambiente padre. Si se proporciona, esta ubicación se crea como sub-ubicación del ambiente padre indicado.",
  "idInstructorResponsable": "Long — Obligatorio para ADMIN; si es INSTRUCTOR puede ser null y se asigna él mismo."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/ambientes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "padreId": 1, "idInstructorResponsable": 1}'
```

### [GET] https://api.sigea.com/api/v1/ambientes/instructor/{instructorId}
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {instructorId} -> Long -> Identificador del instructor asociado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes/instructor/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/ambientes/mi-ambiente
- **Descripcion**: Ambientes que el usuario actual (instructor) administra. Solo aplica para INSTRUCTOR.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes/mi-ambiente \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/ambientes/todos
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes/todos \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/ambientes/{id}
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/ambientes/{id}
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud max 100.",
  "ubicacion": "String — Ubicacion fisica o logica asociada al recurso. Validaciones: longitud max 200.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: longitud max 500.",
  "direccion": "String — Direccion o referencia de localizacion adicional. Validaciones: longitud max 250.",
  "padreId": "Long — Identificador del ambiente padre. Si se proporciona, esta ubicación se crea como sub-ubicación del ambiente padre indicado.",
  "idInstructorResponsable": "Long — Obligatorio para ADMIN; si es INSTRUCTOR puede ser null y se asigna él mismo."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/ambientes/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "padreId": 1, "idInstructorResponsable": 1}'
```

### [PATCH] https://api.sigea.com/api/v1/ambientes/{id}/activar
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/ambientes/1/activar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/ambientes/{id}/desactivar
- **Descripcion**: Gestiona ubicaciones, ambientes y sububicaciones del inventario.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/ambientes/1/desactivar \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/ambientes/{padreId}/sub-ubicaciones
- **Descripcion**: Listar sub-ubicaciones de un ambiente padre.  /api/v1/ambientes/{padreId}/sub-ubicaciones.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {padreId} -> Long -> Identificador del ambiente o recurso padre. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/ambientes/1/sub-ubicaciones \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/ambientes/{padreId}/sub-ubicaciones
- **Descripcion**: Crear sub-ubicación de un ambiente padre (JSON).  /api/v1/ambientes/{padreId}/sub-ubicaciones.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')
- **Parametros de ruta**: {padreId} -> Long -> Identificador del ambiente o recurso padre. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud max 100.",
  "ubicacion": "String — Ubicacion fisica o logica asociada al recurso. Validaciones: longitud max 200.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: longitud max 500.",
  "direccion": "String — Direccion o referencia de localizacion adicional. Validaciones: longitud max 250.",
  "padreId": "Long — Identificador del ambiente padre. Si se proporciona, esta ubicación se crea como sub-ubicación del ambiente padre indicado.",
  "idInstructorResponsable": "Long — Obligatorio para ADMIN; si es INSTRUCTOR puede ser null y se asigna él mismo."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "instructorResponsableId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/ambientes/1/sub-ubicaciones \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "ubicacion": "texto", "descripcion": "texto", "direccion": "texto", "padreId": 1, "idInstructorResponsable": 1}'
```

### Dominio: Auditoria

### [GET] https://api.sigea.com/api/v1/auditoria
- **Descripcion**: Consulta trazabilidad y eventos de auditoria del sistema.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "accion": "texto", "entidadAfectada": "texto", "entidadId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/auditoria \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/auditoria/entidad/{entidad}/{entidadId}
- **Descripcion**: Consulta trazabilidad y eventos de auditoria del sistema.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {entidad} -> String -> Entidad del endpoint. -> requerido; {entidadId} -> Long -> Entidad id del endpoint. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "accion": "texto", "entidadAfectada": "texto", "entidadId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/auditoria/entidad/valor/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/auditoria/rango
- **Descripcion**: Consulta trazabilidad y eventos de auditoria del sistema.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?desde -> LocalDateTime -> Fecha/hora inicial del rango en formato ISO. -> requerido; ?hasta -> LocalDateTime -> Fecha/hora final del rango en formato ISO. -> requerido
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "accion": "texto", "entidadAfectada": "texto", "entidadId": 1}]` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/auditoria/rango?desde=2026-01-01T00:00:00&hasta=2026-01-01T00:00:00 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/auditoria/usuario/{usuarioId}
- **Descripcion**: Consulta trazabilidad y eventos de auditoria del sistema.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {usuarioId} -> Long -> Identificador del usuario objetivo. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "accion": "texto", "entidadAfectada": "texto", "entidadId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/auditoria/usuario/1 \
  -H "Authorization: Bearer <token>"
```

### Dominio: Categoria

### [GET] https://api.sigea.com/api/v1/categorias
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/categorias \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/categorias
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud min 2, max 100.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: Size ."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/categorias \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "descripcion": "texto"}'
```

### [GET] https://api.sigea.com/api/v1/categorias/todas
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/categorias/todas \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/categorias/{id}
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/categorias/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/categorias/{id}
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/categorias/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/categorias/{id}
- **Descripcion**: Administra el catalogo de categorias de equipos.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud min 2, max 100.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: Size ."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/categorias/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "descripcion": "texto"}'
```

### Dominio: Dashboard

### [GET] https://api.sigea.com/api/v1/dashboard/estadisticas
- **Descripcion**: Entrega estadisticas visibles en el tablero principal.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"totalEquipos": 1, "equiposActivos": 1, "totalCategorias": 1, "totalAmbientes": 1, "totalUsuarios": 1, "prestamosSolicitados": 1}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/dashboard/estadisticas \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/dashboard/grafico-equipos-por-categoria
- **Descripcion**: Entrega estadisticas visibles en el tablero principal.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"categoriaNombre": "texto", "cantidad": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/dashboard/grafico-equipos-por-categoria \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/dashboard/grafico-prestamos-por-mes
- **Descripcion**: Entrega estadisticas visibles en el tablero principal.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"mes": "texto", "cantidad": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/dashboard/grafico-prestamos-por-mes \
  -H "Authorization: Bearer <token>"
```

### Dominio: Equipo

### [GET] https://api.sigea.com/api/v1/equipos
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/equipos
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud max 200.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: longitud max 2000.",
  "placa": "String — Placa de registro SENA — máximo 20 dígitos numéricos. Único por equipo. Validaciones: obligatorio y no vacio, debe cumplir patron \\d{1,20}.",
  "serial": "String — Número de serie del fabricante — obligatorio. Validaciones: obligatorio y no vacio, longitud max 50.",
  "modelo": "String — Modelo del equipo — obligatorio. Validaciones: obligatorio y no vacio, longitud max 100.",
  "marcaId": "Long — ID de la marca (FK) — obligatorio. Validaciones: obligatorio.",
  "estadoEquipoEscala": "Integer — Estado del equipo en escala 1-10. Validaciones: valor minimo 1, valor maximo 10.",
  "codigoUnico": "String — Opcional: si viene vacío o null, el sistema genera uno automáticamente. Validaciones: longitud max 50.",
  "categoriaId": "Long — Identificador de la categoria. Validaciones: obligatorio.",
  "ambienteId": "Long — Identificador del ambiente consultado. Validaciones: obligatorio.",
  "subUbicacionId": "Long — Sub-ubicación dentro del ambiente (opcional).",
  "propietarioId": "Long — Solo para ALIMENTADOR_EQUIPOS: asigna el instructor dueño original del equipo.",
  "cantidadTotal": "Integer — Cantidad total registrada para el equipo. Validaciones: obligatorio, valor minimo 1.",
  "tipoUso": "TipoUsoEquipo — Indica si el equipo es consumible o no consumible. Validaciones: obligatorio.",
  "umbralMinimo": "Integer — Cantidad minima para alertar stock bajo. Validaciones: obligatorio, valor minimo 0."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/equipos \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto", "marcaId": 1, "estadoEquipoEscala": 1, "codigoUnico": "texto"}'
```

### [GET] https://api.sigea.com/api/v1/equipos/ambiente/{ambienteId}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {ambienteId} -> Long -> Identificador del ambiente consultado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/ambiente/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/categoria/{categoriaId}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {categoriaId} -> Long -> Identificador de la categoria. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/categoria/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/estado/{estado}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {estado} -> EstadoEquipo -> Estado usado como filtro o ruta enum. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/estado/ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/mi-inventario
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/mi-inventario \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/mis-equipos
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/mis-equipos \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/todos
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/todos \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/equipos/{id}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/equipos/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/equipos/{id}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/equipos/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/equipos/{id}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud max 200.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: longitud max 2000.",
  "placa": "String — Placa de registro SENA — máximo 20 dígitos numéricos. Único por equipo. Validaciones: obligatorio y no vacio, debe cumplir patron \\d{1,20}.",
  "serial": "String — Número de serie del fabricante — obligatorio. Validaciones: obligatorio y no vacio, longitud max 50.",
  "modelo": "String — Modelo del equipo — obligatorio. Validaciones: obligatorio y no vacio, longitud max 100.",
  "marcaId": "Long — ID de la marca (FK) — obligatorio. Validaciones: obligatorio.",
  "estadoEquipoEscala": "Integer — Estado del equipo en escala 1-10. Validaciones: valor minimo 1, valor maximo 10.",
  "codigoUnico": "String — Opcional: si viene vacío o null, el sistema genera uno automáticamente. Validaciones: longitud max 50.",
  "categoriaId": "Long — Identificador de la categoria. Validaciones: obligatorio.",
  "ambienteId": "Long — Identificador del ambiente consultado. Validaciones: obligatorio.",
  "subUbicacionId": "Long — Sub-ubicación dentro del ambiente (opcional).",
  "propietarioId": "Long — Solo para ALIMENTADOR_EQUIPOS: asigna el instructor dueño original del equipo.",
  "cantidadTotal": "Integer — Cantidad total registrada para el equipo. Validaciones: obligatorio, valor minimo 1.",
  "tipoUso": "TipoUsoEquipo — Indica si el equipo es consumible o no consumible. Validaciones: obligatorio.",
  "umbralMinimo": "Integer — Cantidad minima para alertar stock bajo. Validaciones: obligatorio, valor minimo 0."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/equipos/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto", "marcaId": 1, "estadoEquipoEscala": 1, "codigoUnico": "texto"}'
```

### [PATCH] https://api.sigea.com/api/v1/equipos/{id}/activar
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/equipos/1/activar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/equipos/{id}/dar-de-baja
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/equipos/1/dar-de-baja \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/equipos/{id}/estado/{nuevoEstado}
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido; {nuevoEstado} -> EstadoEquipo -> Nuevo estado del equipo o recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/equipos/1/estado/ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/equipos/{id}/recuperar
- **Descripcion**: Gestiona el inventario fisico de equipos y sus fotos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/equipos/1/recuperar \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/equipos/{id}/sub-ubicacion
- **Descripcion**: Quita la sub-ubicación asignada al equipo (lo deja solo en el ambiente principal).
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/equipos/1/sub-ubicacion \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/equipos/{id}/sub-ubicacion/{subUbicacionId}
- **Descripcion**: /api/v1/equipos/{id}/sub-ubicacion/{subUbicacionId} Asigna un equipo a una sub-ubicación dentro de su ambiente principal. Para eliminar la sub-ubicación, usar.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido; {subUbicacionId} -> Long -> Identificador de la sububicacion dentro del ambiente. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "placa": "texto", "serial": "texto", "modelo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/equipos/1/sub-ubicacion/1 \
  -H "Authorization: Bearer <token>"
```

### Dominio: Mantenimiento

### [GET] https://api.sigea.com/api/v1/mantenimientos
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?equipoId -> Long -> Identificador del equipo consultado. -> opcional; ?tipo -> TipoMantenimiento -> Tipo usado como filtro del dominio. -> opcional; ?enCurso -> Boolean -> En curso del endpoint. -> opcional
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}]` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/mantenimientos?equipoId=1&tipo=valor&enCurso=true \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/mantenimientos
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "equipoId": "Long — Identificador del equipo consultado. Validaciones: obligatorio.",
  "tipo": "TipoMantenimiento — Tipo usado como filtro del dominio. Validaciones: obligatorio.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: obligatorio y no vacio, longitud max 2000.",
  "fechaInicio": "LocalDate — Fecha inicio del endpoint. Validaciones: obligatorio.",
  "fechaFin": "LocalDate — Fecha fin del endpoint.",
  "responsable": "String — Responsable del endpoint. Validaciones: obligatorio y no vacio, longitud max 200.",
  "observaciones": "String — Observaciones del endpoint. Validaciones: longitud max 2000."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/mantenimientos \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"equipoId": 1, "tipo": "texto", "descripcion": "texto", "fechaInicio": "valor", "fechaFin": "valor", "responsable": "texto", "observaciones": "texto"}'
```

### [GET] https://api.sigea.com/api/v1/mantenimientos/en-curso
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/mantenimientos/en-curso \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/mantenimientos/equipo/{equipoId}
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {equipoId} -> Long -> Identificador del equipo consultado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/mantenimientos/equipo/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/mantenimientos/tipo/{tipo}
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {tipo} -> TipoMantenimiento -> Tipo usado como filtro del dominio. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/mantenimientos/tipo/PREVENTIVO \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/mantenimientos/{id}
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/mantenimientos/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/mantenimientos/{id}
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/mantenimientos/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/mantenimientos/{id}
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "equipoId": "Long — Identificador del equipo consultado. Validaciones: obligatorio.",
  "tipo": "TipoMantenimiento — Tipo usado como filtro del dominio. Validaciones: obligatorio.",
  "descripcion": "String — Descripcion funcional o detalle del recurso. Validaciones: obligatorio y no vacio, longitud max 2000.",
  "fechaInicio": "LocalDate — Fecha inicio del endpoint. Validaciones: obligatorio.",
  "fechaFin": "LocalDate — Fecha fin del endpoint.",
  "responsable": "String — Responsable del endpoint. Validaciones: obligatorio y no vacio, longitud max 200.",
  "observaciones": "String — Observaciones del endpoint. Validaciones: longitud max 2000."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/mantenimientos/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"equipoId": 1, "tipo": "texto", "descripcion": "texto", "fechaInicio": "valor", "fechaFin": "valor", "responsable": "texto", "observaciones": "texto"}'
```

### [PATCH] https://api.sigea.com/api/v1/mantenimientos/{id}/cerrar
- **Descripcion**: Administra mantenimientos preventivos y correctivos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "fechaFin": "LocalDate — Fecha fin del endpoint. Validaciones: obligatorio.",
  "observaciones": "String — Observaciones del endpoint. Validaciones: longitud max 2000."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "tipo": "texto", "descripcion": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/mantenimientos/1/cerrar \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fechaFin": "valor", "observaciones": "texto"}'
```

### Dominio: Marca

### [GET] https://api.sigea.com/api/v1/marcas
- **Descripcion**: /api/v1/marcas — Crear marca (roles operativos) */ @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')") public ResponseEntity<MarcaRespuestaDTO> crear(@Valid @RequestBody MarcaCrearDTO dto) { return new ResponseEntity<>(marcaServicio.crear(dto), HttpStatus.CREATED); } /**  /api/v1/marcas — Listar marcas activas (todos los autenticados).
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/marcas \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/marcas/{id}
- **Descripcion**: /api/v1/marcas/todas — Listar todas (roles operativos) */ @GetMapping("/todas") @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')") public ResponseEntity<List<MarcaRespuestaDTO>> listarTodas() { return ResponseEntity.ok(marcaServicio.listarTodas()); } /**  /api/v1/marcas/{id}.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/marcas/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/marcas/{id}/activar
- **Descripcion**: /api/v1/marcas/{id} — Editar (roles operativos) */ @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')") public ResponseEntity<MarcaRespuestaDTO> actualizar( @PathVariable Long id, @Valid @RequestBody MarcaCrearDTO dto) { return ResponseEntity.ok(marcaServicio.actualizar(id, dto)); } /**  /api/v1/marcas/{id}/activar.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR','ALIMENTADOR_EQUIPOS')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombre": "texto", "descripcion": "texto", "activo": true, "fechaCreacion": "2026-01-01T08:00:00", "fechaActualizacion": "2026-01-01T08:00:00"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/marcas/1/activar \
  -H "Authorization: Bearer <token>"
```

### Dominio: Notificacion

### [GET] https://api.sigea.com/api/v1/notificaciones/mis-notificaciones
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioDestinoId": 1, "nombreUsuarioDestino": "texto", "tipoNotificacion": "texto", "mensaje": "texto", "titulo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/notificaciones/mis-notificaciones \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/notificaciones/mis-notificaciones/contador
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"mensaje": "operacion completada"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/notificaciones/mis-notificaciones/contador \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/notificaciones/mis-notificaciones/no-leidas
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioDestinoId": 1, "nombreUsuarioDestino": "texto", "tipoNotificacion": "texto", "mensaje": "texto", "titulo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/notificaciones/mis-notificaciones/no-leidas \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/notificaciones/probar-correo
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "destinatario": "String — Destinatario del endpoint. Validaciones: debe ser correo valido.",
  "suiteCompleta": "Boolean — Indica si se envia la suite completa de correos de prueba."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"mensaje": "operacion completada"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/notificaciones/probar-correo \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"destinatario": "texto", "suiteCompleta": true}'
```

### [GET] https://api.sigea.com/api/v1/notificaciones/usuario/{usuarioId}
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {usuarioId} -> Long -> Identificador del usuario objetivo. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioDestinoId": 1, "nombreUsuarioDestino": "texto", "tipoNotificacion": "texto", "mensaje": "texto", "titulo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/notificaciones/usuario/1 \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/notificaciones/{id}/marcar-leida
- **Descripcion**: Consulta o actualiza notificaciones del usuario.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/notificaciones/1/marcar-leida \
  -H "Authorization: Bearer <token>"
```

### Dominio: Observacion

### [GET] https://api.sigea.com/api/v1/observaciones-equipo/equipo/{equipoId}
- **Descripcion**: Registra una observación al devolver un equipo */ @PostMapping @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR') or hasRole('FUNCIONARIO')") public ResponseEntity<ObservacionEquipoRespuestaDTO> registrar( @Valid @RequestBody ObservacionEquipoCrearDTO dto, @AuthenticationPrincipal UserDetails userDetails) { return ResponseEntity.status(HttpStatus.CREATED) .body(servicio.registrar(dto, userDetails.getUsername())); } /** Lista todas las observaciones de un equipo específico.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {equipoId} -> Long -> Identificador del equipo consultado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "prestamoId": 1, "equipoId": 1, "equipoNombre": "texto", "equipoPlaca": "texto", "usuarioDuenioId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/observaciones-equipo/equipo/1 \
  -H "Authorization: Bearer <token>"
```

### Dominio: Prestamo

### [GET] https://api.sigea.com/api/v1/prestamos
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/prestamos
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "fechaHoraDevolucionEstimada": "LocalDateTime — Fecha hora devolucion estimada del endpoint. Validaciones: obligatorio.",
  "observacionesGenerales": "String — Observaciones generales del endpoint. Validaciones: longitud max 500.",
  "detalles": "List<DetallePrestamoDTO> — Detalles del endpoint."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/prestamos \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fechaHoraDevolucionEstimada": "2026-01-01T08:00:00", "observacionesGenerales": "texto", "detalles": []}'
```

### [GET] https://api.sigea.com/api/v1/prestamos/estado/{estado}
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {estado} -> EstadoPrestamo -> Estado usado como filtro o ruta enum. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos/estado/ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos/mis-prestamos
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos/mis-prestamos \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos/usuario/{usuarioId}
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {usuarioId} -> Long -> Identificador del usuario objetivo. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos/usuario/1 \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/prestamos/{id}
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/prestamos/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos/{id}
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos/1 \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/prestamos/{id}/aprobar
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/prestamos/1/aprobar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/prestamos/{id}/rechazar
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/prestamos/1/rechazar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/prestamos/{id}/registrar-devolucion
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "detalles": "List<PrestamoDevolucionDetalleDTO> — Detalles del endpoint."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/prestamos/1/registrar-devolucion \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"detalles": []}'
```

### [PATCH] https://api.sigea.com/api/v1/prestamos/{id}/registrar-salida
- **Descripcion**: Gestiona solicitudes, aprobaciones y devoluciones de prestamos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioSolicitanteId": 1, "nombreUsuarioSolicitante": "texto", "correoUsuarioSolicitante": "texto", "nombreAdministradorAprueba": "texto", "nombreAdministradorRecibe": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/prestamos/1/registrar-salida \
  -H "Authorization: Bearer <token>"
```

### Dominio: Prestamoambiente

### [GET] https://api.sigea.com/api/v1/prestamos-ambientes
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos-ambientes \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/prestamos-ambientes
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "ambienteId": "Long — Identificador del ambiente consultado. Validaciones: obligatorio.",
  "fechaInicio": "LocalDate — Fecha inicio del endpoint. Validaciones: obligatorio.",
  "fechaFin": "LocalDate — Fecha fin del endpoint. Validaciones: obligatorio.",
  "horaInicio": "LocalTime — Hora inicio del endpoint. Validaciones: obligatorio.",
  "horaFin": "LocalTime — Hora fin del endpoint. Validaciones: obligatorio.",
  "proposito": "String — Proposito del endpoint. Validaciones: obligatorio y no vacio.",
  "numeroParticipantes": "Integer — Numero participantes del endpoint. Validaciones: obligatorio, valor minimo 1.",
  "tipoActividad": "TipoActividad — Tipo actividad del endpoint. Validaciones: obligatorio.",
  "observacionesSolicitud": "String — Observaciones solicitud del endpoint."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/prestamos-ambientes \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"ambienteId": 1, "fechaInicio": "valor", "fechaFin": "valor", "horaInicio": "valor", "horaFin": "valor", "proposito": "texto", "numeroParticipantes": 1, "tipoActividad": "texto"}'
```

### [GET] https://api.sigea.com/api/v1/prestamos-ambientes/ambiente/{ambienteId}
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {ambienteId} -> Long -> Identificador del ambiente consultado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos-ambientes/ambiente/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos-ambientes/estado/{estado}
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR')
- **Parametros de ruta**: {estado} -> EstadoPrestamoAmbiente -> Estado usado como filtro o ruta enum. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos-ambientes/estado/ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos-ambientes/mis-solicitudes
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos-ambientes/mis-solicitudes \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/prestamos-ambientes/{id}
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/prestamos-ambientes/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/prestamos-ambientes/{id}/aprobar
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/prestamos-ambientes/1/aprobar \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/prestamos-ambientes/{id}/cancelar
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/prestamos-ambientes/1/cancelar \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/prestamos-ambientes/{id}/devolver
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "observacionesDevolucion": "String — Observaciones devolucion del endpoint. Validaciones: obligatorio y no vacio.",
  "estadoDevolucionAmbiente": "Integer — Estado devolucion ambiente del endpoint. Validaciones: obligatorio, valor minimo 1, valor maximo 10."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/prestamos-ambientes/1/devolver \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"observacionesDevolucion": "texto", "estadoDevolucionAmbiente": 1}'
```

### [PUT] https://api.sigea.com/api/v1/prestamos-ambientes/{id}/rechazar
- **Descripcion**: Gestiona solicitudes y devoluciones de ambientes.
- **Roles permitidos**: hasRole('ADMINISTRADOR') or hasRole('INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ?motivo -> String -> Motivo textual de rechazo, cancelacion o cambio. -> requerido
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "ambienteId": 1, "ambienteNombre": "texto", "solicitanteId": 1, "solicitanteNombre": "texto", "propietarioAmbienteId": 1}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/prestamos-ambientes/1/rechazar?motivo=valor \
  -H "Authorization: Bearer <token>"
```

### Dominio: Reporte

### [GET] https://api.sigea.com/api/v1/reportes/equipos-mas-solicitados
- **Descripcion**: RF-REP-03 + RF-REP-05/06: Equipos más solicitados (ranking).  /reportes/equipos-mas-solicitados?formato=xlsx|pdf.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?xlsx -> String -> Xlsx del endpoint. -> opcional
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"file": "binario descargable"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reportes/equipos-mas-solicitados?xlsx=valor \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reportes/inventario
- **Descripcion**: RF-REP-01 + RF-REP-05/06: Reporte de inventario con filtros opcionales.  /reportes/inventario?formato=xlsx|pdf&inventarioInstructorId=&categoriaId=&estado=.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?xlsx -> String -> Xlsx del endpoint. -> opcional; ?inventarioInstructorId -> Long -> Inventario instructor id del endpoint. -> opcional; ?categoriaId -> Long -> Identificador de la categoria. -> opcional; ?estado -> EstadoEquipo -> Estado usado como filtro o ruta enum. -> opcional
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"file": "binario descargable"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reportes/inventario?xlsx=valor&inventarioInstructorId=1&categoriaId=1&estado=ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reportes/prestamos
- **Descripcion**: RF-REP-02 + RF-REP-05/06: Historial de préstamos con filtros opcionales.  /reportes/prestamos?formato=xlsx|pdf&usuarioId=&equipoId=&desde=&hasta=&estado= desde/hasta: ISO (2026-01-01T00:00:00) o solo fecha (2026-01-01).
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?xlsx -> String -> Xlsx del endpoint. -> opcional; ?usuarioId -> Long -> Identificador del usuario objetivo. -> opcional; ?equipoId -> Long -> Identificador del equipo consultado. -> opcional; ?desde -> String -> Fecha/hora inicial del rango en formato ISO. -> opcional; ?hasta -> String -> Fecha/hora final del rango en formato ISO. -> opcional; ?estado -> EstadoPrestamo -> Estado usado como filtro o ruta enum. -> opcional
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"file": "binario descargable"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reportes/prestamos?xlsx=valor&usuarioId=1&equipoId=1&desde=2026-01-01T00:00:00&hasta=2026-01-01T00:00:00&estado=ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reportes/usuarios-en-mora
- **Descripcion**: RF-REP-04 + RF-REP-05/06: Usuarios con préstamos pendientes o vencidos.  /reportes/usuarios-en-mora?formato=xlsx|pdf.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ?xlsx -> String -> Xlsx del endpoint. -> opcional
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"file": "binario descargable"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reportes/usuarios-en-mora?xlsx=valor \
  -H "Authorization: Bearer <token>"
```

### Dominio: Reserva

### [GET] https://api.sigea.com/api/v1/reservas
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reservas \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/reservas
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "equipoId": "Long — Identificador del equipo consultado. Validaciones: obligatorio.",
  "cantidad": "Integer — Cantidad del endpoint. Validaciones: obligatorio, valor minimo 1.",
  "fechaHoraInicio": "LocalDateTime — Fecha hora inicio del endpoint. Validaciones: obligatorio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/reservas \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"equipoId": 1, "cantidad": 1, "fechaHoraInicio": "2026-01-01T08:00:00"}'
```

### [GET] https://api.sigea.com/api/v1/reservas/estado/{estado}
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {estado} -> EstadoReserva -> Estado usado como filtro o ruta enum. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reservas/estado/ACTIVO \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reservas/mis-reservas
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reservas/mis-reservas \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reservas/usuario/{usuarioId}
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {usuarioId} -> Long -> Identificador del usuario objetivo. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reservas/usuario/1 \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/reservas/{id}
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/reservas/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/reservas/{id}
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/reservas/1 \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/reservas/{id}/cancelar
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/reservas/1/cancelar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/reservas/{id}/equipo-recogido
- **Descripcion**: Administra reservas anticipadas de equipos.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "fechaHoraDevolucion": "LocalDateTime — DTO para marcar una reserva como "equipo recogido" y registrar la hora de devolución. Validaciones: obligatorio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "usuarioId": 1, "nombreUsuario": "texto", "correoUsuario": "texto", "equipoId": 1, "nombreEquipo": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/reservas/1/equipo-recogido \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"fechaHoraDevolucion": "2026-01-01T08:00:00"}'
```

### Dominio: Seguridad

### [POST] https://api.sigea.com/api/v1/auth/login
- **Descripcion**: Gestiona autenticacion, registro y recuperacion de acceso.
- **Roles permitidos**: Publico, no requiere JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "numeroDocumento": "String — Numero de documento de la persona usuaria. Validaciones: obligatorio y no vacio.",
  "contrasena": "String — Contrasena en texto plano antes de ser cifrada. Validaciones: obligatorio y no vacio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "token": "texto", "tipo": "texto", "nombreCompleto": "texto", "rol": "texto", "correoElectronico": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"numeroDocumento": "texto", "contrasena": "texto"}'
```

### [POST] https://api.sigea.com/api/v1/auth/recuperar-contrasena
- **Descripcion**: Gestiona autenticacion, registro y recuperacion de acceso.
- **Roles permitidos**: Publico, no requiere JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "correo": "String — Correo del endpoint. Validaciones: obligatorio y no vacio, debe ser correo valido."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"mensaje": "operacion completada"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/auth/recuperar-contrasena \
  -H "Content-Type: application/json" \
  -d '{"correo": "texto"}'
```

### [POST] https://api.sigea.com/api/v1/auth/registro
- **Descripcion**: Gestiona autenticacion, registro y recuperacion de acceso.
- **Roles permitidos**: Publico, no requiere JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombre": "String — Nombre visible del recurso. Validaciones: obligatorio y no vacio, longitud min 2, max 150.",
  "tipoDocumento": "TipoDocumento — Tipo de documento permitido por el sistema. Validaciones: obligatorio.",
  "numeroDocumento": "String — Numero de documento de la persona usuaria. Validaciones: obligatorio y no vacio, longitud min 5, max 20.",
  "correoElectronico": "String — Correo electronico del usuario. Validaciones: debe ser correo valido.",
  "programaFormacion": "String — Programa de formacion asociado al usuario. Validaciones: longitud max 200.",
  "telefono": "String — Telefono de contacto. Validaciones: longitud max 20.",
  "numeroFicha": "String — Numero de ficha academica o grupo. Validaciones: longitud max 10.",
  "contrasena": "String — Contrasena en texto plano antes de ser cifrada. Validaciones: obligatorio y no vacio, longitud min 8, max 100, debe cumplir patron ^(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"message": "operacion exitosa"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "programaFormacion": "texto", "telefono": "texto", "numeroFicha": "texto", "contrasena": "texto"}'
```

### [POST] https://api.sigea.com/api/v1/auth/restablecer-contrasena
- **Descripcion**: Gestiona autenticacion, registro y recuperacion de acceso.
- **Roles permitidos**: Publico, no requiere JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "correo": "String — Correo del endpoint. Validaciones: obligatorio y no vacio, debe ser correo valido.",
  "codigo": "String — Codigo de verificacion o recuperacion. Validaciones: obligatorio y no vacio, longitud min 6, max 6.",
  "nuevaContrasena": "String — Nueva contrasena que reemplazara la actual. Validaciones: obligatorio y no vacio, longitud min 8."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"mensaje": "operacion completada"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/auth/restablecer-contrasena \
  -H "Content-Type: application/json" \
  -d '{"correo": "texto", "codigo": "texto", "nuevaContrasena": "texto"}'
```

### [POST] https://api.sigea.com/api/v1/auth/verificar-email
- **Descripcion**: Verificación de email por código de 6 dígitos enviado al correo.  /api/v1/auth/verificar-email con body { "correo": "...", "codigo": "123456" }.
- **Roles permitidos**: Publico, no requiere JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "correo": "String — DTO para verificar el correo con el código de 6 dígitos enviado por email. Validaciones: obligatorio y no vacio.",
  "codigo": "String — Codigo de verificacion o recuperacion. Validaciones: obligatorio y no vacio, longitud min 6, max 6."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"mensaje": "operacion completada"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
  | 503 | Falla un servicio externo de correo o WhatsApp requerido por la operacion. | `{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/auth/verificar-email \
  -H "Content-Type: application/json" \
  -d '{"correo": "texto", "codigo": "texto"}'
```

### Dominio: Transferencia

### [GET] https://api.sigea.com/api/v1/transferencias
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/transferencias \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/transferencias
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: hasAnyRole('ADMINISTRADOR','INSTRUCTOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "equipoId": "Long — Identificador del equipo consultado. Validaciones: obligatorio.",
  "instructorDestinoId": "Long — Instructor destino id del endpoint. Validaciones: obligatorio.",
  "ubicacionDestinoId": "Long — Ubicacion destino id del endpoint.",
  "motivo": "String — Motivo textual de rechazo, cancelacion o cambio.",
  "fechaTransferencia": "LocalDateTime — Fecha transferencia del endpoint. Validaciones: obligatorio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/transferencias \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"equipoId": 1, "instructorDestinoId": 1, "ubicacionDestinoId": 1, "motivo": "texto", "fechaTransferencia": "2026-01-01T08:00:00"}'
```

### [GET] https://api.sigea.com/api/v1/transferencias/equipo/{equipoId}
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {equipoId} -> Long -> Identificador del equipo consultado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/transferencias/equipo/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/transferencias/inventario-destino/{instructorId}
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {instructorId} -> Long -> Identificador del instructor asociado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/transferencias/inventario-destino/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/transferencias/inventario-origen/{instructorId}
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {instructorId} -> Long -> Identificador del instructor asociado. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/transferencias/inventario-origen/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/transferencias/{id}
- **Descripcion**: Registra movimientos de equipos entre responsables o inventarios.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "equipoId": 1, "nombreEquipo": "texto", "codigoEquipo": "texto", "inventarioOrigenInstructorId": 1, "nombreInventarioOrigenInstructor": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/transferencias/1 \
  -H "Authorization: Bearer <token>"
```

### Dominio: Usuario

### [GET] https://api.sigea.com/api/v1/usuarios
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios \
  -H "Authorization: Bearer <token>"
```

### [POST] https://api.sigea.com/api/v1/usuarios
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombreCompleto": "String — Nombre completo de la persona. Validaciones: obligatorio y no vacio, Size .",
  "tipoDocumento": "TipoDocumento — Tipo de documento permitido por el sistema. Validaciones: obligatorio.",
  "numeroDocumento": "String — Numero de documento de la persona usuaria. Validaciones: obligatorio y no vacio, longitud min 5, max 10.",
  "correoElectronico": "String — Correo electronico del usuario. Validaciones: debe ser correo valido.",
  "programaFormacion": "String — Programa de formacion asociado al usuario. Validaciones: longitud max 200.",
  "numeroFicha": "String — Numero de ficha academica o grupo. Validaciones: longitud max 10.",
  "telefono": "String — Telefono de contacto. Validaciones: longitud max 20.",
  "contrasena": "String — Contrasena en texto plano antes de ser cifrada. Validaciones: obligatorio y no vacio, longitud min 8, max 100.",
  "rol": "Rol — Rol a consultar o asignar. Validaciones: obligatorio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 201 | El recurso se crea correctamente. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X POST https://api.sigea.com/api/v1/usuarios \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "programaFormacion": "texto", "numeroFicha": "texto", "telefono": "texto", "contrasena": "texto"}'
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/cambiar-contrasena
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "contrasenaActual": "String — Contrasena vigente del usuario autenticado. Validaciones: obligatorio y no vacio.",
  "nuevaContrasena": "String — Nueva contrasena que reemplazara la actual. Validaciones: obligatorio y no vacio, longitud min 8."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/cambiar-contrasena \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"contrasenaActual": "texto", "nuevaContrasena": "texto"}'
```

### [GET] https://api.sigea.com/api/v1/usuarios/pendientes
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios/pendientes \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/usuarios/perfil
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: Cualquier usuario autenticado con JWT
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios/perfil \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/usuarios/rol/{rol}
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {rol} -> Rol -> Rol a consultar o asignar. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios/rol/ADMINISTRADOR \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/usuarios/todos
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: ninguno
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `[{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}]` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios/todos \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/usuarios/{id}
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/usuarios/1 \
  -H "Authorization: Bearer <token>"
```

### [GET] https://api.sigea.com/api/v1/usuarios/{id}
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: isAuthenticated()
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
- **Ejemplo curl**:
```bash
curl -X GET https://api.sigea.com/api/v1/usuarios/1 \
  -H "Authorization: Bearer <token>"
```

### [PUT] https://api.sigea.com/api/v1/usuarios/{id}
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nombreCompleto": "String — Nombre completo de la persona. Validaciones: obligatorio y no vacio.",
  "tipoDocumento": "TipoDocumento — Tipo de documento permitido por el sistema. Validaciones: obligatorio.",
  "numeroDocumento": "String — Numero de documento de la persona usuaria. Validaciones: obligatorio y no vacio.",
  "correoElectronico": "String — Correo electronico del usuario. Validaciones: debe ser correo valido.",
  "numeroTelefono": "String — Numero telefono del endpoint. Validaciones: longitud max 10.",
  "programaFormacion": "String — Programa de formacion asociado al usuario. Validaciones: longitud max 200.",
  "numeroFicha": "String — Numero de ficha academica o grupo. Validaciones: longitud max 10."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PUT https://api.sigea.com/api/v1/usuarios/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "numeroTelefono": "texto", "programaFormacion": "texto", "numeroFicha": "texto"}'
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/activar
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/activar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/aprobar
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/aprobar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/desactivar
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/desactivar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/desbloquear
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/desbloquear \
  -H "Authorization: Bearer <token>"
```

### [DELETE] https://api.sigea.com/api/v1/usuarios/{id}/rechazar
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica): no aplica
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X DELETE https://api.sigea.com/api/v1/usuarios/1/rechazar \
  -H "Authorization: Bearer <token>"
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/restablecer-contrasena
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nuevaContrasena": "String — Nueva contrasena que reemplazara la actual. Validaciones: obligatorio y no vacio, longitud min 8."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 204 | La operacion se ejecuta correctamente sin body de respuesta. | `sin contenido` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/restablecer-contrasena \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nuevaContrasena": "texto"}'
```

### [PATCH] https://api.sigea.com/api/v1/usuarios/{id}/rol
- **Descripcion**: Administra usuarios, roles, aprobaciones y credenciales.
- **Roles permitidos**: hasRole('ADMINISTRADOR')
- **Parametros de ruta**: {id} -> Long -> Identificador unico del recurso. -> requerido
- **Parametros de query**: ninguno
- **Request body** (si aplica):
```json
{
  "nuevoRol": "Rol — Nuevo rol del endpoint. Validaciones: obligatorio."
}
```
- **Respuestas posibles**:
  | Código | Cuándo ocurre | Ejemplo de body |
  |--------|--------------|-----------------|
  | 200 | La operacion se completa correctamente y devuelve datos. | `{"id": 1, "nombreCompleto": "texto", "tipoDocumento": "texto", "numeroDocumento": "texto", "correoElectronico": "texto", "telefono": "texto"}` |
  | 400 | El body, los enums o los parametros no cumplen validaciones o formato esperado. | `{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}` |
  | 403 | El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio. | `{"error": "Operacion No Permitida", "message": "Acceso denegado"}` |
  | 404 | El recurso consultado o relacionado no existe. | `{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}` |
  | 409 | Ya existe un registro incompatible o hay conflicto de estado/duplicidad. | `{"error": "Recurso Duplicado", "message": "Conflicto de datos"}` |
- **Ejemplo curl**:
```bash
curl -X PATCH https://api.sigea.com/api/v1/usuarios/1/rol \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nuevoRol": "texto"}'
```
## 10. Guia de Setup para Desarrollo

### 1. PRERREQUISITOS

Herramientas minimas para correr el proyecto de forma estable:

| Herramienta | Version minima | Verificar en Windows PowerShell | Verificar en Unix bash | Nota |
|-------------|----------------|----------------------------------|------------------------|------|
| Git | 2.40+ | `git --version` | `git --version` | Necesario para clonar el repo. |
| Java | 21 | `java --version` | `java --version` | Requerido por Spring Boot y el Dockerfile del backend. |
| Node.js | 18.19+ | `node --version` | `node --version` | Angular 18 funciona bien con Node 18+, Dockerfile usa Node 20. |
| npm | 9+ | `npm --version` | `npm --version` | Necesario para instalar dependencias del frontend. |
| MariaDB Server | 11+ | `mariadb --version` | `mariadb --version` | Necesario solo para setup sin Docker. |
| Docker Engine | 24+ | `docker --version` | `docker --version` | Opcional; requerido para el setup con Docker Compose. |
| Docker Compose | v2.20+ | `docker compose version` | `docker compose version` | Opcional; requerido para levantar todo con un comando. |

Notas rapidas:

- Maven no es obligatorio porque el repositorio ya incluye `mvnw` y `mvnw.cmd`.
- En local el frontend espera el backend en `http://localhost:8082` porque [sigea-frontend/proxy.conf.json](sigea-frontend/proxy.conf.json) apunta a ese puerto.

### 2. VARIABLES DE ENTORNO

Variables y propiedades relevantes para desarrollo y despliegue:

| Variable | Descripción | Ejemplo | Requerida |
|----------|-------------|---------|-----------|
| server.port | Puerto del backend en local | `8082` | Sí |
| sigea.app.url | URL pública del frontend usada por CORS y enlaces | `http://localhost:4200` | Sí |
| sigea.auth.require-email-verification | Activa verificación por correo en registro | `false` | Sí |
| spring.datasource.url | URL JDBC local de MariaDB | `jdbc:mariadb://localhost:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota` | Sí |
| spring.datasource.username | Usuario de la base de datos local | `sigea_user` | Sí |
| spring.datasource.password | Clave de la base de datos local | `sigea_password_2026` | Sí |
| sigea.jwt.secret | Secreto JWT de la aplicación | `sigea-clave-secreta-desarrollo-cambiar-en-produccion-2026-sena-cimi` | Sí |
| spring.mail.host | Host SMTP | `smtp.gmail.com` | No |
| spring.mail.port | Puerto SMTP | `587` | No |
| spring.mail.username | Usuario SMTP | `sigeasena@gmail.com` | No |
| spring.mail.password | Contraseña SMTP | `⚠️ CONFIGURAR` | No |
| sigea.mail.from-name | Nombre remitente del correo | `SIGEA SENA` | No |
| sigea.whatsapp.enabled | Activa integración Twilio/WhatsApp | `false` | No |
| sigea.whatsapp.twilio.account-sid | SID de Twilio | `⚠️ CONFIGURAR` | No |
| sigea.whatsapp.twilio.auth-token | Token de Twilio | `⚠️ CONFIGURAR` | No |
| sigea.uploads.path | Ruta local para archivos subidos | `./uploads` | Sí |
| spring.datasource.hikari.maximum-pool-size | Límite del pool de conexiones | `20` | No |
| SIGEA_DB_URL | URL JDBC usada en Docker/producción | `jdbc:mariadb://db:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota` | No |
| SIGEA_DB_USERNAME | Usuario DB para Docker/producción | `sigea_user` | No |
| SIGEA_DB_PASSWORD | Clave DB para Docker/producción | `sigea_password_2026` | No |
| SIGEA_DB_ROOT_PASSWORD | Clave root de MariaDB para Docker | `root_sigea_2026` | No |

Archivo `.env.example` listo para copiar:

```dotenv
# =============================================================
# SIGEA - ejemplo de configuracion local
# Copiar a .env en la raiz del proyecto para desarrollo sin Docker.
# Este archivo es importado por Spring Boot como properties.
# =============================================================

# Backend local
server.port=8082
sigea.app.url=http://localhost:4200
sigea.auth.require-email-verification=false

# Base de datos local (sin Docker)
spring.datasource.url=jdbc:mariadb://localhost:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
spring.datasource.username=sigea_user
spring.datasource.password=sigea_password_2026
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

# Flyway local
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT local
sigea.jwt.secret=sigea-clave-secreta-desarrollo-cambiar-en-produccion-2026-sena-cimi
sigea.jwt.expiration-ms=28800000

# Uploads local
sigea.uploads.path=./uploads
sigea.uploads.max-file-size=5MB
sigea.uploads.allowed-extensions=jpg,jpeg,png

# SMTP local - ⚠️ CONFIGURAR si quieres registro con correo real
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
sigea.mail.from-name=SIGEA SENA

# WhatsApp / Twilio - opcional
sigea.whatsapp.enabled=false
sigea.whatsapp.default-country-code=+57
sigea.whatsapp.twilio.account-sid=
sigea.whatsapp.twilio.auth-token=
sigea.whatsapp.twilio.from=whatsapp:+14155238886

# Pool de conexiones local
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Variables de referencia para despliegue / Docker Compose
SIGEA_DB_URL=jdbc:mariadb://db:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
SIGEA_DB_USERNAME=sigea_user
SIGEA_DB_PASSWORD=sigea_password_2026
SIGEA_DB_ROOT_PASSWORD=root_sigea_2026
SIGEA_FRONTEND_PORT=8083
SIGEA_BACKEND_PORT=8082
SIGEA_DB_PORT=3305
SIGEA_JWT_SECRET=sigea-clave-secreta-desarrollo-cambiar-en-produccion-2026-sena-cimi
```

⚠️ Aviso de seguridad obligatorio:

- Los valores de `.env.example` son solo de referencia academica/desarrollo.
- Antes de cualquier despliegue real, cambiar credenciales de base de datos, secretos JWT y claves SMTP.
- Nunca publicar secretos reales en repositorio, PDF o canales compartidos.

### 3. SETUP LOCAL SIN DOCKER

1. Clonar el repositorio.

Windows PowerShell:

```powershell
git clone ⚠️CONFIGURAR_REPO_URL "$HOME\dev\SIGEA_SENA"
Set-Location "$HOME\dev\SIGEA_SENA"
Copy-Item .env.example .env
```

Unix bash:

```bash
git clone ⚠️CONFIGURAR_REPO_URL "$HOME/dev/SIGEA_SENA"
cd "$HOME/dev/SIGEA_SENA"
cp .env.example .env
```

2. Crear base de datos local y usuario MariaDB.

Windows PowerShell:

```powershell
mariadb -u root -p -e "CREATE DATABASE IF NOT EXISTS sigea_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'sigea_user'@'localhost' IDENTIFIED BY 'sigea_password_2026'; GRANT ALL PRIVILEGES ON sigea_db.* TO 'sigea_user'@'localhost'; FLUSH PRIVILEGES;"
```

Unix bash:

```bash
mariadb -u root -p -e "CREATE DATABASE IF NOT EXISTS sigea_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS 'sigea_user'@'localhost' IDENTIFIED BY 'sigea_password_2026'; GRANT ALL PRIVILEGES ON sigea_db.* TO 'sigea_user'@'localhost'; FLUSH PRIVILEGES;"
```

3. Ejecutar migraciones Flyway de forma explícita.

Windows PowerShell:

```powershell
Set-Location .\sigea-backend
.\mvnw.cmd flyway:migrate "-Dflyway.url=jdbc:mariadb://localhost:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8" "-Dflyway.user=sigea_user" "-Dflyway.password=sigea_password_2026"
Set-Location ..
```

Unix bash:

```bash
cd sigea-backend
./mvnw flyway:migrate -Dflyway.url=jdbc:mariadb://localhost:3306/sigea_db?useUnicode=true\&characterEncoding=UTF-8 -Dflyway.user=sigea_user -Dflyway.password=sigea_password_2026
cd ..
```

4. Levantar backend.

Windows PowerShell:

```powershell
Set-Location .\sigea-backend
.\mvnw.cmd spring-boot:run
```

Unix bash:

```bash
cd sigea-backend
./mvnw spring-boot:run
```

5. Levantar frontend en otra terminal.

Windows PowerShell:

```powershell
Set-Location .\sigea-frontend
npm install
npm start
```

Unix bash:

```bash
cd sigea-frontend
npm install
npm start
```

### 4. SETUP CON DOCKER COMPOSE

Archivo `docker-compose.yml` agregado para levantar base de datos, backend y frontend en local:

```yaml
services:
  db:
    image: mariadb:11.4
    container_name: sigea-db
    restart: unless-stopped
    environment:
      MARIADB_DATABASE: sigea_db
      MARIADB_USER: sigea_user
      MARIADB_PASSWORD: sigea_password_2026
      MARIADB_ROOT_PASSWORD: root_sigea_2026
    ports:
      - "3305:3306"
    volumes:
      - sigea-db-data:/var/lib/mysql
    healthcheck:
      test: ["CMD-SHELL", "mariadb-admin ping -h localhost -uroot -proot_sigea_2026"]
      interval: 10s
      timeout: 5s
      retries: 10

  backend:
    build:
      context: ./sigea-backend
    container_name: sigea-backend
    restart: unless-stopped
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_DATASOURCE_URL: jdbc:mariadb://db:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
      SPRING_DATASOURCE_USERNAME: sigea_user
      SPRING_DATASOURCE_PASSWORD: sigea_password_2026
      SERVER_PORT: 8080
      SIGEA_APP_URL: http://localhost:8083
      SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION: "false"
      SIGEA_SMTP_HOST: smtp.gmail.com
      SIGEA_SMTP_PORT: "587"
      SIGEA_SMTP_USERNAME: ""
      SIGEA_SMTP_PASSWORD: ""
      SIGEA_SMTP_AUTH: "true"
      SIGEA_SMTP_STARTTLS: "true"
      SIGEA_SMTP_STARTTLS_REQUIRED: "true"
      SIGEA_SMTP_SSL_TRUST: smtp.gmail.com
    ports:
      - "8082:8080"
    volumes:
      - sigea-uploads:/app/uploads

  frontend:
    build:
      context: ./sigea-frontend
    container_name: sigea-frontend
    restart: unless-stopped
    depends_on:
      - backend
    ports:
      - "8083:80"
    volumes:
      - ./docker/nginx.local.conf:/etc/nginx/nginx.conf:ro

volumes:
  sigea-db-data:
  sigea-uploads:
```

Archivo `docker/nginx.local.conf` usado por el frontend en contenedor:

```nginx
worker_processes auto;

events {
  worker_connections 1024;
}

http {
  include       /etc/nginx/mime.types;
  default_type  application/octet-stream;
  sendfile      on;
  keepalive_timeout 65;

  server {
    listen 80;
    server_name localhost _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
      try_files $uri $uri/ /index.html;
    }

    location /api/v1/ {
      proxy_pass http://backend:8080/api/v1/;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
      proxy_pass http://backend:8080/api/;
      proxy_http_version 1.1;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      proxy_set_header X-Forwarded-Proto $scheme;
    }
  }
}
```

Comando único para levantar todo:

```bash
docker compose up --build -d
```

Cómo verificar que está corriendo:

```bash
docker compose ps
curl http://localhost:8082/api/v1/actuator/health
curl http://localhost:8083
```

### 5. PRIMER USO

URLs una vez levantado:

- Frontend sin Docker: `http://localhost:4200`
- Frontend con Docker Compose: `http://localhost:8083`
- Backend: `http://localhost:8082/api/v1`
- Swagger/OpenAPI: `http://localhost:8082/api/v1/swagger-ui.html`

Credenciales admin por defecto disponibles en código/scripts:

- Usuario administrador por script/runner: `admin2@sigea.local`
- Contraseña temporal: `password`

Crear el primer administrador con el runner Spring:

Windows PowerShell:

```powershell
Set-Location .\sigea-backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=crear-admin"
```

Unix bash:

```bash
cd sigea-backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=crear-admin
```

Crear el primer administrador con el script SQL:

Windows PowerShell:

```powershell
Get-Content .\sigea-backend\scripts\crear-usuario-admin.sql | mariadb -u root -p sigea_db
```

Unix bash:

```bash
mariadb -u root -p sigea_db < sigea-backend/scripts/crear-usuario-admin.sql
```

Crear un usuario alimentador desde script SQL:

Windows PowerShell:

```powershell
Get-Content .\sigea-backend\scripts\crear-usuario-alimentador.sql | mariadb -u root -p sigea_db
```

Unix bash:

```bash
mariadb -u root -p sigea_db < sigea-backend/scripts/crear-usuario-alimentador.sql
```

### 6. PROBLEMAS COMUNES

- Error de conexión a BD: causa -> MariaDB no está corriendo o el puerto no coincide; solución -> verificar `spring.datasource.url`, confirmar `mariadb -u sigea_user -p -h localhost -P 3306 sigea_db` y revisar que el backend local use el `.env` correcto.
- Error de Flyway checksum mismatch: causa -> una migración aplicada fue modificada; solución -> ejecutar `cd sigea-backend && ./mvnw flyway:repair` en Unix o `Set-Location .\sigea-backend; .\mvnw.cmd flyway:repair` en PowerShell.
- Error de CORS: causa -> `sigea.app.url` no coincide con la URL real del frontend; solución -> ajustar `sigea.app.url` en `.env` y reiniciar backend.
- Puerto 4200/8082/8083 en uso: causa -> otro proceso ocupa el puerto; solución -> liberar el puerto o cambiar `server.port` y actualizar [sigea-frontend/proxy.conf.json](sigea-frontend/proxy.conf.json).
- El frontend no responde a la API en local: causa -> [sigea-frontend/proxy.conf.json](sigea-frontend/proxy.conf.json) apunta a `http://localhost:8082`; solución -> arrancar backend en 8082 o corregir el proxy.
- Registro de usuarios falla al enviar correo: causa -> `sigea.auth.require-email-verification=true` sin SMTP válido; solución -> dejarlo en `false` para desarrollo o completar `spring.mail.*`.
- Errores rojos de Lombok en VS Code: causa -> falso positivo del language server; solución -> validar con `./mvnw -DskipTests compile` o `./mvnw.cmd -DskipTests compile` y limpiar workspace Java si compila bien.
- Subida de archivos falla: causa -> la carpeta `uploads` no existe o no tiene permisos; solución -> crearla en la raíz o dejar que Docker monte el volumen `sigea-uploads`.

### 7. ESTRUCTURA DE CARPETAS CLAVE

```text
SIGEA_SENA/
├── .env.example                 -> ejemplo de configuracion local para backend y referencia de despliegue
├── docker-compose.yml           -> stack local de db, backend y frontend
├── docker/                      -> configuraciones auxiliares de contenedores
├── docs/                        -> PDF, HTML y Markdown de la documentacion generada
├── sigea-backend/               -> API REST Spring Boot
│   ├── src/main/java/           -> codigo fuente por dominio (controller, dto, service, repository, entity)
│   ├── src/main/resources/      -> application.properties, plantillas de correo y migraciones Flyway
│   ├── scripts/                 -> SQL operativos para crear admin, alimentador y limpieza de datos
│   ├── uploads/                 -> almacenamiento local de archivos cuando aplica
│   └── Dockerfile               -> build y runtime del backend en contenedor
├── sigea-frontend/              -> SPA Angular 18
│   ├── src/app/core/            -> services, guards, models e interceptor JWT
│   ├── src/app/layout/          -> shell principal de la aplicacion
│   ├── src/app/pages/           -> pantallas funcionales del sistema
│   ├── proxy.conf.json          -> proxy de desarrollo hacia el backend local
│   └── Dockerfile               -> build Angular + entrega con Nginx
└── tools/                       -> scripts auxiliares, incluido el generador de documentacion
```

Resumen operativo:

- Ruta mas rapida sin Docker: copiar `.env.example`, crear BD local, correr Flyway, levantar backend, levantar frontend.
- Ruta mas rapida con Docker: `docker compose up --build -d` y abrir `http://localhost:8083`.
- Si falta la URL remota del repositorio, eso no está en el código: ⚠️ CONFIGURAR `git clone` con la URL real.
