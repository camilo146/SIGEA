# SIGEA – Diagramas de Diseño del Modelo de Datos

**Versión:** 1.0  
**Fecha:** 16 de febrero de 2026  
**Autor:** Camilo López Romero  

> **Nota:** Para visualizar los diagramas Mermaid en VS Code, instala la extensión
> **"Markdown Preview Mermaid Support"** o **"Mermaid Markdown Syntax Highlighting"**.

---

## 1. Proceso: De los Requerimientos a las Entidades

Antes de dibujar cualquier diagrama, necesitamos **extraer las entidades** (las "cosas" del mundo real que el sistema necesita recordar) directamente de los requerimientos. La regla es simple:

> **Si el sistema necesita almacenar, consultar o modificar información sobre algo → eso es una entidad.**

---

## 2. Entidades Identificadas

| # | Entidad | Descripción | Requerimientos origen | Módulo | Prioridad |
|---|---------|-------------|----------------------|--------|-----------|
| 1 | **Usuario** | Personas que interactúan con el sistema (admins y estándar) | RF-USR-01 a RF-USR-06, RS-AUT-01 a RS-AUT-05 | Usuarios | Must |
| 2 | **Categoria** | Clasificación de equipos (herramientas manuales, equipos de medición, etc.) | RF-INV-05 | Inventario | Must |
| 3 | **Ambiente** | Espacios físicos de formación donde se ubican los equipos | RF-AMB-01 a RF-AMB-05 | Multi-ambiente | Should |
| 4 | **Equipo** | Herramientas, dispositivos e instrumentos del inventario | RF-INV-01 a RF-INV-10 | Inventario | Must |
| 5 | **FotoEquipo** | Fotografías asociadas a cada equipo (máx. 3) | RF-INV-07 | Inventario | Should |
| 6 | **Prestamo** | Registro maestro de un préstamo (cabecera) | RF-PRE-01 a RF-PRE-10 | Préstamos | Must |
| 7 | **DetallePrestamo** | Cada equipo incluido dentro de un préstamo (líneas) | RF-PRE-03, RF-PRE-04, RF-PRE-10 | Préstamos | Must |
| 8 | **ExtensionPrestamo** | Solicitudes de extensión de un préstamo activo (máx. 2) | RF-PRE-07 | Préstamos | Should |
| 9 | **ReporteDano** | Registro de daño al devolver un equipo en mal estado | RF-PRE-08, RN-05 | Préstamos | Should |
| 10 | **Reserva** | Reserva anticipada de equipos (máx. 5 días hábiles) | RF-RES-01 a RF-RES-04 | Reservas | Could |
| 11 | **Transferencia** | Movimiento de equipos entre ambientes de formación | RF-AMB-04, RN-10 | Multi-ambiente | Should |
| 12 | **Mantenimiento** | Historial de reparaciones y mantenimientos por equipo | RF-INV-10 | Inventario | Could |
| 13 | **Notificacion** | Registro de cada notificación enviada o mostrada | RF-NOT-01 a RF-NOT-06 | Notificaciones | Must |
| 14 | **LogAuditoria** | Registro de acciones críticas para trazabilidad | RS-AUD-01, RS-AUD-02 | Sistema | Must |
| 15 | **Configuracion** | Parámetros configurables del sistema (clave-valor) | RF-NOT-04, RS-AUT-03, RS-AUT-04 | Sistema | Should |

**Total: 15 entidades** que cubren todos los módulos del sistema.

---

## 3. Diagrama Entidad-Relación Conceptual

Este diagrama muestra **QUÉ entidades existen** y **CÓMO se relacionan entre sí**, sin entrar en detalles de columnas ni tipos de datos. Es el "mapa general" del sistema.

### ¿Cómo leer este diagrama?

| Símbolo | Significado | Ejemplo |
|---------|------------|---------|
| `\|\|` | Exactamente uno (obligatorio) | Un préstamo tiene exactamente un solicitante |
| `o\|` | Cero o uno (opcional) | Un detalle de préstamo puede tener cero o un reporte de daño |
| `\|{` | Uno o más (obligatorio múltiple) | Un préstamo tiene al menos un detalle |
| `o{` | Cero o más (opcional múltiple) | Un usuario puede tener cero o muchos préstamos |

