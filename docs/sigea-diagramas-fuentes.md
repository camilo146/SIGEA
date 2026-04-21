# SIGEA - Fuentes Mermaid

## 01 Arquitectura

```mermaid
flowchart TB
    classDef client fill:#dbeafe,stroke:#1d4ed8,color:#0f172a
    classDef fe fill:#e2e8f0,stroke:#334155,color:#0f172a
    classDef be fill:#dcfce7,stroke:#166534,color:#14532d
    classDef data fill:#fef9c3,stroke:#a16207,color:#713f12
    classDef infra fill:#ede9fe,stroke:#5b21b6,color:#4c1d95

    USER[Usuario final<br/>Navegador web / móvil]:::client

    subgraph FE[Frontend Angular 18 SPA]
        ROUTER[Angular Router<br/>guards de rol y sesión]:::fe
        PAGES[Páginas de negocio<br/>inventario préstamos reservas reportes]:::fe
        CORE[HttpClient + services<br/>interceptor JWT + manejo de errores]:::fe
    end

    subgraph API[Backend Spring Boot 3.5.10 / Java 21]
        SEC[SecurityConfig + JwtFiltroAutenticacion<br/>BCrypt + autorización por rol]:::be
        CTRL[REST Controllers<br/>base path /api/v1]:::be
        SVC[Servicios de dominio<br/>inventario préstamos reservas transferencias notificaciones]:::be
        JOBS[Tareas programadas y reglas automáticas<br/>mora recordatorios stock bajo]:::be
        REPO[Spring Data JPA Repositories]:::be
        MAIL[CorreoServicio + Thymeleaf<br/>emails HTML institucionales]:::be
    end

    subgraph PERSIST[Persistencia e infraestructura]
        DB[(MariaDB 11.4<br/>Flyway migrations)]:::data
        UP[(Volumen de uploads<br/>fotos y archivos)]:::data
        SMTP[SMTP Gmail<br/>SSL/TLS 465 o STARTTLS 587]:::infra
    end

    USER --> ROUTER
    ROUTER --> PAGES --> CORE
    CORE --> SEC --> CTRL --> SVC
    SVC --> REPO --> DB
    SVC --> JOBS
    SVC --> MAIL --> SMTP
    SVC --> UP
```

## 02 MER

