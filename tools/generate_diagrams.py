#!/usr/bin/env python3
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).parent.parent
DOCS = ROOT / "docs"
TOOLS = ROOT / "tools"
DOCS.mkdir(exist_ok=True)

MERMAID_CDN = "https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"
MERMAID_CACHE = TOOLS / "_mermaid.min.js"

EDGE_PATHS = [
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
]


def get_mermaid_tag() -> str:
    if MERMAID_CACHE.exists():
        js = MERMAID_CACHE.read_text(encoding="utf-8", errors="replace")
        return f"<script>\n{js}\n</script>"
    try:
        with urllib.request.urlopen(MERMAID_CDN, timeout=45) as res:
            js = res.read().decode("utf-8", errors="replace")
        MERMAID_CACHE.write_text(js, encoding="utf-8")
        return f"<script>\n{js}\n</script>"
    except Exception:
        return f'<script src="{MERMAID_CDN}"></script>'


D1_ARQ = r"""flowchart TB
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
"""

D2_MER = r"""erDiagram
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
"""

D3_CLASS = r"""classDiagram
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
"""

D4_FLOW_PRE = r"""flowchart TD
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
"""

D4_FLOW_AUTH = r"""flowchart TD
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
"""

D5_USE = r"""flowchart LR
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
"""

D6_SEQ_LOGIN = r"""sequenceDiagram
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
"""

D6_SEQ_PRE = r"""sequenceDiagram
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
"""

D7_COMP = r"""flowchart TB
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
"""

D8_DEPLOY = r"""flowchart TB
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
"""

SECTIONS = [
    ("01", "Arquitectura del Sistema", "Vista general de capas y comunicacion.", [D1_ARQ]),
    ("02", "Modelo Entidad Relacion", "Entidades principales y relaciones.", [D2_MER]),
    ("03", "Diagrama de Clases UML", "Modelo de clases del dominio.", [D3_CLASS]),
    ("04", "Diagramas de Flujo", "Procesos de prestamo y autenticacion.", [D4_FLOW_PRE, D4_FLOW_AUTH]),
    ("05", "Casos de Uso", "Actores y funcionalidades por rol.", [D5_USE]),
    ("06", "Diagramas de Secuencia", "Secuencias de login y prestamo.", [D6_SEQ_LOGIN, D6_SEQ_PRE]),
    ("07", "Diagrama de Componentes", "Componentes y dependencias.", [D7_COMP]),
    ("08", "Diagrama de Despliegue", "Infraestructura de ejecucion.", [D8_DEPLOY]),
]


def build_toc() -> str:
    return "\n".join([
        f'<li><a href="#section-{n}"><span class="toc-num">{n}</span>{t}</a></li>'
        for (n, t, _, _) in SECTIONS
    ])


def build_sections() -> str:
    parts: list[str] = []
    for n, title, desc, diagrams in SECTIONS:
        dhtml = []
        for i, code in enumerate(diagrams, 1):
            dhtml.append(
                f"<div class=\"diagram-block\"><h3>{title} #{i}</h3><div class=\"diagram-shell\"><div class=\"mermaid\">{code}</div></div></div>"
            )
        parts.append(
            f"<section class=\"section\" id=\"section-{n}\"><header><div class=\"badge\">{n}</div><h2>{title}</h2></header><p>{desc}</p>{''.join(dhtml)}</section>"
        )
    return "\n".join(parts)