```mermaid
erDiagram
    USUARIO ||--o{ PRESTAMO : "solicita / aprueba"
    USUARIO ||--o{ RESERVA : "crea"
    USUARIO ||--o{ NOTIFICACION : "recibe"
    USUARIO ||--o{ LOG_AUDITORIA : "genera"
    USUARIO ||--o{ AMBIENTE : "es responsable de"
    USUARIO ||--o{ TRANSFERENCIA : "autoriza"
    USUARIO ||--o{ REPORTE_DANO : "reporta"

    CATEGORIA ||--o{ EQUIPO : "clasifica"
    AMBIENTE ||--o{ EQUIPO : "aloja"

    EQUIPO ||--o{ FOTO_EQUIPO : "tiene fotos"
    EQUIPO ||--o{ DETALLE_PRESTAMO : "incluido en"
    EQUIPO ||--o{ RESERVA : "se reserva"
    EQUIPO ||--o{ TRANSFERENCIA : "se transfiere"
    EQUIPO ||--o{ MANTENIMIENTO : "recibe"

    PRESTAMO ||--|{ DETALLE_PRESTAMO : "contiene"
    PRESTAMO ||--o{ EXTENSION_PRESTAMO : "puede extenderse"

    DETALLE_PRESTAMO |o--o| REPORTE_DANO : "puede generar"

    AMBIENTE ||--o{ TRANSFERENCIA : "origen o destino"

    CONFIGURACION }o--o{ CONFIGURACION : "independiente"
```

### Explicación de las Relaciones Clave

| Relación | Cardinalidad | ¿Por qué? |
|----------|-------------|-----------|
| Usuario → Préstamo | 1:N | Un usuario puede solicitar muchos préstamos. Además, un admin aprueba y otro puede recibir la devolución (3 relaciones diferentes al mismo `Usuario`). |
| Préstamo → DetallePréstamo | 1:N | Un préstamo puede incluir **varios equipos** (RF-PRE-03 dice "equipo**s** prestado**s**"). Cada línea es un `DetallePrestamo`. |
| Equipo → DetallePréstamo | 1:N | Un mismo tipo de equipo puede aparecer en muchos préstamos diferentes a lo largo del tiempo. |
| Equipo → FotoEquipo | 1:N (máx. 3) | RF-INV-07 permite hasta 3 fotos por equipo. Se almacenan como registros separados. |
| Categoría → Equipo | 1:N | Cada equipo pertenece a exactamente una categoría. Una categoría agrupa muchos equipos. |
| Ambiente → Equipo | 1:N | Cada equipo está ubicado en un ambiente. Un ambiente tiene muchos equipos. |
| Préstamo → ExtensionPrestamo | 1:N (máx. 2) | RN-02: máximo 2 extensiones por préstamo. |
| DetallePréstamo → ReporteDaño | 1:0..1 | Solo se genera reporte de daño si el equipo se devuelve en mal estado (RN-05). |
| Ambiente → Transferencia | Doble 1:N | Cada transferencia tiene un ambiente origen y uno destino (dos FKs al mismo `Ambiente`). |

---

## 4. Modelo Relacional – Diagrama de Base de Datos (Físico)

Este diagrama muestra las **tablas reales** que se crearán en MariaDB, con sus **columnas, tipos de datos, llaves primarias (PK) y llaves foráneas (FK)**.

### Convenciones de Nombrado

| Elemento | Convención | Ejemplo |
|----------|-----------|---------|
| Tablas | `snake_case`, singular | `usuario`, `detalle_prestamo` |
| Columnas | `snake_case` | `nombre_completo`, `fecha_creacion` |
| Primary Key | Siempre `id`, tipo `BIGINT`, autoincremental | `id BIGINT PK` |
| Foreign Key | `nombre_entidad_id` | `usuario_id`, `ambiente_id` |
| Campos de auditoría | Presentes en todas las tablas | `fecha_creacion`, `fecha_actualizacion` |
| Soft delete | Campo `activo` tipo `BOOLEAN` | Solo en entidades que lo requieran |

### 4.1 Tabla: Usuario

```mermaid
erDiagram
    Usuario {
        BIGINT id PK "Identificador unico autoincremental"
        VARCHAR nombre_completo "NOT NULL - Maximo 150 caracteres"
        VARCHAR numero_documento UK "NOT NULL - Unico e irrepetible"
        VARCHAR tipo_documento "NOT NULL - CC TI CE PP PEP"
        VARCHAR correo_electronico UK "Unico - Puede ser NULL"
        VARCHAR telefono "Opcional - Maximo 20 caracteres"
        VARCHAR programa_formacion "Programa SENA - Maximo 200 chars"
        VARCHAR ficha "Identificador del grupo - Maximo 20"
        VARCHAR contrasena_hash "NOT NULL - Hash BCrypt 255 chars"
        VARCHAR rol "NOT NULL - ADMINISTRADOR o USUARIO_ESTANDAR"
        BOOLEAN es_super_admin "DEFAULT FALSE"
        BOOLEAN activo "DEFAULT TRUE - Soft delete"
        INT intentos_fallidos "DEFAULT 0 - Max 3 antes de bloqueo"
        DATETIME cuenta_bloqueada_hasta "NULL si no esta bloqueada"
        DATETIME fecha_creacion "NOT NULL - Se fija al crear"
        DATETIME fecha_actualizacion "NOT NULL - Se actualiza siempre"
    }
```