```mermaid
erDiagram
    USUARIO {
        bigint id PK
        varchar nombre_completo
        varchar numero_documento UK
        varchar correo_electronico UK
        varchar rol
        boolean activo
        boolean email_verificado
        varchar estado_aprobacion
    }

    CATEGORIA {
        bigint id PK
        varchar nombre UK
        boolean activo
    }

    MARCA {
        bigint id PK
        varchar nombre UK
        boolean activo
    }

    AMBIENTE {
        bigint id PK
        varchar nombre
        varchar ubicacion
        bigint instructor_responsable_id FK
        bigint propietario_id FK
        bigint padre_id FK
        boolean activo
    }

    EQUIPO {
        bigint id PK
        varchar nombre
        varchar codigo_unico UK
        varchar placa UK
        varchar serial
        varchar modelo
        bigint categoria_id FK
        bigint marca_id FK
        bigint ambiente_id FK
        bigint sub_ubicacion_id FK
        bigint propietario_id FK
        bigint inventario_actual_instructor_id FK
        varchar estado
        varchar tipo_uso
        int cantidad_total
        int cantidad_disponible
        int umbral_minimo
        boolean activo
    }

    FOTO_EQUIPO {
        bigint id PK
        bigint equipo_id FK
        varchar nombre_archivo
        varchar ruta_archivo
    }

    PRESTAMO {
        bigint id PK
        bigint usuario_solicitante_id FK
        bigint administrador_aprueba_id FK
        bigint administrador_recibe_id FK
        bigint reserva_id FK
        varchar estado
        datetime fecha_hora_solicitud
        datetime fecha_hora_devolucion_estimada
    }

    DETALLE_PRESTAMO {
        bigint id PK
        bigint prestamo_id FK
        bigint equipo_id FK
        int cantidad
        boolean devuelto
        varchar estado_equipo_entrega
        varchar estado_equipo_devolucion
    }

    EXTENSION_PRESTAMO {
        bigint id PK
        bigint prestamo_id FK
        bigint administrador_aprueba_id FK
        varchar estado
        datetime nueva_fecha_devolucion
    }

    REPORTE_DANO {
        bigint id PK
        bigint detalle_prestamo_id FK
        bigint reportado_por_id FK
        text descripcion
    }

    RESERVA {
        bigint id PK
        bigint usuario_id FK
        bigint equipo_id FK
        int cantidad
        varchar estado
        datetime fecha_hora_inicio
        datetime fecha_hora_fin
    }

    TRANSFERENCIA {
        bigint id PK
        bigint equipo_id FK
        bigint inventario_origen_instructor_id FK
        bigint inventario_destino_instructor_id FK
        bigint propietario_equipo_id FK
        bigint ubicacion_destino_id FK
        bigint administrador_autoriza_id FK
        int cantidad
    }

    MANTENIMIENTO {
        bigint id PK
        bigint equipo_id FK
        varchar tipo
        date fecha_inicio
        date fecha_fin
    }

    OBSERVACION_EQUIPO {
        bigint id PK
        bigint prestamo_id FK
        bigint equipo_id FK
        bigint usuario_duenio_id FK
        bigint usuario_prestatario_id FK
        int estado_devolucion
    }

    PRESTAMO_AMBIENTE {
        bigint id PK
        bigint ambiente_id FK
        bigint solicitante_id FK
        bigint propietario_ambiente_id FK
        date fecha_inicio
        date fecha_fin
        varchar estado
    }

    NOTIFICACION {
        bigint id PK
        bigint usuario_destino_id FK
        varchar tipo
        varchar medio_envio
        varchar estado_envio
        boolean leida
    }

    LOG_AUDITORIA {
        bigint id PK
        bigint usuario_id FK
        varchar accion
        varchar entidad_afectada
        bigint entidad_id
    }

    CONFIGURACION {
        bigint id PK
        varchar clave UK
        varchar valor
    }

    USUARIO ||--o{ AMBIENTE : responsable_propietario
    AMBIENTE ||--o{ AMBIENTE : jerarquia
    CATEGORIA ||--o{ EQUIPO : clasifica
    MARCA ||--o{ EQUIPO : fabrica
    AMBIENTE ||--o{ EQUIPO : ubica
    USUARIO ||--o{ EQUIPO : custodio_inventario
    EQUIPO ||--o{ FOTO_EQUIPO : adjunta
    USUARIO ||--o{ PRESTAMO : solicita_aprueba_recibe
    PRESTAMO ||--|{ DETALLE_PRESTAMO : contiene
    EQUIPO ||--o{ DETALLE_PRESTAMO : item
    PRESTAMO ||--o{ EXTENSION_PRESTAMO : extiende
    DETALLE_PRESTAMO ||--o| REPORTE_DANO : genera
    USUARIO ||--o{ RESERVA : crea
    EQUIPO ||--o{ RESERVA : bloquea_stock
    RESERVA ||--o| PRESTAMO : origen_opcional
    EQUIPO ||--o{ TRANSFERENCIA : mueve
    EQUIPO ||--o{ MANTENIMIENTO : recibe
    EQUIPO ||--o{ OBSERVACION_EQUIPO : historial
    PRESTAMO ||--o{ OBSERVACION_EQUIPO : contexto
    AMBIENTE ||--o{ PRESTAMO_AMBIENTE : agenda
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO ||--o{ LOG_AUDITORIA : ejecuta
```

## 03 Clases UML

### 03a Dominio base e inventario