def build_html(mermaid_tag: str) -> str:
    return f"""<!doctype html>
<html lang=\"es\">
<head>
  <meta charset=\"utf-8\" />
  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
  <title>SIGEA - Diagramas</title>
  <link rel=\"preconnect\" href=\"https://fonts.googleapis.com\" />
  <link rel=\"preconnect\" href=\"https://fonts.gstatic.com\" crossorigin />
  <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap\" rel=\"stylesheet\" />
  {mermaid_tag}
  <script>
    mermaid.initialize({{
      startOnLoad: true,
      theme: 'base',
      securityLevel: 'loose',
      fontFamily: 'Inter, Segoe UI, Arial, sans-serif',
      themeVariables: {{
        primaryColor: '#f8fafc',
        primaryBorderColor: '#64748b',
        primaryTextColor: '#0f172a',
        lineColor: '#475569',
        noteBkgColor: '#f8fafc',
        noteBorderColor: '#94a3b8'
      }},
      flowchart: {{ useMaxWidth: true, curve: 'linear' }},
      sequence: {{ useMaxWidth: true }},
      er: {{ useMaxWidth: true }},
      class: {{ useMaxWidth: true }}
    }});
  </script>
  <style>
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    :root {{ --brand:#006e2d; --brand2:#004d1e; --ink:#0f172a; --muted:#475569; --border:#cbd5e1; --bg:#f8fafc; --card:#ffffff; }}
    @page {{
      size: A4 landscape;
      margin: 12mm 12mm 14mm 12mm;
      @top-left {{ content: 'SIGEA SENA'; font-family: Inter, sans-serif; font-size: 8pt; color: #006e2d; }}
      @top-center {{ content: 'Documentacion de Arquitectura y Diagramas'; font-family: Inter, sans-serif; font-size: 8pt; color: #64748b; }}
      @bottom-center {{ content: 'Pagina ' counter(page) ' de ' counter(pages); font-family: Inter, sans-serif; font-size: 8pt; color: #64748b; }}
    }}
    @page :first {{ @top-left {{ content:none; }} @top-center {{ content:none; }} @bottom-center {{ content:none; }} }}
    body {{ font-family: Inter, Segoe UI, Arial, sans-serif; color: var(--ink); background: var(--bg); line-height: 1.45; font-size: 14px; }}
    .cover {{ min-height: 100vh; page-break-after: always; padding: 48px 56px; background: linear-gradient(145deg,#003814 0%,#005e26 42%,#007d33 100%); color:#fff; display:grid; grid-template-rows:auto 1fr auto; gap:24px; }}
    .cover h1 {{ font-size:56px; font-weight:800; letter-spacing:-0.02em; }}
    .cover h2 {{ font-size:28px; font-weight:600; margin-top:8px; color:#b7f0c5; }}
    .cover p {{ max-width:760px; color:#e2efe5; margin-top:16px; }}
    .cover-meta {{ font-size:12px; color:#d8ebe0; }}
    .layout {{ max-width:1400px; margin:0 auto; padding:24px; }}
    .toc {{ background:var(--card); border:1px solid var(--border); border-radius:12px; margin-bottom:22px; overflow:hidden; }}
    .toc header {{ padding:16px 20px; background:#f1f5f9; border-bottom:1px solid var(--border); }}
    .toc h2 {{ font-size:18px; color:var(--brand2); }}
    .toc ul {{ list-style:none; display:grid; grid-template-columns:1fr 1fr; gap:6px; padding:12px; }}
    .toc a {{ text-decoration:none; color:var(--ink); padding:8px 10px; border-radius:8px; display:flex; align-items:center; gap:10px; }}
    .toc a:hover {{ background:#f1f5f9; }}
    .toc-num {{ width:26px; height:26px; border-radius:6px; background:var(--brand); color:#fff; display:inline-flex; align-items:center; justify-content:center; font-size:11px; font-weight:700; }}
    .section {{ background:var(--card); border:1px solid var(--border); border-radius:12px; margin-bottom:20px; overflow:hidden; break-inside:avoid; page-break-inside:avoid; }}
    .section > header {{ display:flex; align-items:center; gap:12px; padding:14px 18px; border-bottom:1px solid var(--border); background:#f8fafc; }}
    .section .badge {{ width:34px; height:34px; border-radius:8px; background:var(--brand); color:#fff; display:inline-flex; align-items:center; justify-content:center; font-weight:700; }}
    .section h2 {{ font-size:20px; color:var(--brand2); }}
    .section > p {{ padding:12px 18px; color:var(--muted); border-bottom:1px solid var(--border); }}
    .diagram-block {{ padding:14px 16px; }}
    .diagram-block h3 {{ font-size:14px; margin-bottom:10px; color:#1e293b; }}
    .diagram-shell {{ border:1px solid var(--border); border-radius:10px; background:#fff; padding:12px; overflow-x:auto; }}
    .diagram-shell svg {{ max-width:100%; height:auto !important; }}
    @media print {{ body {{ background:#fff; }} .layout {{ padding:0; }} .section {{ box-shadow:none; }} }}
  </style>
</head>
<body>
  <section class=\"cover\">
    <div><p>Servicio Nacional de Aprendizaje - SENA</p></div>
    <div>
      <h1>SIGEA</h1>
      <h2>Documentacion de Arquitectura y Diagramas</h2>
      <p>Documento tecnico integral para comprension funcional, estructural y de despliegue del sistema.</p>
    </div>
    <div class=\"cover-meta\">
      <div>Stack backend: Java 21, Spring Boot, MariaDB, Flyway, JWT</div>
      <div>Stack frontend: Angular, TypeScript</div>
    </div>
  </section>
  <main class=\"layout\">
    <section class=\"toc\"><header><h2>Indice</h2></header><ul>{build_toc()}</ul></section>
    {build_sections()}
  </main>
</body>
</html>"""