**¿Por qué estos campos?**
- `contrasena_hash`: RS-CIF-01 exige hash BCrypt, **nunca texto plano**.
- `es_super_admin`: RF-AMB-05 distingue al primer admin que puede gestionar todos los ambientes.
- `intentos_fallidos` + `cuenta_bloqueada_hasta`: RS-AUT-03, bloqueo tras 3 intentos (5 min → 15 min).
- `activo`: RF-USR-03, la eliminación es **lógica** (el registro no se borra).

### 4.2 Tablas: Inventario

```mermaid
erDiagram
    Categoria {
        BIGINT id PK
        VARCHAR nombre UK "NOT NULL - Maximo 100 chars"
        TEXT descripcion "Opcional"
        BOOLEAN activo "DEFAULT TRUE"
        DATETIME fecha_creacion "NOT NULL"
    }

    Ambiente {
        BIGINT id PK
        VARCHAR nombre "NOT NULL - Maximo 150 chars"
        VARCHAR ubicacion "Descripcion fisica - Maximo 200"
        TEXT descripcion "Opcional"
        BIGINT instructor_responsable_id FK "FK a Usuario"
        BOOLEAN activo "DEFAULT TRUE"
        DATETIME fecha_creacion "NOT NULL"
        DATETIME fecha_actualizacion "NOT NULL"
    }

    Equipo {
        BIGINT id PK
        VARCHAR nombre "NOT NULL - Maximo 200 chars"
        TEXT descripcion "Opcional"
        VARCHAR codigo_unico UK "NOT NULL - Irrepetible RF-INV-06"
        BIGINT categoria_id FK "FK a Categoria"
        VARCHAR estado "NOT NULL - ACTIVO o EN_MANTENIMIENTO"
        INT cantidad_total "NOT NULL - DEFAULT 1"
        INT cantidad_disponible "NOT NULL - DEFAULT 1"
        BIGINT ambiente_id FK "FK a Ambiente"
        INT umbral_minimo "DEFAULT 0 - Alerta stock bajo"
        BOOLEAN activo "DEFAULT TRUE - FALSE es dado de baja"
        DATETIME fecha_creacion "NOT NULL"
        DATETIME fecha_actualizacion "NOT NULL"
    }

    FotoEquipo {
        BIGINT id PK
        BIGINT equipo_id FK "FK a Equipo - NOT NULL"
        VARCHAR nombre_archivo "NOT NULL - Nombre original"
        VARCHAR ruta_archivo "NOT NULL - Ruta en servidor"
        BIGINT tamano_bytes "Peso del archivo"
        DATETIME fecha_subida "NOT NULL"
    }

    Categoria ||--o{ Equipo : "clasifica"
    Ambiente ||--o{ Equipo : "aloja"
    Equipo ||--o{ FotoEquipo : "tiene fotos max 3"
```

**¿Por qué `cantidad_total` y `cantidad_disponible`?**
- RF-INV-01 incluye `cantidad` como campo del equipo. Ejemplo: "Cable UTP Cat6" puede tener 50 unidades.
- `cantidad_disponible` se **decrementa** al prestar y se **incrementa** al devolver (RF-PRE-10).
- Un equipo está "disponible" cuando: `activo = TRUE`, `estado = 'ACTIVO'` y `cantidad_disponible > 0`.

**¿Por qué `Categoria` es una tabla y no un ENUM?**
- Principio **Open/Closed (SOLID)**: Si mañana se agrega una categoría nueva, solo se inserta un registro en la tabla. **No se modifica código**.

**¿Por qué las fotos van en tabla separada?**
- **Normalización**: Un equipo puede tener 0, 1, 2 o 3 fotos. Si pusiéramos `foto1`, `foto2`, `foto3` como columnas en `equipo`, violaríamos la Primera Forma Normal (1FN) que dice "no puede haber grupos repetitivos".

### 4.3 Tablas: Préstamos