```mermaid
classDiagram
    direction LR

    class EntidadBase {
        <<abstract>>
        +Long id
        +LocalDateTime fechaCreacion
        +LocalDateTime fechaActualizacion
    }

    class Usuario {
        +String nombreCompleto
        +String numeroDocumento
        +String correoElectronico
        +Rol rol
        +Boolean activo
        +Boolean emailVerificado
    }

    class Ambiente {
        +String nombre
        +String ubicacion
        +String direccion
        +Boolean activo
    }

    class Categoria {
        +String nombre
        +Boolean activo
    }

    class Marca {
        +String nombre
        +Boolean activo
    }

    class Equipo {
        +String nombre
        +String codigoUnico
        +String placa
        +String serial
        +String modelo
        +EstadoEquipo estado
        +TipoUsoEquipo tipoUso
        +Integer cantidadTotal
        +Integer cantidadDisponible
        +Integer umbralMinimo
        +Boolean activo
    }

    class FotoEquipo {
        +String nombreArchivo
        +String rutaArchivo
        +Long tamanoBytes
    }

    class ObservacionEquipo {
        +String observaciones
        +Integer estadoDevolucion
        +LocalDateTime fechaRegistro
    }

    EntidadBase <|-- Usuario
    EntidadBase <|-- Ambiente
    EntidadBase <|-- Categoria
    EntidadBase <|-- Marca
    EntidadBase <|-- Equipo
    EntidadBase <|-- FotoEquipo
    EntidadBase <|-- ObservacionEquipo

    Ambiente --> Usuario : instructorResponsable
    Ambiente --> Usuario : propietario
    Ambiente --> Ambiente : padre
    Equipo --> Categoria : categoria
    Equipo --> Marca : marca
    Equipo --> Ambiente : ambiente
    Equipo --> Ambiente : subUbicacion
    Equipo --> Usuario : propietario
    Equipo --> Usuario : inventarioActualInstructor
    Equipo *-- FotoEquipo : fotos
    ObservacionEquipo --> Equipo : equipo
```

### 03b Dominio operativo de préstamos y reservas

```mermaid
classDiagram
    direction TB

    class Prestamo {
        +EstadoPrestamo estado
        +LocalDateTime fechaHoraSolicitud
        +LocalDateTime fechaHoraAprobacion
        +LocalDateTime fechaHoraSalida
        +LocalDateTime fechaHoraDevolucionEstimada
        +LocalDateTime fechaHoraDevolucionReal
        +Integer extensionesRealizadas
    }

    class DetallePrestamo {
        +Integer cantidad
        +Boolean devuelto
        +EstadoCondicion estadoEquipoEntrega
        +EstadoCondicion estadoEquipoDevolucion
    }

    class ExtensionPrestamo {
        +EstadoExtension estado
        +LocalDateTime nuevaFechaDevolucion
        +String motivo
    }

    class ReporteDano {
        +String descripcion
        +String fotoRuta
        +LocalDateTime fechaReporte
    }

    class Reserva {
        +Integer cantidad
        +EstadoReserva estado
        +LocalDateTime fechaHoraInicio
        +LocalDateTime fechaHoraFin
    }

    class PrestamoAmbiente {
        +EstadoPrestamoAmbiente estado
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +LocalTime horaInicio
        +LocalTime horaFin
        +Integer numeroParticipantes
    }

    class Transferencia {
        +Integer cantidad
        +String motivo
        +LocalDateTime fechaTransferencia
    }

    class Mantenimiento {
        +TipoMantenimiento tipo
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +String responsable
    }

    class Notificacion {
        +TipoNotificacion tipo
        +MedioEnvio medioEnvio
        +EstadoEnvio estadoEnvio
        +Boolean leida
        +LocalDateTime fechaEnvio
    }

    class Configuracion {
        +String clave
        +String valor
    }

    Prestamo --> Usuario : usuarioSolicitante
    Prestamo --> Usuario : administradorAprueba
    Prestamo --> Usuario : administradorRecibe
    Prestamo --> Reserva : reservaOrigen
    Prestamo *-- DetallePrestamo : detalles
    Prestamo o-- ExtensionPrestamo : extensiones
    DetallePrestamo --> Equipo : equipo
    DetallePrestamo o-- ReporteDano : reporteDano
    ReporteDano --> Usuario : reportadoPor
    Reserva --> Usuario : usuario
    Reserva --> Equipo : equipo
    PrestamoAmbiente --> Ambiente : ambiente
    PrestamoAmbiente --> Usuario : solicitante
    PrestamoAmbiente --> Usuario : propietarioAmbiente
    Transferencia --> Equipo : equipo
    Transferencia --> Usuario : adminAutoriza
    Transferencia --> Usuario : inventarioOrigen
    Transferencia --> Usuario : inventarioDestino
    Transferencia --> Ambiente : ubicacionDestino
    Mantenimiento --> Equipo : equipo
    Notificacion --> Usuario : usuarioDestino
```

