# SIGEA - Fuentes Mermaid

## 01 Arquitectura

```mermaid
flowchart TB
    classDef fe fill:#e2e8f0,stroke:#334155,color:#0f172a
    classDef be fill:#dcfce7,stroke:#166534,color:#14532d
    classDef db fill:#fef9c3,stroke:#a16207,color:#713f12
    classDef ex fill:#ede9fe,stroke:#5b21b6,color:#4c1d95

    subgraph FRONTEND[Frontend Angular 17]
        FE_PAGES[Pages and Components]:::fe
        FE_SERV[Core Services]:::fe
        FE_GUARD[Guards and JWT Interceptor]:::fe
    end

    subgraph BACKEND[Backend Spring Boot 3 Java 21]
        BE_SEC[Security and JWT Filter]:::be
        BE_CTRL[REST Controllers /api/v1]:::be
        BE_SVC[Business Services]:::be
        BE_REPO[JPA Repositories]:::be
    end

    subgraph DATA[Persistence]
        DB[(MariaDB 11)]:::db
        FS[(Uploads Storage)]:::db
    end

    subgraph EXT[External Services]
        SMTP[SMTP]:::ex
        WA[WhatsApp API]:::ex
    end

    FE_PAGES --> FE_SERV --> FE_GUARD --> BE_SEC
    BE_SEC --> BE_CTRL --> BE_SVC --> BE_REPO --> DB
    BE_SVC --> FS
    BE_SVC --> SMTP
    BE_SVC --> WA
```

## 02 MER

```mermaid
erDiagram
    USUARIO {
        bigint id PK
        varchar nombre_completo
        varchar tipo_documento
        varchar numero_documento UK
        varchar correo_electronico UK
        varchar rol
        boolean activo
    }
    CATEGORIA { bigint id PK varchar nombre UK }
    MARCA { bigint id PK varchar nombre UK }
    AMBIENTE { bigint id PK varchar nombre UK bigint propietario_id FK bigint padre_id FK }
    EQUIPO {
        bigint id PK
        varchar nombre
        varchar placa UK
        bigint categoria_id FK
        bigint marca_id FK
        bigint ambiente_id FK
        bigint propietario_id FK
        int cantidad_total
        int cantidad_disponible
    }
    FOTO_EQUIPO { bigint id PK bigint equipo_id FK }
    PRESTAMO { bigint id PK bigint usuario_solicitante_id FK bigint administrador_aprueba_id FK bigint administrador_recibe_id FK bigint reserva_id FK varchar estado }
    DETALLE_PRESTAMO { bigint id PK bigint prestamo_id FK bigint equipo_id FK int cantidad boolean devuelto }
    EXTENSION_PRESTAMO { bigint id PK bigint prestamo_id FK bigint administrador_aprueba_id FK varchar estado }
    REPORTE_DANO { bigint id PK bigint detalle_prestamo_id FK bigint reportado_por_id FK }
    RESERVA { bigint id PK bigint usuario_id FK bigint equipo_id FK int cantidad varchar estado }
    TRANSFERENCIA { bigint id PK bigint equipo_id FK bigint inventario_origen_instructor_id FK bigint inventario_destino_instructor_id FK bigint propietario_equipo_id FK bigint ubicacion_destino_id FK bigint administrador_autoriza_id FK }
    MANTENIMIENTO { bigint id PK bigint equipo_id FK varchar tipo }
    OBSERVACION_EQUIPO { bigint id PK bigint equipo_id FK bigint autor_id FK }
    PRESTAMO_AMBIENTE { bigint id PK bigint ambiente_id FK bigint solicitante_id FK bigint propietario_ambiente_id FK varchar estado }
    NOTIFICACION { bigint id PK bigint usuario_destino_id FK varchar tipo varchar estado_envio }
    LOG_AUDITORIA { bigint id PK bigint usuario_id FK varchar accion }
    CONFIGURACION { bigint id PK varchar clave UK }

    USUARIO ||--o{ AMBIENTE : propietario
    AMBIENTE ||--o{ AMBIENTE : sububicacion
    CATEGORIA ||--o{ EQUIPO : clasifica
    MARCA ||--o{ EQUIPO : marca
    AMBIENTE ||--o{ EQUIPO : ubica
    USUARIO ||--o{ EQUIPO : propietario
    EQUIPO ||--o{ FOTO_EQUIPO : fotos
    USUARIO ||--o{ PRESTAMO : solicita
    PRESTAMO ||--|{ DETALLE_PRESTAMO : detalle
    EQUIPO ||--o{ DETALLE_PRESTAMO : item
    PRESTAMO ||--o{ EXTENSION_PRESTAMO : extension
    DETALLE_PRESTAMO ||--o| REPORTE_DANO : dano
    USUARIO ||--o{ RESERVA : reserva
    EQUIPO ||--o{ RESERVA : reservado
    RESERVA ||--o| PRESTAMO : origen
    EQUIPO ||--o{ TRANSFERENCIA : transfiere
    EQUIPO ||--o{ MANTENIMIENTO : mantenimiento
    EQUIPO ||--o{ OBSERVACION_EQUIPO : observacion
    AMBIENTE ||--o{ PRESTAMO_AMBIENTE : uso
    USUARIO ||--o{ NOTIFICACION : recibe
    USUARIO ||--o{ LOG_AUDITORIA : audita
```

## 03 Clases UML