```mermaid
erDiagram
    Prestamo {
        BIGINT id PK
        BIGINT usuario_solicitante_id FK "FK a Usuario - NOT NULL"
        BIGINT administrador_aprueba_id FK "FK a Usuario - NULL hasta aprobar"
        BIGINT administrador_recibe_id FK "FK a Usuario - NULL hasta devolver"
        DATETIME fecha_hora_solicitud "NOT NULL - Se fija al solicitar"
        DATETIME fecha_hora_aprobacion "NULL hasta que el admin apruebe"
        DATETIME fecha_hora_salida "NULL hasta entrega fisica"
        DATETIME fecha_hora_devolucion_estimada "NOT NULL - Fecha limite"
        DATETIME fecha_hora_devolucion_real "NULL hasta que se devuelva"
        VARCHAR estado "NOT NULL - Ver estados abajo"
        TEXT observaciones_generales "Notas del prestamo"
        INT extensiones_realizadas "DEFAULT 0 - Maximo 2"
        DATETIME fecha_creacion "NOT NULL"
        DATETIME fecha_actualizacion "NOT NULL"
    }

    DetallePrestamo {
        BIGINT id PK
        BIGINT prestamo_id FK "FK a Prestamo - NOT NULL"
        BIGINT equipo_id FK "FK a Equipo - NOT NULL"
        INT cantidad "NOT NULL - DEFAULT 1"
        TEXT observaciones_entrega "Estado al prestar RN-03"
        VARCHAR estado_equipo_entrega "EXCELENTE BUENO REGULAR MALO"
        TEXT observaciones_devolucion "Estado al devolver RN-04"
        VARCHAR estado_equipo_devolucion "EXCELENTE BUENO REGULAR MALO"
        BOOLEAN devuelto "DEFAULT FALSE"
    }

    ExtensionPrestamo {
        BIGINT id PK
        BIGINT prestamo_id FK "FK a Prestamo - NOT NULL"
        DATETIME fecha_solicitud "NOT NULL"
        DATETIME nueva_fecha_devolucion "NOT NULL"
        BIGINT administrador_aprueba_id FK "FK a Usuario - NULL hasta aprobar"
        VARCHAR estado "SOLICITADA APROBADA RECHAZADA"
        TEXT motivo "Razon de la extension"
        DATETIME fecha_respuesta "NULL hasta que se responda"
    }

    ReporteDano {
        BIGINT id PK
        BIGINT detalle_prestamo_id FK "FK a DetallePrestamo - NOT NULL"
        TEXT descripcion "NOT NULL - Descripcion del dano"
        VARCHAR foto_ruta "Opcional - Ruta de foto del dano"
        DATETIME fecha_reporte "NOT NULL"
        BIGINT reportado_por_id FK "FK a Usuario - NOT NULL"
    }

    Prestamo ||--|{ DetallePrestamo : "contiene items"
    DetallePrestamo |o--o| ReporteDano : "puede generar"
    Prestamo ||--o{ ExtensionPrestamo : "puede extenderse max 2"
```

**Estados del Préstamo (ciclo de vida):**

```
SOLICITADO → APROBADO → ACTIVO → DEVUELTO
     │                    │
     ↓                    ↓
  RECHAZADO            EN_MORA → DEVUELTO
     │
     ↓
  CANCELADO
```

- `SOLICITADO`: El usuario creó la solicitud, esperando aprobación del admin.
- `APROBADO`: El admin autorizó pero aún no se ha hecho la entrega física.
- `ACTIVO`: El equipo fue entregado al usuario.
- `DEVUELTO`: El equipo fue devuelto y recibido.
- `RECHAZADO`: El admin denegó la solicitud.
- `EN_MORA`: Pasó la fecha de devolución estimada sin devolución (RF-PRE-06).
- `CANCELADO`: El usuario canceló la solicitud antes de la aprobación.

**¿Por qué `DetallePrestamo` como tabla separada?**
- Esto es un patrón clásico de diseño: **Cabecera-Detalle** (Master-Detail), igual que una factura con sus líneas.
- Un préstamo = la cabecera (quién, cuándo, estado general).
- Cada detalle = un equipo específico con su cantidad y observaciones.
- Esto resuelve la relación **muchos-a-muchos** entre `Prestamo` y `Equipo`.

**¿Por qué `devuelto` en DetallePrestamo?**
- Permite **devoluciones parciales**: si presto 3 equipos, puedo devolver 2 hoy y 1 mañana. Esto es extensibilidad pensando a futuro.

### 4.4 Tablas: Reservas, Transferencias y Mantenimiento

```mermaid
erDiagram
    Reserva {
        BIGINT id PK
        BIGINT usuario_id FK "FK a Usuario - NOT NULL"
        BIGINT equipo_id FK "FK a Equipo - NOT NULL"
        INT cantidad "NOT NULL - DEFAULT 1"
        DATETIME fecha_hora_inicio "NOT NULL"
        DATETIME fecha_hora_fin "NOT NULL"
        VARCHAR estado "ACTIVA CANCELADA COMPLETADA EXPIRADA"
        DATETIME fecha_creacion "NOT NULL"
    }

    Transferencia {
        BIGINT id PK
        BIGINT equipo_id FK "FK a Equipo - NOT NULL"
        BIGINT ambiente_origen_id FK "FK a Ambiente - NOT NULL"
        BIGINT ambiente_destino_id FK "FK a Ambiente - NOT NULL"
        INT cantidad "NOT NULL - DEFAULT 1"
        BIGINT administrador_autoriza_id FK "FK a Usuario - NOT NULL"
        TEXT motivo "Razon de la transferencia"
        DATETIME fecha_transferencia "NOT NULL"
    }

    Mantenimiento {
        BIGINT id PK
        BIGINT equipo_id FK "FK a Equipo - NOT NULL"
        VARCHAR tipo "PREVENTIVO o CORRECTIVO"
        TEXT descripcion "NOT NULL"
        DATETIME fecha_inicio "NOT NULL"
        DATETIME fecha_fin "NULL si aun esta en proceso"
        VARCHAR responsable "Persona a cargo - Max 150"
        TEXT observaciones "Notas adicionales"
        DATETIME fecha_creacion "NOT NULL"
    }
```