## 04a Flujo Prestamo

```mermaid
flowchart TD
    A[Usuario autenticado<br/>selecciona equipos] --> B[Frontend valida formulario<br/>cantidades y fechas]
    B --> C[POST /api/v1/prestamos]
    C --> D[PrestamoServicio inicia validaciones de negocio]
    D --> E{Stock disponible<br/>estado ACTIVO<br/>usuario habilitado}
    E -->|No| F[Retorna 400 o 409<br/>sin persistir cambios]
    E -->|Si| G[Persistir Prestamo y DetallePrestamo<br/>estado SOLICITADO]
    G --> H[Registrar LogAuditoria]
    H --> I[Notificacion al administrador responsable]
    I --> J{Administrador aprueba}
    J -->|No| K[Actualizar estado RECHAZADO<br/>notificar rechazo]
    J -->|Si| L[Actualizar estado APROBADO]
    L --> M[Registrar salida física<br/>estado ACTIVO]
    M --> N[Descontar cantidad_disponible<br/>en transacción]
    N --> O{Devolución dentro del plazo}
    O -->|No| P[Marcar EN_MORA<br/>emitir recordatorio]
    O -->|Sí| Q[Registrar devolución]
    P --> Q
    Q --> R[Reintegrar stock disponible]
    R --> S{Estado devuelto menor al esperado}
    S -->|Sí| T[Crear ObservacionEquipo y/o ReporteDano]
    S -->|No| U[Cerrar como DEVUELTO]
    T --> U
```

## 04b Flujo Autenticacion

```mermaid
flowchart TD
    A[Usuario abre login] --> B[Angular valida campos requeridos]
    B --> C[POST /api/v1/auth/login]
    C --> D[AuthControlador recibe credenciales]
    D --> E[AutenticacionServicio busca usuario por numeroDocumento]
    E --> F{Usuario activo existe}
    F -->|No| G[401 no autorizado]
    F -->|Sí| H{Cuenta bloqueada o pendiente aprobación}
    H -->|Sí| I[403 acceso denegado]
    H -->|No| J[Comparar contraseña con BCrypt]
    J --> K{Coincide hash}
    K -->|No| L[Incrementar intentosFallidos<br/>eventual bloqueo temporal]
    L --> M[401 credenciales inválidas]
    K -->|Sí| N{Requiere email verificado}
    N -->|Sí y no verificado| O[403 verificar correo]
    N -->|No o verificado| P[Generar JWT con rol y subject]
    P --> Q[SecurityContext y respuesta LoginRespuestaDTO]
    Q --> R[Frontend guarda token y navega al dashboard]
```

## 05 Casos de Uso

```mermaid
flowchart LR
    classDef actor fill:#e2e8f0,stroke:#334155,color:#0f172a
    classDef uc fill:#f8fafc,stroke:#64748b,color:#0f172a

    ADMIN[ADMINISTRADOR]:::actor
    INST[INSTRUCTOR]:::actor
    USER[APRENDIZ o FUNCIONARIO]:::actor
    ALIM[ALIMENTADOR_EQUIPOS]:::actor

    U1((Iniciar sesion)):::uc
    U2((Gestion inventario)):::uc
    U3((Solicitar prestamo)):::uc
    U4((Aprobar o rechazar prestamo)):::uc
    U5((Reservas)):::uc
    U6((Ambientes)):::uc
    U7((Transferencias)):::uc
    U8((Mantenimientos)):::uc
    U9((Reportes)):::uc
    U10((Usuarios y roles)):::uc

    ADMIN --> U1
    ADMIN --> U2
    ADMIN --> U3
    ADMIN --> U4
    ADMIN --> U5
    ADMIN --> U6
    ADMIN --> U7
    ADMIN --> U8
    ADMIN --> U9
    ADMIN --> U10

    INST --> U1
    INST --> U2
    INST --> U3
    INST --> U4
    INST --> U5
    INST --> U6
    INST --> U7
    INST --> U8
    INST --> U9

    USER --> U1
    USER --> U3
    USER --> U5

    ALIM --> U1
    ALIM --> U2
    ALIM --> U6
```