```mermaid
classDiagram
    class EntidadBase { +id: Long +fechaCreacion: LocalDateTime +fechaActualizacion: LocalDateTime }
    class Usuario { +nombreCompleto: String +correoElectronico: String +rol: Rol +activo: boolean }
    class Ambiente { +nombre: String +ubicacion: String +activo: boolean }
    class Categoria { +nombre: String +activo: boolean }
    class Marca { +nombre: String +activo: boolean }
    class Equipo { +nombre: String +placa: String +estado: EstadoEquipo +cantidadTotal: int +cantidadDisponible: int }
    class FotoEquipo { +nombreArchivo: String +rutaArchivo: String }
    class Prestamo { +estado: EstadoPrestamo +fechaSolicitud: LocalDateTime }
    class DetallePrestamo { +cantidad: int +devuelto: boolean }
    class ExtensionPrestamo { +estado: EstadoExtension }
    class ReporteDano { +descripcion: String }
    class Reserva { +cantidad: int +estado: EstadoReserva }
    class Transferencia { +cantidad: int +motivo: String }
    class Mantenimiento { +tipo: TipoMantenimiento }
    class ObservacionEquipo { +texto: String }
    class PrestamoAmbiente { +estado: EstadoPrestamoAmbiente }
    class Notificacion { +tipo: TipoNotificacion +estadoEnvio: EstadoEnvio }
    class Configuracion { +clave: String +valor: String }

    EntidadBase <|-- Usuario
    EntidadBase <|-- Ambiente
    EntidadBase <|-- Categoria
    EntidadBase <|-- Marca
    EntidadBase <|-- Equipo
    EntidadBase <|-- FotoEquipo
    EntidadBase <|-- Prestamo
    EntidadBase <|-- DetallePrestamo
    EntidadBase <|-- ExtensionPrestamo
    EntidadBase <|-- ReporteDano
    EntidadBase <|-- Reserva
    EntidadBase <|-- Transferencia
    EntidadBase <|-- Mantenimiento
    EntidadBase <|-- ObservacionEquipo
    EntidadBase <|-- PrestamoAmbiente
    EntidadBase <|-- Notificacion
    EntidadBase <|-- Configuracion

    Equipo --> Categoria : categoria
    Equipo --> Ambiente : ambiente
    Equipo --> Marca : marca
    Equipo --> Usuario : propietario
    Equipo *-- FotoEquipo : fotos
    Ambiente --> Usuario : propietario
    Ambiente --> Ambiente : padre
    Prestamo --> Usuario : solicitante
    Prestamo --> Usuario : adminAprueba
    Prestamo --> Usuario : adminRecibe
    Prestamo *-- DetallePrestamo : detalles
    Prestamo o-- ExtensionPrestamo : extensiones
    DetallePrestamo --> Equipo : equipo
    DetallePrestamo o-- ReporteDano : dano
    Reserva --> Usuario : usuario
    Reserva --> Equipo : equipo
    Transferencia --> Equipo : equipo
    Mantenimiento --> Equipo : equipo
    ObservacionEquipo --> Equipo : equipo
    ObservacionEquipo --> Usuario : autor
    PrestamoAmbiente --> Ambiente : ambiente
    PrestamoAmbiente --> Usuario : solicitante
    PrestamoAmbiente --> Usuario : propietario
    Notificacion --> Usuario : destino
```

## 04a Flujo Prestamo

```mermaid
flowchart TD
    A[Inicio solicitud] --> B[Seleccion de equipos]
    B --> C{Stock suficiente}
    C -->|No| D[Rechazo por stock]
    C -->|Si| E[Crear prestamo pendiente]
    E --> F{Aprobacion admin}
    F -->|No| G[Prestamo rechazado]
    F -->|Si| H[Registrar salida y descontar stock]
    H --> I[Registrar devolucion y reponer stock]
    I --> J{Hay dano}
    J -->|Si| K[Crear reporte dano]
    J -->|No| L[Cerrar prestamo devuelto]
    K --> L
```

## 04b Flujo Autenticacion

```mermaid
flowchart TD
    A[Inicio login] --> B[Validar formulario]
    B --> C{Valido}
    C -->|No| D[Mostrar errores]
    C -->|Si| E[POST auth login]
    E --> F{Usuario existe}
    F -->|No| G[401]
    F -->|Si| H{Email verificado}
    H -->|No| I[403 verificar email]
    H -->|Si| J{Contrasena correcta}
    J -->|No| K[Incrementar intentos]
    J -->|Si| L[Generar JWT y sesion]
    L --> M[Dashboard]
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
flowchart TB
    classDef box fill:#f8fafc,stroke:#64748b,color:#0f172a

    subgraph FE[Angular Frontend]
        FE1[Routing and Guards]:::box
        FE2[Pages]:::box
        FE3[Core Services]:::box
    end

    subgraph BE[Spring Boot Backend]
        BE1[Security and JWT]:::box
        BE2[Controllers]:::box
        BE3[Services]:::box
        BE4[Repositories]:::box
    end

    subgraph ST[Storage]
        DB[(MariaDB)]:::box
        FS[(Uploads)]:::box
    end

    FE1 --> FE2 --> FE3 --> BE1 --> BE2 --> BE3 --> BE4 --> DB
    BE3 --> FS
```

## 08 Despliegue

```mermaid
flowchart TB
    classDef node fill:#f8fafc,stroke:#64748b,color:#0f172a

    C[Cliente navegador]:::node
    N[Nginx reverse proxy]:::node
    A[Angular dist]:::node
    B[Spring Boot JAR port 8080]:::node
    D[(MariaDB port 3306)]:::node
    U[(Uploads)]:::node
    S[SMTP]:::node
    W[WhatsApp API]:::node

    C --> N
    N --> A
    N --> B
    B --> D
    B --> U
    B --> S
    B --> W
```