**¿Por qué `Transferencia` tiene dos FKs a `Ambiente`?**
- RF-AMB-04 exige registrar "ambiente origen" y "ambiente destino". Ambos apuntan a la **misma tabla** `Ambiente`, pero representan conceptos diferentes. Esto se llama **relación reflexiva** (dos FKs de la misma tabla a otra).

**¿Por qué `Mantenimiento` está incluido aunque es "Could Have"?**
- Principio de **diseño para extensibilidad**: es mucho más fácil crear la tabla ahora (solo estructura, sin código) que alterar el modelo después cuando ya hay datos en producción.

### 4.5 Tablas: Sistema (Notificaciones, Auditoría, Configuración)

```mermaid
erDiagram
    Notificacion {
        BIGINT id PK
        BIGINT usuario_destino_id FK "FK a Usuario - NOT NULL"
        VARCHAR tipo "RECORDATORIO MORA STOCK_BAJO GENERAL"
        VARCHAR titulo "NOT NULL - Maximo 200 chars"
        TEXT mensaje "NOT NULL - Cuerpo de la notificacion"
        VARCHAR medio_envio "EMAIL o INTERNA"
        VARCHAR estado_envio "PENDIENTE ENVIADA FALLIDA"
        BOOLEAN leida "DEFAULT FALSE - Solo para internas"
        DATETIME fecha_envio "NULL si aun no se envia"
        DATETIME fecha_creacion "NOT NULL"
    }

    LogAuditoria {
        BIGINT id PK
        BIGINT usuario_id FK "FK a Usuario - Puede ser NULL"
        VARCHAR accion "NOT NULL - LOGIN LOGOUT CREAR EDITAR etc"
        VARCHAR entidad_afectada "NOT NULL - Nombre de la tabla"
        BIGINT entidad_id "ID del registro afectado"
        TEXT detalles "Descripcion de lo que cambio"
        VARCHAR direccion_ip "IPv4 o IPv6 - Maximo 45 chars"
        DATETIME fecha_hora "NOT NULL"
    }

    Configuracion {
        BIGINT id PK
        VARCHAR clave UK "NOT NULL - Identificador unico"
        VARCHAR valor "NOT NULL - Valor del parametro"
        VARCHAR tipo "STRING INTEGER BOOLEAN"
        TEXT descripcion "Que hace este parametro"
        DATETIME fecha_actualizacion "NOT NULL"
    }
```

**¿Por qué `LogAuditoria.usuario_id` puede ser NULL?**
- Hay acciones del sistema que no las ejecuta un usuario humano (ej: una tarea programada que envía notificaciones automáticas). En esos casos, `usuario_id` es `NULL`.

**¿Por qué `Configuracion` es una tabla clave-valor?**
- Permite cambiar parámetros del sistema (ej: `sesion.timeout.minutos = 30`, `intentos.maximos = 3`, `stock.alerta.umbral = 5`) **sin modificar código ni redesplegar**. Solo se actualiza un registro en la BD.

---

## 5. Diagrama de Relaciones Completo

Este diagrama muestra **TODAS las tablas y TODAS las relaciones** del sistema en una sola vista:

```mermaid
erDiagram
    Usuario ||--o{ Prestamo : "solicita"
    Usuario ||--o{ Prestamo : "aprueba"
    Usuario ||--o{ Prestamo : "recibe devolucion"
    Usuario ||--o{ Reserva : "crea"
    Usuario ||--o{ Notificacion : "recibe"
    Usuario ||--o{ LogAuditoria : "genera"
    Usuario ||--o{ Ambiente : "responsable"
    Usuario ||--o{ Transferencia : "autoriza"
    Usuario ||--o{ ReporteDano : "reporta"
    Usuario ||--o{ ExtensionPrestamo : "aprueba ext"

    Categoria ||--o{ Equipo : "clasifica"
    Ambiente ||--o{ Equipo : "aloja"

    Equipo ||--o{ FotoEquipo : "tiene fotos"
    Equipo ||--o{ DetallePrestamo : "prestado en"
    Equipo ||--o{ Reserva : "reservado"
    Equipo ||--o{ Transferencia : "transferido"
    Equipo ||--o{ Mantenimiento : "mantenimiento"

    Prestamo ||--|{ DetallePrestamo : "contiene"
    Prestamo ||--o{ ExtensionPrestamo : "extensiones"

    DetallePrestamo |o--o| ReporteDano : "dano"

    Ambiente ||--o{ Transferencia : "origen"
    Ambiente ||--o{ Transferencia : "destino"
```

---

## 6. Diagrama de Clases – Modelo de Dominio (Java / Spring Boot)

Este diagrama muestra cómo se traducen las tablas de la base de datos a **clases Java** (entidades JPA). Cada tabla se convierte en una clase con la anotación `@Entity` de JPA.