## 06a Secuencia Login

```mermaid
sequenceDiagram
    actor Usuario
    participant Frontend
    participant API
    participant AuthService
    participant DB

    Usuario->>Frontend: Ingresa credenciales
    Frontend->>API: POST /auth/login
    API->>AuthService: login()
    AuthService->>DB: Buscar usuario
    DB-->>AuthService: Usuario

    alt Credenciales validas
        AuthService->>AuthService: Generar JWT
        AuthService-->>API: Token
        API-->>Frontend: 200 OK
    else Credenciales invalidas
        API-->>Frontend: 401 o 403
    end
```

## 06b Secuencia Prestamo

```mermaid
sequenceDiagram
    actor Solicitante
    actor Administrador
    participant Frontend
    participant API
    participant PrestamoService
    participant DB

    Solicitante->>Frontend: Solicita prestamo
    Frontend->>API: POST /prestamos
    API->>PrestamoService: crearSolicitud()
    PrestamoService->>DB: Validar y guardar
    API-->>Frontend: 201 Created

    Administrador->>Frontend: Aprobar solicitud
    Frontend->>API: PATCH /prestamos/{id}/aprobar
    API->>PrestamoService: aprobar()
    PrestamoService->>DB: Actualizar estado
```

## 07 Componentes

```mermaid
flowchart LR
    classDef box fill:#f8fafc,stroke:#64748b,color:#0f172a
    classDef sec fill:#fee2e2,stroke:#b91c1c,color:#7f1d1d
    classDef data fill:#fef9c3,stroke:#a16207,color:#713f12

    subgraph FE[Frontend Angular 18]
        UI[Standalone components y páginas]:::box
        ROUTES[App routes + guards por rol]:::box
        HTTP[Core services + HttpClient]:::box
        INT[JWT interceptor + manejo global de errores]:::sec
    end

    subgraph BE[Backend Spring Boot]
        API[Controllers REST /api/v1]:::box
        AUTH[JwtFiltroAutenticacion + SecurityConfig + BCrypt]:::sec
        DOM[Servicios de dominio<br/>Usuario Equipo Prestamo Reserva Transferencia]:::box
        NOTI[NotificacionServicio + CorreoServicio]:::box
        REP[Reportes PDF/Excel + Dashboard queries]:::box
        PERSIST[JPA repositories + entidades]:::data
    end

    subgraph INFRA[Infraestructura]
        DB[(MariaDB 11.4)]:::data
        FLY[Flyway migrations]:::data
        UP[(Volumen uploads)]:::data
        SMTP[Servidor SMTP Gmail]:::box
    end

    UI --> ROUTES --> HTTP --> INT --> AUTH --> API
    API --> DOM
    API --> NOTI
    API --> REP
    DOM --> PERSIST --> DB
    PERSIST --> FLY
    DOM --> UP
    NOTI --> SMTP
```

## 08 Despliegue

```mermaid
flowchart TB
    classDef node fill:#f8fafc,stroke:#64748b,color:#0f172a
    classDef host fill:#dbeafe,stroke:#1d4ed8,color:#0f172a
    classDef vol fill:#fef9c3,stroke:#a16207,color:#713f12

    USER[Cliente web<br/>desktop o móvil]:::host

    subgraph HOST[Servidor Docker Compose]
        FE[sigea-frontend<br/>Nginx sirviendo Angular dist<br/>puerto 80]:::node
        BE[sigea-backend<br/>Spring Boot JAR<br/>puerto interno 8080]:::node
        DB[(sigea-db<br/>MariaDB 11.4<br/>puerto interno 3306)]:::node
        VDB[(volumen sigea-db-data)]:::vol
        VUP[(volumen sigea-uploads)]:::vol
    end

    SMTP[Proveedor SMTP Gmail<br/>salida segura SSL/TLS]:::node

    USER --> FE
    FE -->|proxy /api| BE
    BE -->|JPA JDBC| DB
    DB --> VDB
    BE --> VUP
    BE --> SMTP
```