def build_sources_md() -> str:
    sources = [
        ("01 Arquitectura", D1_ARQ),
        ("02 MER", D2_MER),
        ("03 Clases UML", D3_CLASS),
        ("04a Flujo Prestamo", D4_FLOW_PRE),
        ("04b Flujo Autenticacion", D4_FLOW_AUTH),
        ("05 Casos de Uso", D5_USE),
        ("06a Secuencia Login", D6_SEQ_LOGIN),
        ("06b Secuencia Prestamo", D6_SEQ_PRE),
        ("07 Componentes", D7_COMP),
        ("08 Despliegue", D8_DEPLOY),
    ]
    out = ["# SIGEA - Fuentes Mermaid", ""]
    for title, code in sources:
        out += [f"## {title}", "", "```mermaid", code.strip(), "```", ""]
    return "\n".join(out)


def find_edge() -> str | None:
    for p in EDGE_PATHS:
        if Path(p).exists():
            return p
    return None


def export_pdf(html_path: Path, pdf_path: Path) -> bool:
    edge = find_edge()
    if not edge:
        return False
    file_url = f"file:///{html_path.resolve().as_posix()}"
    cmd = [
        edge,
        "--headless=new",
        "--disable-gpu",
        "--no-sandbox",
        "--run-all-compositor-stages-before-draw",
        "--virtual-time-budget=7000",
        f"--print-to-pdf={pdf_path}",
        "--print-to-pdf-no-header",
        file_url,
    ]
    try:
        subprocess.run(cmd, check=False, capture_output=True, text=True, timeout=120)
        return pdf_path.exists() and pdf_path.stat().st_size > 10000
    except Exception:
        return False


def main() -> None:
    html_path = DOCS / "sigea-diagramas.html"
    html_path.write_text(build_html(get_mermaid_tag()), encoding="utf-8")
    print(f"[ok] HTML: {html_path}")

    md_path = DOCS / "sigea-diagramas-fuentes.md"
    md_path.write_text(build_sources_md(), encoding="utf-8")
    print(f"[ok] Mermaid: {md_path}")

    pdf_path = DOCS / "sigea-diagramas.pdf"
    if export_pdf(html_path, pdf_path):
        print(f"[ok] PDF: {pdf_path} ({pdf_path.stat().st_size:,} bytes)")
    else:
        print("[warn] PDF no generado automaticamente")


if __name__ == "__main__":
    main()