### 6.1 Enumeraciones (Enums)

Los valores fijos que no cambian se modelan como **enumeraciones** en Java. Esto da **seguridad de tipos** en tiempo de compilación: si escribes mal un valor, Java te avisa antes de ejecutar.

```mermaid
classDiagram
    class Rol {
        <<enumeration>>
        ADMINISTRADOR
        USUARIO_ESTANDAR
    }

    class TipoDocumento {
        <<enumeration>>
        CC
        TI
        CE
        PP
        PEP
    }

    class EstadoEquipo {
        <<enumeration>>
        ACTIVO
        EN_MANTENIMIENTO
    }

    class EstadoPrestamo {
        <<enumeration>>
        SOLICITADO
        APROBADO
        RECHAZADO
        ACTIVO
        DEVUELTO
        EN_MORA
        CANCELADO
    }

    class EstadoCondicion {
        <<enumeration>>
        EXCELENTE
        BUENO
        REGULAR
        MALO
    }

    class EstadoReserva {
        <<enumeration>>
        ACTIVA
        CANCELADA
        COMPLETADA
        EXPIRADA
    }

    class EstadoExtension {
        <<enumeration>>
        SOLICITADA
        APROBADA
        RECHAZADA
    }

    class TipoNotificacion {
        <<enumeration>>
        RECORDATORIO_VENCIMIENTO
        MORA
        STOCK_BAJO
        SOLICITUD_PRESTAMO
        GENERAL
    }

    class MedioEnvio {
        <<enumeration>>
        EMAIL
        INTERNA
    }

    class EstadoEnvio {
        <<enumeration>>
        PENDIENTE
        ENVIADA
        FALLIDA
    }

    class TipoMantenimiento {
        <<enumeration>>
        PREVENTIVO
        CORRECTIVO
    }
```

### 6.2 Clase Base (EntidadBase)

Todas las entidades comparten campos comunes: `id`, `fechaCreacion`, `fechaActualizacion`. Para no **repetir** estos campos en cada clase, creamos una **clase abstracta** de la que todas heredan.

**Principios aplicados:**
- **Herencia (POO)**: Las clases hijas obtienen automáticamente los campos de la clase padre.
- **DRY (Don't Repeat Yourself)**: Escribimos `id`, `fechaCreacion`, `fechaActualizacion` **una sola vez**.
- **Liskov Substitution (SOLID)**: Cualquier entidad puede ser tratada como `EntidadBase`.

```mermaid
classDiagram
    class EntidadBase {
        <<abstract>>
        #Long id
        #LocalDateTime fechaCreacion
        #LocalDateTime fechaActualizacion
        #onCreate() void
        #onUpdate() void
    }

    note for EntidadBase "Anotaciones JPA:\n@MappedSuperclass\n@PrePersist → onCreate()\n@PreUpdate → onUpdate()"
```

### 6.3 Entidades del Dominio

```mermaid
classDiagram
    class EntidadBase {
        <<abstract>>
        #Long id
        #LocalDateTime fechaCreacion
        #LocalDateTime fechaActualizacion
    }

    class Usuario {
        -String nombreCompleto
        -String numeroDocumento
        -TipoDocumento tipoDocumento
        -String correoElectronico
        -String telefono
        -String programaFormacion
        -String ficha
        -String contrasenaHash
        -Rol rol
        -Boolean esSuperAdmin
        -Boolean activo
        -Integer intentosFallidos
        -LocalDateTime cuentaBloqueadaHasta
        -List~Prestamo~ prestamosRealizados
        -List~Reserva~ reservas
        -List~Notificacion~ notificaciones
    }

    class Categoria {
        -String nombre
        -String descripcion
        -Boolean activo
    }

    class Ambiente {
        -String nombre
        -String ubicacion
        -String descripcion
        -Usuario instructorResponsable
        -Boolean activo
        -List~Equipo~ equipos
    }

    class Equipo {
        -String nombre
        -String descripcion
        -String codigoUnico
        -Categoria categoria
        -EstadoEquipo estado
        -Integer cantidadTotal
        -Integer cantidadDisponible
        -Ambiente ambiente
        -Integer umbralMinimo
        -Boolean activo
        -List~FotoEquipo~ fotos
    }

    class FotoEquipo {
        -Equipo equipo
        -String nombreArchivo
        -String rutaArchivo
        -Long tamanoBytes
        -LocalDateTime fechaSubida
    }

    class Prestamo {
        -Usuario usuarioSolicitante
        -Usuario administradorAprueba
        -Usuario administradorRecibe
        -LocalDateTime fechaHoraSolicitud
        -LocalDateTime fechaHoraAprobacion
        -LocalDateTime fechaHoraSalida
        -LocalDateTime fechaHoraDevolucionEstimada
        -LocalDateTime fechaHoraDevolucionReal
        -EstadoPrestamo estado
        -String observacionesGenerales
        -Integer extensionesRealizadas
        -List~DetallePrestamo~ detalles
        -List~ExtensionPrestamo~ extensiones
    }

    class DetallePrestamo {
        -Prestamo prestamo
        -Equipo equipo
        -Integer cantidad
        -String observacionesEntrega
        -EstadoCondicion estadoEquipoEntrega
        -String observacionesDevolucion
        -EstadoCondicion estadoEquipoDevolucion
        -Boolean devuelto
        -ReporteDano reporteDano
    }

    class ExtensionPrestamo {
        -Prestamo prestamo
        -LocalDateTime fechaSolicitud
        -LocalDateTime nuevaFechaDevolucion
        -Usuario administradorAprueba
        -EstadoExtension estado
        -String motivo
        -LocalDateTime fechaRespuesta
    }

    class ReporteDano {
        -DetallePrestamo detallePrestamo
        -String descripcion
        -String fotoRuta
        -LocalDateTime fechaReporte
        -Usuario reportadoPor
    }

    class Reserva {
        -Usuario usuario
        -Equipo equipo
        -Integer cantidad
        -LocalDateTime fechaHoraInicio
        -LocalDateTime fechaHoraFin
        -EstadoReserva estado
    }

    class Transferencia {
        -Equipo equipo
        -Ambiente ambienteOrigen
        -Ambiente ambienteDestino
        -Integer cantidad
        -Usuario administradorAutoriza
        -String motivo
        -LocalDateTime fechaTransferencia
    }

    class Mantenimiento {
        -Equipo equipo
        -TipoMantenimiento tipo
        -String descripcion
        -LocalDateTime fechaInicio
        -LocalDateTime fechaFin
        -String responsable
        -String observaciones
    }

    class Notificacion {
        -Usuario usuarioDestino
        -TipoNotificacion tipo
        -String titulo
        -String mensaje
        -MedioEnvio medioEnvio
        -EstadoEnvio estadoEnvio
        -Boolean leida
        -LocalDateTime fechaEnvio
    }

    class LogAuditoria {
        -Usuario usuario
        -String accion
        -String entidadAfectada
        -Long entidadId
        -String detalles
        -String direccionIp
        -LocalDateTime fechaHora
    }

    class Configuracion {
        -String clave
        -String valor
        -String tipo
        -String descripcion
    }

    EntidadBase <|-- Usuario
    EntidadBase <|-- Categoria
    EntidadBase <|-- Ambiente
    EntidadBase <|-- Equipo
    EntidadBase <|-- FotoEquipo
    EntidadBase <|-- Prestamo
    EntidadBase <|-- DetallePrestamo
    EntidadBase <|-- ExtensionPrestamo
    EntidadBase <|-- ReporteDano
    EntidadBase <|-- Reserva
    EntidadBase <|-- Transferencia
    EntidadBase <|-- Mantenimiento
    EntidadBase <|-- Notificacion
    EntidadBase <|-- LogAuditoria
    EntidadBase <|-- Configuracion

    Usuario "1" --> "*" Prestamo : solicita
    Usuario "1" --> "*" Reserva : crea
    Usuario "1" --> "1" Ambiente : responsable de

    Categoria "1" --> "*" Equipo : clasifica
    Ambiente "1" --> "*" Equipo : aloja
    Equipo "1" --> "*" FotoEquipo : tiene

    Prestamo "1" *-- "*" DetallePrestamo : contiene
    Prestamo "1" --> "*" ExtensionPrestamo : extensiones
    DetallePrestamo "1" --> "0..1" ReporteDano : dano

    Equipo "1" --> "*" DetallePrestamo : incluido
    Equipo "1" --> "*" Reserva : reservado
    Equipo "1" --> "*" Transferencia : transferido
    Equipo "1" --> "*" Mantenimiento : mantenimiento

    Usuario "1" --> "*" Notificacion : recibe
    Usuario "1" --> "*" LogAuditoria : genera
```

### 6.4 Mapeo: Tabla BD ↔ Clase Java ↔ Convención de Nombres

| Base de Datos (snake_case) | Java (camelCase) | Anotación JPA |
|---------------------------|------------------|---------------|
| `usuario` | `Usuario` | `@Entity @Table(name = "usuario")` |
| `nombre_completo` | `nombreCompleto` | `@Column(name = "nombre_completo")` |
| `usuario_solicitante_id` | `usuarioSolicitante` | `@ManyToOne @JoinColumn(name = "usuario_solicitante_id")` |
| `prestamos` (no existe en BD) | `List<Prestamo> prestamos` | `@OneToMany(mappedBy = "usuarioSolicitante")` |

> **Regla clave**: En la BD usamos `snake_case` porque es la convención de SQL. En Java usamos `camelCase` porque es la convención de Java. **JPA se encarga de traducir** entre ambos mundos con `@Column(name = "...")`.

---

## 7. Decisiones de Diseño y Principios Aplicados

### 7.1 Principios POO Aplicados

| Principio | Dónde se aplica | Ejemplo concreto |
|-----------|----------------|-----------------|
| **Encapsulamiento** | Todos los campos son `private` (`-`) | Solo se accede mediante getters/setters. Los datos internos están protegidos. |
| **Herencia** | `EntidadBase` → Todas las entidades | `id`, `fechaCreacion`, `fechaActualizacion` se escriben una sola vez. |
| **Polimorfismo** | Enumeraciones con comportamiento | `EstadoPrestamo` puede tener métodos como `puedeExtenderse()` que retorna `true` solo si es `ACTIVO`. |
| **Abstracción** | `EntidadBase` es `abstract` | No se puede instanciar directamente. Solo sus hijos concretos existen. |

### 7.2 Principios SOLID Aplicados

| Principio | Aplicación |
|-----------|-----------|
| **S – Single Responsibility** | Cada entidad tiene una sola razón de existir. `Prestamo` solo maneja la cabecera. `DetallePrestamo` solo maneja los items. `ReporteDano` solo maneja daños. |
| **O – Open/Closed** | `Categoria` es una tabla, no un ENUM en código. Agregar categorías no requiere cambiar código. Las enumeraciones de estados se usan donde los valores son **verdaderamente fijos** (APROBADO/RECHAZADO no va a cambiar). |
| **L – Liskov Substitution** | Todas las entidades heredan de `EntidadBase`. Cualquier método que reciba `EntidadBase` funciona con cualquier entidad hija. |
| **I – Interface Segregation** | `activo` NO está en `EntidadBase` porque no todas las entidades necesitan soft-delete. Solo `Usuario`, `Equipo`, `Ambiente` y `Categoria` lo tienen. |
| **D – Dependency Inversion** | Las relaciones se definen por tipo abstracto cuando es posible. Los servicios dependerán de interfaces (Repository), no de implementaciones concretas. |

### 7.3 Clean Code Aplicado

| Práctica | Ejemplo |
|----------|---------|
| **Nombres descriptivos** | `fechaHoraDevolucionEstimada` en vez de `fhde` o `fecha2`. |
| **Sin abreviaciones** | `observacionesEntrega` en vez de `obs_ent`. |
| **Consistencia** | Todos los IDs son `Long`, todas las fechas son `LocalDateTime`, todos los textos largos son `String` (mapeado a `TEXT`). |
| **Sin números mágicos** | El máximo de extensiones (2), el máximo de fotos (3), los tiempos de bloqueo (5 min, 15 min) se almacenan en la tabla `Configuracion`, no hardcodeados. |

### 7.4 Normalización de la Base de Datos

| Forma Normal | Cómo se cumple |
|-------------|---------------|
| **1FN** | No hay grupos repetitivos. Las fotos no son `foto1, foto2, foto3` sino registros en `FotoEquipo`. |
| **2FN** | Todos los campos no-clave dependen de la clave primaria completa, no de una parte. |
| **3FN** | No hay dependencias transitivas. El nombre de la categoría no se guarda en `equipo`; solo su `categoria_id`. |

---

## 8. Resumen de Cardinalidades

| Relación | Tipo | Regla de Negocio |
|----------|------|-----------------|
| Usuario → Préstamo | 1:N | Un usuario puede solicitar muchos préstamos |
| Préstamo → DetallePréstamo | 1:N | Un préstamo contiene uno o más equipos |
| Equipo → DetallePréstamo | 1:N | Un equipo puede ser prestado muchas veces |
| Equipo → FotoEquipo | 1:N (max 3) | Máximo 3 fotos por equipo |
| Categoría → Equipo | 1:N | Una categoría agrupa muchos equipos |
| Ambiente → Equipo | 1:N | Un ambiente contiene muchos equipos |
| Ambiente → Usuario (responsable) | N:1 | Cada ambiente tiene un instructor responsable |
| Préstamo → ExtensionPréstamo | 1:N (max 2) | Máximo 2 extensiones por préstamo |
| DetallePréstamo → ReporteDaño | 1:0..1 | Solo si se devuelve dañado |
| Usuario → Reserva | 1:N | Un usuario puede tener muchas reservas |
| Equipo → Reserva | 1:N | Un equipo puede ser reservado muchas veces |
| Equipo → Transferencia | 1:N | Un equipo puede ser transferido muchas veces |
| Ambiente → Transferencia | Doble 1:N | Origen y destino son ambientes |
| Equipo → Mantenimiento | 1:N | Un equipo puede tener muchos mantenimientos |
| Usuario → Notificación | 1:N | Un usuario recibe muchas notificaciones |
| Usuario → LogAuditoría | 1:N | Un usuario genera muchas entradas de log |

---

> **Próximo paso:** Con estos diagramas aprobados, procederemos a crear la estructura de carpetas
> del proyecto (backend Spring Boot + frontend Angular) y configurar el entorno de desarrollo.

---

*Documento generado como parte del proyecto SIGEA – SENA CIMI*
