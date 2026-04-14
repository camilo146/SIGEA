from __future__ import annotations

import json
import re
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date
from html import escape
from pathlib import Path
from typing import Iterable

import markdown


ROOT = Path(__file__).resolve().parents[1]
BACKEND = ROOT / "sigea-backend"
FRONTEND = ROOT / "sigea-frontend"
OUTPUT_DIR = ROOT / "docs"
MD_OUTPUT = OUTPUT_DIR / "sigea-documentacion-tecnica.md"
HTML_OUTPUT = OUTPUT_DIR / "sigea-documentacion-tecnica.html"
API_BASE_URL = "https://api.sigea.com"
ENV_EXAMPLE_PATH = ROOT / ".env.example"
COMPOSE_PATH = ROOT / "docker-compose.yml"
NGINX_LOCAL_PATH = ROOT / "docker" / "nginx.local.conf"

BACKEND_EXCLUDES = {"target", "uploads"}
FRONTEND_EXCLUDES = {"node_modules", ".angular", "dist"}

BACKEND_EXTS = {".java", ".properties", ".sql", ".xml", ".md"}
FRONTEND_EXTS = {".ts", ".html", ".scss", ".json", ".md"}
SPECIAL_NAMES = {"Dockerfile", "mvnw", "mvnw.cmd", "nginx.conf", ".editorconfig"}

JAVA_KEYWORDS = {"if", "for", "while", "switch", "catch", "return", "new", "throw", "else", "do", "try"}
TS_KEYWORDS = {"if", "for", "while", "switch", "catch", "constructor", "return", "new"}


@dataclass
class ParamDoc:
    name: str
    type: str
    optional: bool = False


@dataclass
class MemberDoc:
    kind: str
    name: str
    return_type: str = "void"
    params: list[ParamDoc] = field(default_factory=list)
    annotations: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    purpose: str = ""
    side_effects: list[str] = field(default_factory=list)
    example: str = ""
    notes: str = ""


@dataclass
class ExportDoc:
    kind: str
    name: str
    description: str
    fields: list[ParamDoc] = field(default_factory=list)
    values: list[str] = field(default_factory=list)
    members: list[MemberDoc] = field(default_factory=list)


@dataclass
class FileDoc:
    path: Path
    category: str
    description: str
    dependencies: list[str] = field(default_factory=list)
    exports: list[ExportDoc] = field(default_factory=list)
    warnings: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


@dataclass
class DtoField:
    name: str
    type: str
    validations: list[str] = field(default_factory=list)
    description: str = ""


@dataclass
class DtoDoc:
    name: str
    fields: list[DtoField] = field(default_factory=list)


@dataclass
class EndpointDoc:
    domain: str
    controller: str
    method: str
    path: str
    full_url: str
    description: str
    roles: str
    path_params: list[ParamDoc] = field(default_factory=list)
    query_params: list[ParamDoc] = field(default_factory=list)
    request_body_type: str | None = None
    request_body_mode: str | None = None
    response_type: str | None = None
    status_code: int = 200
    validations: dict[str, list[str]] = field(default_factory=dict)
    service_errors: list[str] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def collect_files() -> tuple[list[Path], list[Path]]:
    backend_files: list[Path] = []
    frontend_files: list[Path] = []

    for path in BACKEND.rglob("*"):
        if not path.is_file():
            continue
        parts = set(path.relative_to(BACKEND).parts)
        if parts & BACKEND_EXCLUDES:
            continue
        if path.suffix in BACKEND_EXTS or path.name in SPECIAL_NAMES:
            backend_files.append(path)

    for path in FRONTEND.rglob("*"):
        if not path.is_file():
            continue
        parts = set(path.relative_to(FRONTEND).parts)
        if parts & FRONTEND_EXCLUDES:
            continue
        if path.suffix in FRONTEND_EXTS or path.name in SPECIAL_NAMES:
            frontend_files.append(path)

    return sorted(backend_files), sorted(frontend_files)


def collect_controller_files() -> list[Path]:
    return sorted(BACKEND.glob("src/main/java/co/edu/sena/sigea/*/controller/*Controlador.java"))


def collect_java_files() -> list[Path]:
    return sorted(BACKEND.glob("src/main/java/co/edu/sena/sigea/**/*.java"))


def summarize_dependencies(imports: Iterable[str], file_path: str) -> list[str]:
    groups: dict[str, list[str]] = defaultdict(list)
    for item in imports:
        if item.startswith("org.springframework"):
            groups["Spring"].append(item)
        elif item.startswith("jakarta"):
            groups["Jakarta"].append(item)
        elif item.startswith("java."):
            groups["Java SE"].append(item)
        elif item.startswith("co.edu.sena.sigea"):
            groups["SIGEA interno"].append(item)
        elif item.startswith("@angular"):
            groups["Angular"].append(item)
        elif item.startswith("rxjs"):
            groups["RxJS"].append(item)
        else:
            groups["Otros"].append(item)

    reasons: list[str] = []
    for group, values in groups.items():
        sample = ", ".join(short_import(v) for v in values[:5])
        if group == "Spring":
            reasons.append(f"{group}: soporte de web, seguridad, DI o persistencia mediante {sample}.")
        elif group == "Angular":
            reasons.append(f"{group}: bootstrap, componentes, router o HttpClient mediante {sample}.")
        elif group == "RxJS":
            reasons.append(f"{group}: manejo reactivo de flujos HTTP y estado asincrono con {sample}.")
        elif group == "SIGEA interno":
            reasons.append(f"{group}: reutiliza contratos o servicios del propio dominio ({sample}).")
        elif group == "Jakarta":
            reasons.append(f"{group}: validaciones y contratos web/JPA ({sample}).")
        elif group == "Java SE":
            reasons.append(f"{group}: estructuras base y utilidades de lenguaje ({sample}).")
        else:
            reasons.append(f"{group}: dependencias auxiliares ({sample}).")

    if not reasons and file_path.endswith((".html", ".scss", ".sql")):
        reasons.append("Sin imports explicitos; depende del componente, del motor CSS o del motor SQL que la consume.")
    return reasons


def short_import(value: str) -> str:
    return value.split(".")[-1].strip("{} ")


def infer_domain_description(path: str) -> str:
    table = {
        "ambiente": "Gestiona ambientes, sububicaciones e inventario fisico asociado.",
        "auditoria": "Centraliza trazabilidad y consulta de eventos auditables.",
        "categoria": "Administra catalogos de categorias de equipos.",
        "common": "Agrupa infraestructura compartida: enums, excepciones, DTOs y utilidades.",
        "configuracion": "Declara configuracion transversal y tareas programadas.",
        "dashboard": "Expone estadisticas para el tablero principal.",
        "equipo": "Modela inventario, fotos, estados y disponibilidad de equipos.",
        "evaluacion": "Ejecuta procesos de evaluacion periodica o tareas automaticas.",
        "mantenimiento": "Registra mantenimientos preventivos/correctivos de equipos.",
        "marca": "Administra marcas asociadas a equipos.",
        "notificacion": "Gestiona notificaciones internas, correo y WhatsApp.",
        "observacion": "Adjunta observaciones historicas a equipos.",
        "prestamo": "Gestiona el ciclo de vida de prestamos de equipos.",
        "prestamoambiente": "Gestiona prestamos o reservas de ambientes.",
        "reporte": "Genera reportes exportables en Excel y PDF.",
        "reserva": "Gestiona reservas anticipadas de equipos.",
        "seguridad": "Resuelve autenticacion, autorizacion y JWT.",
        "transferencia": "Registra traspasos de equipos entre responsables o ubicaciones.",
        "usuario": "Gestiona usuarios, roles, aprobaciones y credenciales.",
        "core": "Contiene guardas, servicios, modelos e interceptor compartidos del frontend.",
        "layout": "Implementa el layout principal y la navegacion base.",
        "pages": "Agrupa pantallas funcionales cargadas por rutas.",
        "shared": "Componentes y utilidades reutilizables del frontend.",
        "environments": "Define configuracion de build y base URL de API.",
    }
    for key, description in table.items():
        if f"/{key}/" in path:
            return description
    return "Describe una pieza de configuracion o soporte del proyecto."


def camel_to_label(value: str) -> str:
    spaced = re.sub(r"([a-z0-9])([A-Z])", r"\1 \2", value)
    return spaced.replace("_", " ").strip().capitalize()


def describe_param(name: str) -> str:
    table = {
        "id": "Identificador unico del recurso.",
        "nombre": "Nombre visible del recurso.",
        "descripcion": "Descripcion funcional o detalle del recurso.",
        "ubicacion": "Ubicacion fisica o logica asociada al recurso.",
        "direccion": "Direccion o referencia de localizacion adicional.",
        "numeroDocumento": "Numero de documento de la persona usuaria.",
        "tipoDocumento": "Tipo de documento permitido por el sistema.",
        "correoElectronico": "Correo electronico del usuario.",
        "contrasena": "Contrasena en texto plano antes de ser cifrada.",
        "nuevaContrasena": "Nueva contrasena que reemplazara la actual.",
        "contrasenaActual": "Contrasena vigente del usuario autenticado.",
        "telefono": "Telefono de contacto.",
        "programaFormacion": "Programa de formacion asociado al usuario.",
        "numeroFicha": "Numero de ficha academica o grupo.",
        "rol": "Rol funcional que define permisos del usuario.",
        "nombreCompleto": "Nombre completo de la persona.",
        "codigo": "Codigo de verificacion o recuperacion.",
        "placa": "Placa o identificador patrimonial SENA.",
        "serial": "Serial del fabricante.",
        "modelo": "Modelo comercial o tecnico del equipo.",
        "marcaId": "Identificador de la marca seleccionada.",
        "categoriaId": "Identificador de la categoria.",
        "ambienteId": "Identificador del ambiente consultado.",
        "subUbicacionId": "Identificador de la sububicacion dentro del ambiente.",
        "cantidadTotal": "Cantidad total registrada para el equipo.",
        "tipoUso": "Indica si el equipo es consumible o no consumible.",
        "umbralMinimo": "Cantidad minima para alertar stock bajo.",
        "codigoUnico": "Codigo unico interno del equipo.",
        "estadoEquipoEscala": "Calificacion del estado fisico del equipo en escala numerica.",
        "padreId": "Identificador del ambiente o recurso padre.",
        "idInstructorResponsable": "Instructor responsable del ambiente o ubicacion.",
        "padreId": "Identificador del ambiente o recurso padre.",
        "instructorId": "Identificador del instructor asociado.",
        "usuarioId": "Identificador del usuario objetivo.",
        "equipoId": "Identificador del equipo consultado.",
        "ambienteId": "Identificador del ambiente consultado.",
        "fotoId": "Identificador de la foto asociada.",
        "categoriaId": "Identificador de la categoria.",
        "subUbicacionId": "Identificador de la sububicacion dentro del ambiente.",
        "nuevoEstado": "Nuevo estado del equipo o recurso.",
        "estado": "Estado usado como filtro o ruta enum.",
        "rol": "Rol a consultar o asignar.",
        "tipo": "Tipo usado como filtro del dominio.",
        "archivo": "Archivo binario cargado por multipart/form-data.",
        "formato": "Formato de salida solicitado.",
        "desde": "Fecha/hora inicial del rango en formato ISO.",
        "hasta": "Fecha/hora final del rango en formato ISO.",
        "suiteCompleta": "Indica si se envia la suite completa de correos de prueba.",
        "motivo": "Motivo textual de rechazo, cancelacion o cambio.",
    }
    return table.get(name, f"{camel_to_label(name)} del endpoint.")


def infer_file_description(path: str) -> str:
    name = Path(path).name
    lowered = path.lower()
    if name == "pom.xml":
        return "Define el build Maven, plugins y dependencias del backend." 
    if name == "package.json":
        return "Declara dependencias npm y scripts operativos del frontend."
    if name == "angular.json":
        return "Configura el workspace Angular, builds, budgets y assets."
    if name == "app.routes.ts":
        return "Declara la navegacion principal, lazy loading y guards por ruta."
    if name == "app.config.ts":
        return "Bootstrap de proveedores globales de Angular: router, HttpClient e interceptor JWT."
    if name == "main.ts":
        return "Punto de entrada del frontend que inicia la aplicacion standalone."
    if name == "SigeaBackendApplication.java":
        return "Punto de entrada del backend Spring Boot y habilitacion de tareas programadas."
    if name.startswith("application") and name.endswith(".properties"):
        return "Configura entorno, base de datos, seguridad, correo, uploads y observabilidad del backend."
    if "/controller/" in lowered:
        return f"Expone endpoints REST del dominio. {infer_domain_description(path)}"
    if "/service/" in lowered and path.endswith(".java"):
        return f"Implementa reglas de negocio y orquestacion del dominio. {infer_domain_description(path)}"
    if "/repository/" in lowered:
        return f"Encapsula acceso a datos del dominio mediante Spring Data/JPA. {infer_domain_description(path)}"
    if "/entity/" in lowered:
        return f"Modela datos persistentes del dominio. {infer_domain_description(path)}"
    if "/dto/" in lowered:
        return f"Define contratos de entrada/salida del dominio. {infer_domain_description(path)}"
    if "/jwt/" in lowered:
        return "Implementa generacion o validacion de tokens JWT y su integracion con Spring Security."
    if "/config/" in lowered:
        return "Declara configuracion transversal del backend o frontend."
    if "/guards/" in lowered:
        return "Restringe navegacion frontend en funcion de autenticacion o rol activo."
    if "/interceptors/" in lowered:
        return "Intercepta solicitudes HTTP del frontend para adjuntar JWT o centralizar errores."
    if "/core/services/" in lowered:
        return "Encapsula llamadas HTTP y reglas cliente del dominio en el frontend."
    if "/models/" in lowered:
        return "Declara interfaces y tipos usados por servicios y componentes del frontend."
    if "/layout/" in lowered and path.endswith(".ts"):
        return "Controla el shell principal del frontend: sidebar, navbar, router-outlet y utilidades de sesion."
    if "/pages/" in lowered and path.endswith(".ts"):
        return f"Implementa una pantalla funcional del frontend. {infer_domain_description(path)}"
    if path.endswith(".html"):
        return "Template asociado a un componente Angular; define estructura visual y bindings."
    if path.endswith(".scss"):
        return "Hoja de estilos del componente o layout asociado."
    if path.endswith(".sql") and "/db/migration/" in lowered:
        return "Migracion Flyway versionada; cambia estructura o datos base de la base de datos."
    if path.endswith(".sql") and "/scripts/" in lowered:
        return "Script operativo/manual para inicializacion, limpieza o reparacion de datos."
    if path.endswith(".json"):
        return "Archivo de configuracion del tooling, build o entorno del frontend."
    if path.endswith(".md"):
        return "Documento auxiliar del repositorio con instrucciones o contexto funcional."
    if name in {"Dockerfile", "nginx.conf"}:
        return "Artefacto de despliegue para empaquetado y entrega del servicio."
    return infer_domain_description(path)


def infer_file_category(path: str) -> str:
    lowered = path.lower()
    if path.startswith("sigea-backend/"):
        return "backend"
    if path.startswith("sigea-frontend/"):
        return "frontend"
    return "root"


def clean_comment(text: str) -> str:
    text = re.sub(r"^/\*+", "", text.strip())
    text = re.sub(r"\*/$", "", text)
    lines = []
    for line in text.splitlines():
        line = re.sub(r"^\s*\*\s?", "", line).strip()
        if line:
            lines.append(line)
    return " ".join(lines).strip()


def first_sentence(text: str) -> str:
    if not text:
        return ""
    match = re.split(r"(?<=[.!?])\s+", text.strip(), maxsplit=1)
    return match[0].strip()


def split_params(text: str) -> list[str]:
    if not text.strip():
        return []
    current: list[str] = []
    parts: list[str] = []
    angle = paren = bracket = 0
    for char in text:
        if char == "," and angle == 0 and paren == 0 and bracket == 0:
            part = "".join(current).strip()
            if part:
                parts.append(part)
            current = []
            continue
        current.append(char)
        if char == "<":
            angle += 1
        elif char == ">":
            angle = max(0, angle - 1)
        elif char == "(":
            paren += 1
        elif char == ")":
            paren = max(0, paren - 1)
        elif char == "[":
            bracket += 1
        elif char == "]":
            bracket = max(0, bracket - 1)
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def parse_java_params(text: str) -> list[ParamDoc]:
    params: list[ParamDoc] = []
    for raw in split_params(text):
        raw = raw.replace("final ", "").strip()
        if not raw:
            continue
        if " " not in raw:
            params.append(ParamDoc(name=raw, type="desconocido"))
            continue
        param_type, name = raw.rsplit(" ", 1)
        params.append(ParamDoc(name=name.strip(), type=param_type.strip()))
    return params


def parse_ts_params(text: str) -> list[ParamDoc]:
    params: list[ParamDoc] = []
    for raw in split_params(text):
        raw = raw.strip()
        if not raw:
            continue
        if ":" in raw:
            name, param_type = raw.split(":", 1)
            optional = name.strip().endswith("?")
            params.append(ParamDoc(name=name.strip().rstrip("?"), type=param_type.strip(), optional=optional))
        else:
            optional = raw.endswith("?")
            params.append(ParamDoc(name=raw.rstrip("?"), type="any", optional=optional))
    return params


def find_matching_brace(text: str, start: int) -> int:
    depth = 0
    for index in range(start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return index
    return len(text) - 1


def infer_member_purpose(file_path: str, member_name: str, kind: str, annotations: list[str]) -> str:
    name = member_name.lower()
    if kind in {"enum", "interface", "class", "record"}:
        return f"Representa {member_name} dentro del modulo {Path(file_path).parent.name}."
    if any(a.startswith("@GetMapping") for a in annotations):
        return f"Atiende una consulta HTTP GET para la operacion {member_name}."
    if any(a.startswith("@PostMapping") for a in annotations):
        return f"Atiende una operacion HTTP POST para {member_name}."
    if any(a.startswith("@PutMapping") for a in annotations):
        return f"Atiende una actualizacion HTTP PUT relacionada con {member_name}."
    if any(a.startswith("@PatchMapping") for a in annotations):
        return f"Atiende una actualizacion parcial HTTP PATCH relacionada con {member_name}."
    if any(a.startswith("@DeleteMapping") for a in annotations):
        return f"Atiende una operacion HTTP DELETE para {member_name}."
    verbs = {
        "crear": "Crea un recurso o registro del dominio.",
        "registrar": "Registra un evento o cambio de estado en el dominio.",
        "listar": "Obtiene una coleccion filtrada de registros.",
        "buscar": "Busca un recurso puntual por criterio o identificador.",
        "obtener": "Recupera informacion consolidada del dominio.",
        "actualizar": "Actualiza datos persistidos o estado del dominio.",
        "cambiar": "Modifica un atributo relevante del recurso.",
        "desactivar": "Marca un recurso como inactivo sin borrado fisico.",
        "activar": "Rehabilita un recurso previamente inactivado.",
        "desbloquear": "Restablece condiciones de acceso o bloqueo.",
        "login": "Autentica al usuario y entrega el contexto de sesion.",
        "logout": "Finaliza la sesion local del usuario.",
        "verificar": "Valida un codigo, token o condicion del negocio.",
        "generar": "Construye una salida derivada, reporte o token.",
        "load": "Carga datos desde el backend o desde almacenamiento local.",
        "toggle": "Alterna un estado visual o de interfaz.",
        "mark": "Marca un estado local o remoto como procesado.",
        "relative": "Transforma datos para visualizacion en interfaz.",
    }
    for prefix, description in verbs.items():
        if name.startswith(prefix):
            return description
    return f"Resuelve una responsabilidad puntual dentro de {Path(file_path).name}."


def infer_side_effects(body: str, file_path: str) -> list[str]:
    body_lower = body.lower()
    effects: list[str] = []
    if ".save(" in body or ".delete(" in body or "repository" in body_lower:
        effects.append("Consulta o modifica datos persistidos en la base de datos.")
    if "http." in body_lower or "this.http." in body_lower:
        effects.append("Realiza llamadas HTTP hacia la API.")
    if "localstorage." in body_lower:
        effects.append("Lee o escribe datos de sesion en localStorage.")
    if "router.navigate" in body_lower:
        effects.append("Dispara navegacion de rutas en el frontend.")
    if "document." in body_lower or "window." in body_lower:
        effects.append("Interactua con el DOM o con APIs del navegador.")
    if "passwordencoder.encode" in body_lower or "bc" in body_lower:
        effects.append("Transforma credenciales o datos sensibles antes de persistirlos.")
    if "jwt" in body_lower or "token" in body_lower and "generar" in body_lower:
        effects.append("Genera o valida tokens de autenticacion.")
    if "enviarcorreo" in body_lower or "mail.sender" in body_lower or "correoservicio" in body_lower or "whatsapp" in body_lower:
        effects.append("Invoca canales de notificacion externos.")
    if "signal(" in body_lower or ".set(" in body_lower or ".update(" in body_lower:
        effects.append("Actualiza estado reactivo local del frontend.")
    if "files." in body_lower or "uploads" in body_lower or "multipart" in body_lower:
        effects.append("Lee o escribe archivos en el almacenamiento local del servidor.")
    if not effects and file_path.endswith(".html"):
        effects.append("No ejecuta logica por si mismo; delega acciones al componente asociado.")
    return dedupe(effects)


def infer_errors(signature_errors: list[str], body: str) -> list[str]:
    detected = list(signature_errors)
    detected.extend(re.findall(r"throw new ([A-Za-z0-9_$.]+)", body))
    detected.extend(re.findall(r"orElseThrow\(\(\) -> new ([A-Za-z0-9_$.]+)", body))
    return dedupe([item.split(".")[-1] for item in detected])


def dedupe(items: list[str]) -> list[str]:
    seen: set[str] = set()
    ordered: list[str] = []
    for item in items:
        item = item.strip()
        if not item or item in seen:
            continue
        seen.add(item)
        ordered.append(item)
    return ordered


def build_example(file_path: str, member: MemberDoc) -> str:
    if "/controller/" in file_path and any(a.startswith("@") for a in member.annotations):
        route = next((a for a in member.annotations if "Mapping" in a), "@RequestMapping")
        return f"HTTP -> {route} -> {member.name}(...)"
    if file_path.endswith("service.ts"):
        params = ", ".join(example_value(p.type) for p in member.params)
        if "Observable" in member.return_type:
            return f"this.servicio.{member.name}({params}).subscribe();"
        return f"this.servicio.{member.name}({params});"
    if file_path.endswith("component.ts"):
        params = ", ".join(example_value(p.type) for p in member.params)
        return f"component.{member.name}({params});"
    if file_path.endswith(".java"):
        params = ", ".join(example_value(p.type) for p in member.params)
        return f"var resultado = servicio.{member.name}({params});"
    return "No aplica ejemplo ejecutable directo; depende del flujo que lo consume."


def remove_block_comments(text: str) -> str:
    return re.sub(r"/\*(?!\*)(?:.|\n)*?\*/", "", text)


def example_value(type_name: str) -> str:
    lowered = type_name.lower()
    if "string" in lowered or "charsequence" in lowered:
        return '"valor"'
    if "long" in lowered or "int" in lowered or "number" in lowered or "integer" in lowered:
        return "1"
    if "boolean" in lowered or lowered == "bool":
        return "true"
    if "localdatetime" in lowered or "date" in lowered:
        return "fecha"
    if "list" in lowered or "[]" in lowered or "array" in lowered:
        return "[]"
    return "dato"


def parse_java_file(path: Path) -> FileDoc:
    text = path.read_text(encoding="utf-8")
    scan_text = sanitize_java_for_scan(text)
    file_path = rel(path)
    imports = re.findall(r"^import\s+([^;]+);", text, flags=re.M)
    package_match = re.search(r"^package\s+([^;]+);", text, flags=re.M)
    notes = [f"Paquete Java: {package_match.group(1)}."] if package_match else []

    class_matches = list(re.finditer(
        r"(?P<doc>/\*\*.*?\*/\s*)?(?P<annotations>(?:@[A-Za-z0-9_$.]+(?:\([^)]*\))?\s*)*)(?P<mods>(?:public|protected|private|abstract|final|static|sealed|non-sealed|\s)+)?(?P<kind>class|interface|enum|record)\s+(?P<name>[A-Za-z0-9_]+)",
        scan_text,
        flags=re.S,
    ))

    classes: list[tuple[ExportDoc, int, int]] = []
    for match in class_matches:
        kind = match.group("kind")
        name = match.group("name")
        doc = first_sentence(clean_comment(match.group("doc") or ""))
        open_brace = scan_text.find("{", match.end())
        close_brace = find_matching_brace(scan_text, open_brace) if open_brace != -1 else match.end()
        body = scan_text[open_brace + 1:close_brace] if open_brace != -1 else ""
        fields = parse_java_fields(body, kind)
        values = parse_java_enum_values(body) if kind == "enum" else []
        export = ExportDoc(
            kind=kind,
            name=name,
            description=doc or f"{kind.title()} del archivo {path.name}.",
            fields=fields,
            values=values,
            members=[],
        )
        classes.append((export, open_brace, close_brace))

    method_pattern = re.compile(
        r"(?P<doc>/\*\*.*?\*/\s*)?(?P<annotations>(?:@[A-Za-z0-9_$.]+(?:\([^)]*\))?\s*)*)(?P<mods>(?:public|protected|private|static|final|synchronized|abstract|default|native|\s)+)(?:(?P<rtype>[A-Za-z0-9_<>,.?@\[\] ]+)\s+)?(?P<name>[A-Za-z0-9_]+)\s*\((?P<params>[^)]*)\)\s*(?:throws\s+(?P<throws>[^\{]+))?\s*\{",
        flags=re.S,
    )

    for match in method_pattern.finditer(scan_text):
        name = match.group("name")
        if name in JAVA_KEYWORDS:
            continue
        start = scan_text.find("{", match.end() - 1)
        end = find_matching_brace(scan_text, start) if start != -1 else match.end()
        body = scan_text[start + 1:end] if start != -1 else ""
        annotations = re.findall(r"@[A-Za-z0-9_$.]+(?:\([^)]*\))?", match.group("annotations") or "")
        throws = [item.strip() for item in split_params((match.group("throws") or "").replace("\n", " "))]
        owner = next((cls for cls, begin, finish in classes if begin <= match.start() <= finish), None)
        return_type = (match.group("rtype") or "void").strip()
        if owner and name == owner.name:
            return_type = owner.name
        member = MemberDoc(
            kind="constructor" if owner and name == owner.name else "method",
            name=name,
            return_type=return_type,
            params=parse_java_params(match.group("params") or ""),
            annotations=annotations,
            purpose=first_sentence(clean_comment(match.group("doc") or "")) or infer_member_purpose(file_path, name, "method", annotations),
            side_effects=infer_side_effects(body, file_path),
            errors=infer_errors(throws, body),
            example="",
        )
        member.example = build_example(file_path, member)
        if owner is not None:
            owner.members.append(member)

    exports = [item[0] for item in classes]
    if not exports:
        exports.append(ExportDoc(kind="archivo", name=path.name, description="Archivo de soporte sin clases publicas detectadas."))

    warnings: list[str] = []
    if "@Getter" in text or "@Setter" in text or "@Builder" in text:
        warnings.append("⚠️ Usa Lombok; parte de getters, setters o builders se generan en compilacion y no aparecen explicitamente en el codigo fuente.")

    return FileDoc(
        path=path,
        category=infer_file_category(file_path),
        description=infer_file_description(file_path),
        dependencies=summarize_dependencies(imports, file_path),
        exports=exports,
        warnings=warnings,
        notes=notes,
    )


def parse_java_fields(body: str, kind: str) -> list[ParamDoc]:
    if kind == "enum":
        return []
    field_pattern = re.compile(r"(?:private|protected|public)\s+(?:static\s+)?(?:final\s+)?(?P<type>[A-Za-z0-9_<>,.?@\[\] ]+)\s+(?P<name>[A-Za-z0-9_]+)\s*(?:=|;)")
    fields: list[ParamDoc] = []
    for match in field_pattern.finditer(body):
        fields.append(ParamDoc(name=match.group("name"), type=match.group("type").strip()))
    return fields[:40]


def parse_java_enum_values(body: str) -> list[str]:
    head = body.split(";", 1)[0]
    values = []
    for raw in head.split(","):
        value = raw.strip().split("(", 1)[0].strip()
        if re.match(r"^[A-Z0-9_]+$", value):
            values.append(value)
    return values


def parse_ts_file(path: Path) -> FileDoc:
    text = path.read_text(encoding="utf-8")
    file_path = rel(path)
    imports = re.findall(r"^import\s+.*?from\s+['\"]([^'\"]+)['\"];?", text, flags=re.M)

    exports: list[ExportDoc] = []
    warnings: list[str] = []

    interface_pattern = re.compile(r"export\s+interface\s+(?P<name>[A-Za-z0-9_]+)\s*\{", flags=re.M)
    for match in interface_pattern.finditer(text):
        name = match.group("name")
        start = text.find("{", match.end() - 1)
        end = find_matching_brace(text, start)
        body = text[start + 1:end]
        fields = []
        for line in body.splitlines():
            line = line.strip().rstrip(";")
            if not line or line.startswith("//") or ":" not in line:
                continue
            prop, prop_type = line.split(":", 1)
            optional = prop.strip().endswith("?")
            fields.append(ParamDoc(name=prop.strip().rstrip("?"), type=prop_type.strip(), optional=optional))
        exports.append(ExportDoc(kind="interface", name=name, description=f"Contrato TypeScript exportado por {path.name}.", fields=fields[:60]))

    type_pattern = re.compile(r"export\s+type\s+(?P<name>[A-Za-z0-9_]+)\s*=\s*(?P<body>[^;]+);", flags=re.M)
    for match in type_pattern.finditer(text):
        exports.append(ExportDoc(kind="type", name=match.group("name"), description=f"Alias de tipo: {match.group('body').strip()}."))

    const_pattern = re.compile(r"export\s+const\s+(?P<name>[A-Za-z0-9_]+)\s*(?::\s*(?P<rtype>[^=]+))?=", flags=re.M)
    for match in const_pattern.finditer(text):
        exports.append(ExportDoc(kind="const", name=match.group("name"), description=f"Constante exportada por {path.name}."))

    class_pattern = re.compile(r"export\s+class\s+(?P<name>[A-Za-z0-9_]+).*?\{", flags=re.M | re.S)
    for match in class_pattern.finditer(text):
        name = match.group("name")
        start = text.find("{", match.end() - 1)
        end = find_matching_brace(text, start)
        body = text[start + 1:end]
        export = ExportDoc(kind="class", name=name, description=f"Clase/Componente Angular definido en {path.name}.")
        export.fields.extend(parse_ts_class_fields(body))
        export.members.extend(parse_ts_class_members(body, file_path))
        exports.append(export)

    function_pattern = re.compile(r"export\s+function\s+(?P<name>[A-Za-z0-9_]+)\s*\((?P<params>[^)]*)\)\s*(?::\s*(?P<rtype>[^\{]+))?\s*\{", flags=re.M)
    for match in function_pattern.finditer(text):
        name = match.group("name")
        member = MemberDoc(
            kind="function",
            name=name,
            return_type=(match.group("rtype") or "void").strip(),
            params=parse_ts_params(match.group("params") or ""),
            purpose=infer_member_purpose(file_path, name, "function", []),
            side_effects=infer_side_effects(text[match.end():find_matching_brace(text, text.find("{", match.end() - 1))], file_path),
            errors=[],
            example="",
        )
        member.example = build_example(file_path, member)
        exports.append(ExportDoc(kind="function", name=name, description=member.purpose, members=[member]))

    if "signal(" in text:
        warnings.append("⚠️ Usa Angular signals; parte del estado se deriva en runtime y no siempre se refleja como propiedades mutables tradicionales.")
    if not exports:
        exports.append(ExportDoc(kind="archivo", name=path.name, description="Archivo de soporte sin exports TypeScript detectados."))

    return FileDoc(
        path=path,
        category=infer_file_category(file_path),
        description=infer_file_description(file_path),
        dependencies=summarize_dependencies(imports, file_path),
        exports=exports,
        warnings=warnings,
    )


def parse_ts_class_fields(body: str) -> list[ParamDoc]:
    results: list[ParamDoc] = []
    depth = 0
    for raw_line in body.splitlines():
        line = raw_line.strip()
        if not line:
            depth += raw_line.count("{") - raw_line.count("}")
            continue
        if depth == 0:
            match = re.match(r"(?:public|private|protected)?\s*(?:readonly\s+)?(?P<name>[A-Za-z0-9_]+)\s*(?::\s*(?P<type>[^=;\n]+))?\s*=", line)
            if match:
                name = match.group("name")
                if name not in TS_KEYWORDS:
                    results.append(ParamDoc(name=name, type=(match.group("type") or "inferido").strip()))
        depth += raw_line.count("{") - raw_line.count("}")
    return dedupe_params(results[:40])


def dedupe_params(params: list[ParamDoc]) -> list[ParamDoc]:
    seen: set[str] = set()
    ordered: list[ParamDoc] = []
    for item in params:
        if item.name in seen:
            continue
        seen.add(item.name)
        ordered.append(item)
    return ordered


def parse_ts_class_members(body: str, file_path: str) -> list[MemberDoc]:
    members: list[MemberDoc] = []
    depth = 0
    lines = body.splitlines()
    index = 0
    while index < len(lines):
        raw_line = lines[index]
        line = raw_line.strip()
        if depth == 0:
            match = re.match(r"(?:public|private|protected)?\s*(?:async\s+)?(?P<name>[A-Za-z0-9_]+)\s*\((?P<params>[^)]*)\)\s*(?::\s*(?P<rtype>[^\{\n=]+))?\s*\{", line)
            if match and match.group("name") not in TS_KEYWORDS:
                name = match.group("name")
                fragment_lines = []
                local_depth = raw_line.count("{") - raw_line.count("}")
                cursor = index + 1
                while cursor < len(lines) and local_depth > 0:
                    fragment_lines.append(lines[cursor])
                    local_depth += lines[cursor].count("{") - lines[cursor].count("}")
                    cursor += 1
                fragment = "\n".join(fragment_lines)
                member = MemberDoc(
                    kind="method",
                    name=name,
                    return_type=(match.group("rtype") or "void").strip(),
                    params=parse_ts_params(match.group("params") or ""),
                    purpose=infer_member_purpose(file_path, name, "method", []),
                    side_effects=infer_side_effects(fragment, file_path),
                    errors=infer_errors([], fragment),
                    example="",
                )
                member.example = build_example(file_path, member)
                members.append(member)
                depth += raw_line.count("{") - raw_line.count("}")
                index += 1
                continue
        depth += raw_line.count("{") - raw_line.count("}")
        index += 1
    return members


def sanitize_java_for_scan(text: str) -> str:
    text = remove_block_comments(text)
    text = re.sub(r"//.*", "", text)
    return text


def parse_java_dtos() -> dict[str, DtoDoc]:
    dto_files = sorted(BACKEND.glob("src/main/java/co/edu/sena/sigea/**/*.java"))
    result: dict[str, DtoDoc] = {}
    for path in dto_files:
        if "/dto/" not in rel(path):
            continue
        text = path.read_text(encoding="utf-8")
        scan_text = remove_block_comments(text)
        class_match = re.search(r"class\s+([A-Za-z0-9_]+)", scan_text)
        if not class_match:
            continue
        dto = DtoDoc(name=class_match.group(1))
        pending_annotations: list[str] = []
        pending_comment = ""
        for raw_line in text.splitlines():
            line = raw_line.strip()
            if not line:
                continue
            if line.startswith("/**"):
                pending_comment = clean_comment(raw_line)
                continue
            if line.startswith("*"):
                pending_comment = (pending_comment + " " + clean_comment(raw_line)).strip()
                continue
            if line.startswith("@"):
                pending_annotations.append(line)
                continue
            field_match = re.match(r"private\s+([A-Za-z0-9_<>,.?\[\]]+)\s+([A-Za-z0-9_]+)\s*;", line)
            if field_match:
                dto.fields.append(
                    DtoField(
                        name=field_match.group(2),
                        type=field_match.group(1),
                        validations=[normalize_validation(a) for a in pending_annotations if is_validation_annotation(a)],
                        description=normalize_space(pending_comment),
                    )
                )
                pending_annotations = []
                pending_comment = ""
            elif not line.startswith("@"): 
                pending_annotations = []
        if dto.fields:
            result[dto.name] = dto
    return result


def normalize_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def is_validation_annotation(value: str) -> bool:
    return any(value.startswith(prefix) for prefix in (
        "@NotBlank", "@NotNull", "@Size", "@Min", "@Max", "@Pattern", "@Email", "@Positive", "@PositiveOrZero"
    ))


def normalize_validation(annotation: str) -> str:
    name = annotation.split("(", 1)[0].lstrip("@")
    if name == "NotBlank":
        return "obligatorio y no vacio"
    if name == "NotNull":
        return "obligatorio"
    if name == "Email":
        return "debe ser correo valido"
    if name == "Positive":
        return "debe ser positivo"
    if name == "PositiveOrZero":
        return "debe ser cero o positivo"
    min_match = re.search(r"min\s*=\s*(\d+)", annotation)
    max_match = re.search(r"max\s*=\s*(\d+)", annotation)
    value_match = re.search(r"value\s*=\s*(\d+)", annotation)
    regex_match = re.search(r'regexp\s*=\s*"([^"]+)"', annotation)
    if name == "Size":
        parts = []
        if min_match:
            parts.append(f"min {min_match.group(1)}")
        if max_match:
            parts.append(f"max {max_match.group(1)}")
        return "longitud " + ", ".join(parts)
    if name == "Min" and value_match:
        return f"valor minimo {value_match.group(1)}"
    if name == "Max" and value_match:
        return f"valor maximo {value_match.group(1)}"
    if name == "Pattern" and regex_match:
        return f"debe cumplir patron {regex_match.group(1)}"
    return name


def parse_service_exception_map() -> dict[str, list[str]]:
    service_files = sorted(BACKEND.glob("src/main/java/co/edu/sena/sigea/**/service/*.java"))
    exception_map: dict[str, list[str]] = {}
    for path in service_files:
        text = sanitize_java_for_scan(path.read_text(encoding="utf-8"))
        service_name = path.stem
        for match in re.finditer(r"(?:public|protected|private)\s+[A-Za-z0-9_<>,.?\[\] ]+\s+([A-Za-z0-9_]+)\s*\([^)]*\)\s*\{", text):
            method_name = match.group(1)
            start = text.find("{", match.end() - 1)
            end = find_matching_brace(text, start)
            body = text[start + 1:end]
            exception_map[f"{service_name}.{method_name}"] = infer_errors([], body)
    return exception_map


def parse_controller_endpoints(dto_map: dict[str, DtoDoc], service_exception_map: dict[str, list[str]]) -> list[EndpointDoc]:
    endpoints: list[EndpointDoc] = []
    for path in collect_controller_files():
        text = path.read_text(encoding="utf-8")
        clean_text = sanitize_java_for_scan(text)
        domain = path.parts[-3]
        controller_name = path.stem
        base_mapping = extract_mapping_value(re.search(r"@RequestMapping\(([^\)]*)\)", clean_text)) or ""
        service_field = re.search(r"private\s+final\s+([A-Za-z0-9_]+)\s+([A-Za-z0-9_]+);", clean_text)
        service_name = service_field.group(1) if service_field else ""
        lines = text.splitlines()
        index = 0
        pending_comments: list[str] = []
        pending_annotations: list[str] = []
        while index < len(lines):
            stripped = lines[index].strip()
            if stripped.startswith("/**"):
                comment_lines = [stripped]
                index += 1
                while index < len(lines):
                    comment_lines.append(lines[index].strip())
                    if lines[index].strip().endswith("*/"):
                        break
                    index += 1
                pending_comments = [clean_comment("\n".join(comment_lines))]
            elif stripped.startswith("@"):
                pending_annotations.append(stripped)
            elif re.search(r"\bclass\s+[A-Za-z0-9_]+", stripped):
                pending_annotations = []
                pending_comments = []
            elif re.match(r"public\s+ResponseEntity<", stripped):
                signature = stripped
                while "{" not in signature and index + 1 < len(lines):
                    index += 1
                    signature += " " + lines[index].strip()
                endpoint = build_endpoint_from_signature(
                    signature,
                    pending_annotations,
                    pending_comments,
                    base_mapping,
                    domain,
                    controller_name,
                    text,
                    clean_text,
                    service_name,
                    dto_map,
                    service_exception_map,
                )
                if endpoint:
                    endpoints.append(endpoint)
                pending_annotations = []
                pending_comments = []
            index += 1
    return endpoints


def extract_mapping_value(match: re.Match[str] | None) -> str | None:
    if not match:
        return None
    body = match.group(1)
    value_match = re.search(r'"([^"]+)"', body)
    return value_match.group(1) if value_match else ""


def build_endpoint_from_signature(
    signature: str,
    annotations: list[str],
    comments: list[str],
    base_mapping: str,
    domain: str,
    controller_name: str,
    raw_text: str,
    clean_text: str,
    service_name: str,
    dto_map: dict[str, DtoDoc],
    service_exception_map: dict[str, list[str]],
) -> EndpointDoc | None:
    mapping = next((a for a in annotations if "Mapping" in a), None)
    if not mapping:
        return None
    http_method = mapping.split("(", 1)[0].lstrip("@").replace("Mapping", "").upper() or "GET"
    sub_path = extract_mapping_value(re.search(r"\(([^\)]*)\)", mapping)) or ""
    full_path = normalize_endpoint_path(base_mapping, sub_path)
    method_match = re.search(r"public\s+ResponseEntity<(.+)>\s+([A-Za-z0-9_]+)\s*\((.*)\)\s*\{", signature)
    if not method_match:
        return None
    response_type = method_match.group(1).strip()
    method_name = method_match.group(2)
    params_text = method_match.group(3)
    params = split_params(params_text)
    path_params: list[ParamDoc] = []
    query_params: list[ParamDoc] = []
    request_body_type: str | None = None
    request_body_mode: str | None = None
    validations: dict[str, list[str]] = {}
    notes: list[str] = []
    for param in params:
        normalized = normalize_space(param)
        if "@PathVariable" in normalized:
            name = extract_named_annotation_value(normalized, "@PathVariable") or normalized.rsplit(" ", 1)[-1]
            path_params.append(ParamDoc(name=name, type=extract_param_type(normalized)))
        elif "@RequestParam" in normalized:
            name = extract_named_annotation_value(normalized, "@RequestParam") or normalized.rsplit(" ", 1)[-1]
            required = "required = false" not in normalized and "defaultValue" not in normalized
            query_params.append(ParamDoc(name=name, type=extract_param_type(normalized), optional=not required))
        elif "@RequestBody" in normalized or "@ModelAttribute" in normalized:
            request_body_type = extract_param_type(normalized)
            request_body_mode = "multipart/form-data" if "@ModelAttribute" in normalized else "application/json"
            if "MultipartFile" in normalized:
                notes.append("Body multipart con archivo binario.")
            if request_body_type in dto_map:
                validations = {field.name: field.validations for field in dto_map[request_body_type].fields if field.validations}
        elif "MultipartFile" in normalized:
            query_params.append(ParamDoc(name="archivo", type="MultipartFile", optional=False))
            notes.append("Requiere carga de archivo multipart.")
    body = extract_method_body_from_signature(clean_text, signature)
    method_status = infer_status_code(http_method, body)
    roles = infer_roles(annotations, clean_text, controller_name)
    description = infer_endpoint_description(domain, method_name, http_method, full_path, comments)
    service_calls = re.findall(r"([A-Za-z0-9_]+)\.([A-Za-z0-9_]+)\(", body)
    service_errors: list[str] = []
    for owner, called in service_calls:
        if owner in {"ResponseEntity", "HttpStatus", "SecurityContextHolder"}:
            continue
        if service_name and owner.lower().startswith(service_name.replace("Servicio", "").replace("Service", "").lower()[:5]):
            service_errors.extend(service_exception_map.get(f"{service_name}.{called}", []))
        else:
            service_errors.extend(service_exception_map.get(f"{service_name}.{called}", []))
    if request_body_type and request_body_type in dto_map:
        for field in dto_map[request_body_type].fields:
            if field.validations:
                validations[field.name] = field.validations
    return EndpointDoc(
        domain=domain,
        controller=controller_name,
        method=http_method,
        path=full_path,
        full_url=f"{API_BASE_URL}{full_path}",
        description=description,
        roles=roles,
        path_params=path_params,
        query_params=query_params,
        request_body_type=request_body_type,
        request_body_mode=request_body_mode,
        response_type=response_type,
        status_code=method_status,
        validations=validations,
        service_errors=dedupe(service_errors),
        notes=notes,
    )


def extract_method_body(clean_text: str, method_name: str) -> str:
    match = re.search(rf"public\s+ResponseEntity<[^>]+>\s+{method_name}\s*\([^)]*\)\s*\{{", clean_text)
    if not match:
        return ""
    start = clean_text.find("{", match.end() - 1)
    end = find_matching_brace(clean_text, start)
    return clean_text[start + 1:end]


def extract_method_body_from_signature(clean_text: str, signature: str) -> str:
    signature_head = normalize_space(signature.split("{", 1)[0])
    compact_text = normalize_space(clean_text)
    index = compact_text.find(signature_head)
    if index == -1:
        return ""
    start = compact_text.find("{", index + len(signature_head))
    end = find_matching_brace(compact_text, start)
    return compact_text[start + 1:end]


def normalize_endpoint_path(base_mapping: str, sub_path: str) -> str:
    pieces = ["/api/v1"]
    for value in (base_mapping, sub_path):
        value = value or ""
        if value:
            pieces.append(value if value.startswith("/") else f"/{value}")
    path = "".join(pieces)
    return re.sub(r"//+", "/", path)


def extract_named_annotation_value(param: str, annotation: str) -> str | None:
    match = re.search(rf"{re.escape(annotation)}\(([^\)]*)\)", param)
    if not match:
        return None
    body = match.group(1)
    literal = re.search(r'"([^"]+)"', body)
    return literal.group(1) if literal else None


def extract_param_type(param: str) -> str:
    cleaned = re.sub(r"@[A-Za-z0-9_$.]+(?:\([^)]*\))?", "", param)
    cleaned = cleaned.replace("final ", "")
    parts = cleaned.split()
    if len(parts) >= 2:
        return parts[-2]
    return "String"


def infer_status_code(http_method: str, method_body: str) -> int:
    if "noContent().build()" in method_body:
        return 204
    if "HttpStatus.CREATED" in method_body or "status(HttpStatus.CREATED)" in method_body:
        return 201
    if "ResponseEntity.ok" in method_body or ".ok()" in method_body:
        return 200
    return 201 if http_method == "POST" else 200


def infer_roles(annotations: list[str], clean_text: str, controller_name: str) -> str:
    pre = next((a for a in annotations if a.startswith("@PreAuthorize")), None)
    if pre:
        expr = re.search(r'"([^"]+)"', pre)
        return expr.group(1) if expr else "Autorizacion declarativa"
    class_match = re.search(rf"class\s+{controller_name}", clean_text)
    if class_match:
        header = clean_text[:class_match.start()]
        class_pre = re.findall(r'@PreAuthorize\("([^"]+)"\)', header)
        if class_pre:
            return class_pre[-1]
    if controller_name == "AuthControlador":
        return "Publico"
    return "Usuario autenticado"


def infer_endpoint_description(domain: str, method_name: str, http_method: str, full_path: str, comments: list[str]) -> str:
    if comments and comments[-1]:
        comment = comments[-1]
        comment = re.sub(r"GET|POST|PUT|PATCH|DELETE\s+/api/v1/[^ ]+", "", comment).strip()
        if comment:
            return comment.rstrip(".") + "."
    table = {
        "ambiente": "Gestiona ubicaciones, ambientes y sububicaciones del inventario.",
        "auditoria": "Consulta trazabilidad y eventos de auditoria del sistema.",
        "categoria": "Administra el catalogo de categorias de equipos.",
        "dashboard": "Entrega estadisticas visibles en el tablero principal.",
        "equipo": "Gestiona el inventario fisico de equipos y sus fotos.",
        "mantenimiento": "Administra mantenimientos preventivos y correctivos.",
        "marca": "Administra el catalogo de marcas de equipos.",
        "notificacion": "Consulta o actualiza notificaciones del usuario.",
        "observacion": "Registra o consulta observaciones asociadas a equipos y prestamos.",
        "prestamo": "Gestiona solicitudes, aprobaciones y devoluciones de prestamos.",
        "prestamoambiente": "Gestiona solicitudes y devoluciones de ambientes.",
        "reporte": "Genera archivos exportables para seguimiento operativo.",
        "reserva": "Administra reservas anticipadas de equipos.",
        "seguridad": "Gestiona autenticacion, registro y recuperacion de acceso.",
        "transferencia": "Registra movimientos de equipos entre responsables o inventarios.",
        "usuario": "Administra usuarios, roles, aprobaciones y credenciales.",
    }
    return table.get(domain, f"Operacion {method_name} sobre {full_path}.")


def build_request_body_example(dto_name: str | None, dto_map: dict[str, DtoDoc], request_mode: str | None) -> str | None:
    if not dto_name:
        return None
    dto = dto_map.get(dto_name)
    if not dto:
        return None
    lines = ["{"]
    for index, field in enumerate(dto.fields):
        validation_suffix = f" Validaciones: {', '.join(field.validations)}." if field.validations else ""
        description = field.description or describe_param(field.name)
        comma = "," if index < len(dto.fields) - 1 else ""
        lines.append(f'  "{field.name}": "{field.type} — {description}{validation_suffix}"{comma}')
    lines.append("}")
    if request_mode == "multipart/form-data":
        lines.append("Nota: enviar tambien el archivo binario en el campo archivo.")
    return "\n".join(lines)


def example_response_body(endpoint: EndpointDoc, dto_map: dict[str, DtoDoc]) -> str:
    if endpoint.status_code == 204:
        return "sin contenido"
    if endpoint.response_type in {"Void", "void"}:
        return '{"message": "operacion exitosa"}'
    if endpoint.response_type and endpoint.response_type.startswith("List<"):
        inner = endpoint.response_type[5:-1]
        return f'[{build_response_object(inner, dto_map)}]'
    if endpoint.response_type == "ByteArrayResource":
        return '{"file": "binario descargable"}'
    if endpoint.response_type and "Map<" in endpoint.response_type:
        return '{"mensaje": "operacion completada"}'
    return build_response_object(endpoint.response_type or "Respuesta", dto_map)


def build_response_object(type_name: str, dto_map: dict[str, DtoDoc]) -> str:
    dto = dto_map.get(type_name)
    if not dto:
        return '{"id": 1, "resultado": "ok"}'
    preview = []
    for field in dto.fields[:6]:
        preview.append(f'"{field.name}": {json_value_for_type(field.type)}')
    return "{" + ", ".join(preview) + "}"


def json_value_for_type(type_name: str) -> str:
    lowered = type_name.lower()
    if any(token in lowered for token in ["string", "rol", "estado", "tipo"]):
        return '"texto"'
    if any(token in lowered for token in ["long", "integer", "int", "double", "float"]):
        return "1"
    if "boolean" in lowered:
        return "true"
    if "localdatetime" in lowered:
        return '"2026-01-01T08:00:00"'
    if "list" in lowered:
        return "[]"
    return '"valor"'


def infer_response_rows(endpoint: EndpointDoc, dto_map: dict[str, DtoDoc]) -> list[tuple[str, str, str]]:
    rows: list[tuple[str, str, str]] = []
    success_when = {
        200: "La operacion se completa correctamente y devuelve datos.",
        201: "El recurso se crea correctamente.",
        204: "La operacion se ejecuta correctamente sin body de respuesta.",
    }
    rows.append((str(endpoint.status_code), success_when.get(endpoint.status_code, "Operacion exitosa."), example_response_body(endpoint, dto_map)))
    has_validation = bool(endpoint.request_body_type or endpoint.query_params)
    if has_validation:
        rows.append(("400", "El body, los enums o los parametros no cumplen validaciones o formato esperado.", '{"error": "Error de Validacion", "message": "Los datos enviados no son validos"}'))
    rows.append(("403", "El usuario no tiene permisos o el servicio rechaza la operacion por reglas de negocio.", '{"error": "Operacion No Permitida", "message": "Acceso denegado"}'))
    rows.append(("404", "El recurso consultado o relacionado no existe.", '{"error": "Recurso No Encontrado", "message": "No existe el recurso solicitado"}'))
    if endpoint.method in {"POST", "PUT", "PATCH", "DELETE"}:
        rows.append(("409", "Ya existe un registro incompatible o hay conflicto de estado/duplicidad.", '{"error": "Recurso Duplicado", "message": "Conflicto de datos"}'))
    if endpoint.domain in {"seguridad", "notificacion"}:
        rows.append(("503", "Falla un servicio externo de correo o WhatsApp requerido por la operacion.", '{"error": "Servicio de Correo No Disponible", "message": "Proveedor externo no responde"}'))
    return dedupe_rows(rows)


def dedupe_rows(rows: list[tuple[str, str, str]]) -> list[tuple[str, str, str]]:
    seen: set[str] = set()
    result: list[tuple[str, str, str]] = []
    for row in rows:
        if row[0] in seen:
            continue
        seen.add(row[0])
        result.append(row)
    return result


def curl_example(endpoint: EndpointDoc, dto_map: dict[str, DtoDoc]) -> str:
    path = endpoint.full_url
    for param in endpoint.path_params:
        path = path.replace("{" + param.name + "}", sample_path_value(param.type, param.name))
    query_parts = []
    for param in endpoint.query_params:
        query_parts.append(f"{param.name}={sample_query_value(param.type, param.name)}")
    if query_parts:
        path += "?" + "&".join(query_parts)
    lines = [f"curl -X {endpoint.method} {path} \\"]
    if endpoint.roles != "Publico":
        lines.append('  -H "Authorization: Bearer <token>" \\')
    if endpoint.request_body_mode == "application/json" and endpoint.request_body_type:
        lines.append('  -H "Content-Type: application/json" \\')
        sample_json = build_sample_json_payload(endpoint.request_body_type, dto_map)
        lines.append(f"  -d '{sample_json}'")
    elif endpoint.request_body_mode == "multipart/form-data":
        lines.append('  -H "Content-Type: multipart/form-data" \\')
        lines.append('  -F "archivo=@archivo.jpg"')
    else:
        lines[-1] = lines[-1].rstrip(" \\")
    return "\n".join(lines)


def sample_path_value(type_name: str, name: str) -> str:
    lowered = type_name.lower()
    if "long" in lowered or "int" in lowered:
        return "1"
    if name in {"estado", "nuevoEstado"}:
        return "ACTIVO"
    if name == "rol":
        return "ADMINISTRADOR"
    if name == "tipo":
        return "PREVENTIVO"
    return "valor"


def sample_query_value(type_name: str, name: str) -> str:
    lowered = type_name.lower()
    if name == "formato":
        return "pdf"
    if name in {"desde", "hasta"}:
        return "2026-01-01T00:00:00"
    if "boolean" in lowered:
        return "true"
    if "long" in lowered or "int" in lowered:
        return "1"
    if name == "estado":
        return "ACTIVO"
    return "valor"


def build_sample_json_payload(dto_name: str, dto_map: dict[str, DtoDoc]) -> str:
    dto = dto_map.get(dto_name)
    if not dto:
        return '{"campo": "valor"}'
    pairs = []
    for field in dto.fields[:8]:
        pairs.append(f'"{field.name}": {json_value_for_type(field.type)}')
    return "{" + ", ".join(pairs) + "}"


def render_api_section(dto_map: dict[str, DtoDoc], endpoints: list[EndpointDoc]) -> str:
    groups: dict[str, list[EndpointDoc]] = defaultdict(list)
    for endpoint in endpoints:
        groups[endpoint.domain].append(endpoint)
    lines = ["## 9. API REST", "", "Se agrega esta seccion al documento existente para cubrir el catalogo funcional de endpoints REST del backend Spring Boot.", ""]
    for domain in sorted(groups):
        lines.extend([f"### Dominio: {domain.title()}", ""])
        for endpoint in sorted(groups[domain], key=lambda item: (item.path, item.method)):
            lines.extend(render_endpoint(endpoint, dto_map))
            lines.append("")
    return "\n".join(lines).strip()


def build_setup_section() -> str:
    env_example = ENV_EXAMPLE_PATH.read_text(encoding="utf-8") if ENV_EXAMPLE_PATH.exists() else ""
    compose_text = COMPOSE_PATH.read_text(encoding="utf-8") if COMPOSE_PATH.exists() else ""
    nginx_local = NGINX_LOCAL_PATH.read_text(encoding="utf-8") if NGINX_LOCAL_PATH.exists() else ""
    return f"""
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
{env_example.strip()}
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
{compose_text.strip()}
```

Archivo `docker/nginx.local.conf` usado por el frontend en contenedor:

```nginx
{nginx_local.strip()}
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
""".strip()


def render_endpoint(endpoint: EndpointDoc, dto_map: dict[str, DtoDoc]) -> list[str]:
    lines = [f"### [{endpoint.method}] {endpoint.full_url}", f"- **Descripcion**: {endpoint.description}", f"- **Roles permitidos**: {humanize_roles(endpoint.roles)}"]
    lines.append("- **Parametros de ruta**: " + render_api_params(endpoint.path_params, is_query=False))
    lines.append("- **Parametros de query**: " + render_api_params(endpoint.query_params, is_query=True))
    if endpoint.request_body_type:
        lines.append("- **Request body** (si aplica):")
        lines.append("```json")
        lines.append(build_request_body_example(endpoint.request_body_type, dto_map, endpoint.request_body_mode) or "{}")
        lines.append("```")
    else:
        lines.append("- **Request body** (si aplica): no aplica")
    lines.append("- **Respuestas posibles**:")
    lines.append("  | Código | Cuándo ocurre | Ejemplo de body |")
    lines.append("  |--------|--------------|-----------------|")
    for code, when, example in infer_response_rows(endpoint, dto_map):
        lines.append(f"  | {code} | {when} | `{example}` |")
    lines.append("- **Ejemplo curl**:")
    lines.append("```bash")
    lines.append(curl_example(endpoint, dto_map))
    lines.append("```")
    return lines


def humanize_roles(roles: str) -> str:
    if roles == "Publico":
        return "Publico, no requiere JWT"
    if roles == "Usuario autenticado":
        return "Cualquier usuario autenticado con JWT"
    return roles


def render_api_params(params: list[ParamDoc], is_query: bool) -> str:
    if not params:
        return "ninguno"
    rendered = []
    prefix = "?" if is_query else "{"
    suffix = "" if is_query else "}"
    for param in params:
        requirement = "opcional" if param.optional else "requerido"
        token = f"{prefix}{param.name}{suffix}" if not is_query else f"?{param.name}"
        rendered.append(f"{token} -> {param.type} -> {describe_param(param.name)} -> {requirement}")
    return "; ".join(rendered)


def parse_sql_file(path: Path) -> FileDoc:
    text = path.read_text(encoding="utf-8")
    file_path = rel(path)
    statements = []
    for regex, prefix in [
        (r"CREATE TABLE\s+([A-Za-z0-9_]+)", "Crea tabla"),
        (r"ALTER TABLE\s+([A-Za-z0-9_]+)", "Modifica tabla"),
        (r"INSERT INTO\s+([A-Za-z0-9_]+)", "Inserta datos en"),
        (r"UPDATE\s+([A-Za-z0-9_]+)", "Actualiza datos en"),
        (r"DELETE FROM\s+([A-Za-z0-9_]+)", "Elimina datos de"),
    ]:
        for match in re.finditer(regex, text, flags=re.I):
            statements.append(f"{prefix} {match.group(1)}.")
    exports = [ExportDoc(kind="sql", name=path.name, description="Script SQL ejecutable.")]
    notes = dedupe(statements[:12])
    if not notes:
        notes.append("⚠️ No se pudieron inferir operaciones principales solo por analisis estatico; revisar sentencias manualmente si se usa en produccion.")
    return FileDoc(
        path=path,
        category=infer_file_category(file_path),
        description=infer_file_description(file_path),
        dependencies=["Depende del motor MariaDB/Flyway que ejecuta la sentencia SQL."],
        exports=exports,
        notes=notes,
    )


def parse_properties_file(path: Path) -> FileDoc:
    text = path.read_text(encoding="utf-8")
    keys = []
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key = line.split("=", 1)[0].strip()
        keys.append(key)
    export = ExportDoc(kind="properties", name=path.name, description=f"Configura {len(keys)} claves de Spring/entorno.")
    notes = []
    if keys:
        notes.append("Claves destacadas: " + ", ".join(keys[:12]) + ("." if len(keys) <= 12 else ", ..."))
    return FileDoc(
        path=path,
        category=infer_file_category(rel(path)),
        description=infer_file_description(rel(path)),
        dependencies=["Consumido por Spring Boot o por el proceso de build/ejecucion correspondiente."],
        exports=[export],
        notes=notes,
    )


def parse_json_file(path: Path) -> FileDoc:
    text = path.read_text(encoding="utf-8")
    file_path = rel(path)
    notes = []
    try:
        data = json.loads(text)
        if isinstance(data, dict):
            keys = list(data.keys())
            notes.append("Claves de nivel superior: " + ", ".join(keys[:15]) + ("." if len(keys) <= 15 else ", ..."))
    except json.JSONDecodeError:
        notes.append("⚠️ JSON no parseable estaticamente; revisar formato si se edita a mano.")
    return FileDoc(
        path=path,
        category=infer_file_category(file_path),
        description=infer_file_description(file_path),
        dependencies=["Consumido por el tooling del frontend, VS Code, Docker o Angular CLI."],
        exports=[ExportDoc(kind="json", name=path.name, description="Configuracion declarativa sin exports ejecutables.")],
        notes=notes,
    )


def parse_plain_file(path: Path) -> FileDoc:
    file_path = rel(path)
    return FileDoc(
        path=path,
        category=infer_file_category(file_path),
        description=infer_file_description(file_path),
        dependencies=["Consumido por herramientas, runtime o por desarrolladores como apoyo operativo."],
        exports=[ExportDoc(kind="archivo", name=path.name, description="Archivo no ejecutable o de soporte visual/configuracional.")],
    )


def parse_file(path: Path) -> FileDoc:
    if path.suffix == ".java":
        return parse_java_file(path)
    if path.suffix == ".ts":
        return parse_ts_file(path)
    if path.suffix == ".sql":
        return parse_sql_file(path)
    if path.suffix == ".properties":
        return parse_properties_file(path)
    if path.suffix == ".json":
        return parse_json_file(path)
    return parse_plain_file(path)


def build_overview(backend_files: list[Path], frontend_files: list[Path]) -> str:
    return f"""
# SIGEA - Documentacion Tecnica Integral

Fecha de generacion: {date.today().isoformat()}

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

- Archivos backend considerados: {len(backend_files)}
- Archivos frontend considerados: {len(frontend_files)}
- Total documentado: {len(backend_files) + len(frontend_files)}

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
""".strip()


def render_params(params: list[ParamDoc]) -> str:
    if not params:
        return "Sin parametros."
    lines = []
    for param in params:
        optional = "si" if param.optional else "no"
        lines.append(f"- {param.name}: {param.type}. Opcional: {optional}.")
    return "\n".join(lines)


def render_export(export: ExportDoc) -> str:
    parts = [f"#### {export.kind.title()}: {export.name}", "", export.description or "Sin descripcion explicita."]
    if export.fields:
        parts.extend(["", "Campos/props principales:", render_params(export.fields)])
    if export.values:
        parts.extend(["", "Valores expuestos:", "- " + ", ".join(export.values)])
    if export.members:
        parts.extend(["", "Miembros principales:"])
        for member in export.members:
            parts.extend(render_member(member))
    return "\n".join(parts)


def render_member(member: MemberDoc) -> list[str]:
    lines = [f"- {member.name} ({member.kind})", f"  Proposito: {member.purpose}"]
    lines.append(f"  Parametros: {render_params(member.params).replace(chr(10), '; ')}")
    lines.append(f"  Retorno: {member.return_type or 'void'}.")
    lines.append(
        "  Efectos secundarios: " + (", ".join(member.side_effects) if member.side_effects else "No se detectan efectos secundarios relevantes fuera del retorno.")
    )
    lines.append(
        "  Errores: " + (", ".join(member.errors) if member.errors else "No se infieren excepciones propias mas alla de errores del framework o validacion.")
    )
    lines.append(f"  Ejemplo de uso: {member.example}")
    if member.annotations:
        lines.append("  Anotaciones/metadata: " + ", ".join(member.annotations))
    if member.notes:
        lines.append(f"  Nota: {member.notes}")
    return lines


def render_file_doc(file_doc: FileDoc) -> str:
    file_path = rel(file_doc.path)
    title = f"### {file_path}"
    lines = [title, "", f"Descripcion: {file_doc.description}"]
    lines.append("")
    lines.append("Dependencias:")
    for dep in file_doc.dependencies or ["Sin dependencias explicitas detectadas."]:
        lines.append(f"- {dep}")
    lines.append("")
    lines.append("Exports:")
    if file_doc.exports:
        for export in file_doc.exports:
            lines.append(f"- {export.kind}: {export.name}")
    else:
        lines.append("- No expone simbolos reutilizables; sirve como soporte o configuracion.")
    if file_doc.notes:
        lines.append("")
        lines.append("Notas:")
        for note in file_doc.notes:
            lines.append(f"- {note}")
    if file_doc.warnings:
        lines.append("")
        lines.append("Advertencias:")
        for warning in file_doc.warnings:
            lines.append(f"- {warning}")
    if file_doc.exports:
        lines.append("")
        lines.append("Elementos internos:")
        for export in file_doc.exports:
            lines.append("")
            lines.append(render_export(export))
    return "\n".join(lines)


def build_markdown(file_docs: list[FileDoc], backend_files: list[Path], frontend_files: list[Path]) -> str:
    sections = [build_overview(backend_files, frontend_files)]
    grouped: dict[str, list[FileDoc]] = defaultdict(list)
    for doc in file_docs:
        head = rel(doc.path).split("/", 2)
        key = "/".join(head[:2]) if len(head) >= 2 else head[0]
        grouped[key].append(doc)

    for group in sorted(grouped):
        sections.append(f"## Modulo: {group}\n")
        sections.append(infer_domain_description(group))
        sections.append("")
        for doc in grouped[group]:
            sections.append(render_file_doc(doc))
            sections.append("")
    dto_map = parse_java_dtos()
    service_exception_map = parse_service_exception_map()
    endpoints = parse_controller_endpoints(dto_map, service_exception_map)
    sections.append(render_api_section(dto_map, endpoints))
    sections.append(build_setup_section())
    return "\n".join(sections).strip() + "\n"


def strip_html_tags(value: str) -> str:
    cleaned = re.sub(r"<[^>]+>", "", value)
    return normalize_space(cleaned)


def build_html_toc(html_body: str) -> str:
    heading_pattern = re.compile(r"<h([23]) id=\"([^\"]+)\">(.*?)</h\1>", flags=re.S)
    items: list[str] = []
    for match in heading_pattern.finditer(html_body):
        level = int(match.group(1))
        anchor = match.group(2)
        text = strip_html_tags(match.group(3))
        if not text:
            continue
        chip = ""
        chip_match = re.match(r"^(\d+(?:\.\d+)*)", text)
        if chip_match:
            chip = f"<span class='toc-chip'>{chip_match.group(1)}</span>"
        clean_text = escape(text, quote=False)
        items.append(
            f"<li class='toc-level-{level}'><a href='#{anchor}'>{chip}<span>{clean_text}</span></a></li>"
        )
    if not items:
        return ""
    return (
        "<nav class='toc-card'>"
        "<div class='toc-card-header'><h2>Tabla de Contenido</h2><p>Navegacion rapida por secciones</p></div>"
        "<ul>" + "".join(items) + "</ul>"
        "</nav>"
    )


def render_html(markdown_text: str) -> str:
    html_body = markdown.markdown(
        markdown_text,
        extensions=["tables", "fenced_code", "toc", "nl2br", "sane_lists"],
    )
    toc_html = build_html_toc(html_body)
    logo_path = "../sigea-frontend/public/assets/logo-sena.svg"
    css = """
/* ═══════════════════════════════════════════════════════════════
   SIGEA SENA — Design System
   Palette institucional SENA · Tipografia Inter · Stripe-Doc style
   ═══════════════════════════════════════════════════════════════ */
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono:wght@400;500&display=swap');

:root {
    /* Paleta institucional SENA */
    --brand:          #00963f;   /* verde principal */
    --brand-dark:     #006e2d;   /* verde oscuro hover / h2 */
    --brand-deeper:   #004d1e;   /* verde profundo covers */
    --brand-mid:      #0f7a38;
    --brand-soft:     #e8f7ee;   /* fondo suave destacados */
    --brand-border:   #b2ddc3;   /* bordes verdes */

    /* Neutros */
    --bg:             #f4f7f5;
    --paper:          #ffffff;
    --ink:            #18211e;   /* texto principal */
    --ink-2:          #364740;   /* texto secundario */
    --ink-3:          #5a6e67;   /* texto auxiliar / captions */
    --line:           #dce8e1;   /* divisores */

    /* Codigo */
    --code-bg:        #13201b;
    --code-ink:       #c9f0d8;
    --code-comment:   #5f8a72;
    --code-keyword:   #7ee8a2;
    --code-string:    #ffa869;

    /* Sombras */
    --shadow-sm:      0 1px 4px rgba(0,0,0,.06), 0 2px 10px rgba(0,0,0,.05);
    --shadow-md:      0 4px 16px rgba(0,0,0,.08), 0 1px 4px rgba(0,0,0,.04);
    --shadow-lg:      0 8px 32px rgba(0,0,0,.10), 0 2px 8px rgba(0,0,0,.05);

    /* Tipografia */
    --font-body:      'Inter', 'Segoe UI', Arial, sans-serif;
    --font-mono:      'JetBrains Mono', 'Cascadia Code', Consolas, monospace;
    --base-size:      15px;
    --line-height:    1.72;
    --radius:         10px;
    --radius-lg:      16px;
}

/* ── Reset mínimo ────────────────────────────────────────────── */
*, *::before, *::after { box-sizing: border-box; }

/* ── Base ───────────────────────────────────────────────────── */
html { scroll-behavior: smooth; }
body {
    font-family: var(--font-body);
    font-size: var(--base-size);
    line-height: var(--line-height);
    color: var(--ink);
    background: var(--bg);
    margin: 0;
    padding: 0;
    -webkit-font-smoothing: antialiased;
    text-rendering: optimizeLegibility;
}

/* ── Contenedor externo ─────────────────────────────────────── */
.page-wrapper {
    max-width: 1240px;
    margin: 32px auto 64px;
    padding: 0 20px;
}

/* ═══════════════════════════════════════════════════════════════
   PORTADA
   ═══════════════════════════════════════════════════════════════ */
.cover {
    position: relative;
    background: linear-gradient(145deg,
        var(--brand-deeper) 0%,
        #065c28 42%,
        #0d8040 78%,
        #1daa5a 100%);
    border-radius: var(--radius-lg);
    padding: 54px 56px 50px;
    color: #ffffff;
    overflow: hidden;
    margin-bottom: 32px;
    box-shadow: var(--shadow-lg);
}

/* Ornamento geométrico decorativo */
.cover::before {
    content: '';
    position: absolute;
    right: -60px;
    top: -60px;
    width: 340px;
    height: 340px;
    background: radial-gradient(circle, rgba(255,255,255,.10) 0%, transparent 70%);
    border-radius: 50%;
    pointer-events: none;
}
.cover::after {
    content: '';
    position: absolute;
    left: 0; right: 0;
    bottom: 0;
    height: 4px;
    background: linear-gradient(90deg, rgba(255,255,255,.0), rgba(255,255,255,.3), rgba(255,255,255,.0));
}

.cover-header {
    display: flex;
    align-items: center;
    gap: 20px;
    margin-bottom: 36px;
}

.cover-logo {
    width: 80px;
    height: 80px;
    border-radius: 14px;
    background: #ffffff;
    object-fit: contain;
    padding: 9px;
    box-shadow: 0 8px 24px rgba(0,0,0,.28);
    flex-shrink: 0;
}

.cover-org {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.cover-org-name {
    font-size: 13px;
    font-weight: 600;
    letter-spacing: .08em;
    text-transform: uppercase;
    color: rgba(255,255,255,.75);
    margin: 0;
}

.cover-org-sub {
    font-size: 12px;
    color: rgba(255,255,255,.55);
    font-weight: 400;
    margin: 0;
}

.cover-badge {
    display: inline-block;
    background: rgba(255,255,255,.15);
    border: 1px solid rgba(255,255,255,.25);
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    letter-spacing: .12em;
    text-transform: uppercase;
    padding: 4px 14px;
    color: rgba(255,255,255,.9);
    margin-bottom: 18px;
}

.cover h1 {
    margin: 0 0 10px;
    font-size: 40px;
    font-weight: 700;
    color: #ffffff;
    border: none;
    padding: 0;
    line-height: 1.15;
    letter-spacing: -.02em;
}

.cover-subtitle {
    font-size: 17px;
    color: rgba(255,255,255,.78);
    font-weight: 400;
    margin: 0 0 32px;
    line-height: 1.5;
}

.cover-footer {
    display: flex;
    gap: 28px;
    flex-wrap: wrap;
    padding-top: 24px;
    border-top: 1px solid rgba(255,255,255,.18);
}

.cover-meta-item {
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.cover-meta-label {
    font-size: 10px;
    font-weight: 600;
    letter-spacing: .1em;
    text-transform: uppercase;
    color: rgba(255,255,255,.5);
}

.cover-meta-value {
    font-size: 13px;
    font-weight: 500;
    color: rgba(255,255,255,.88);
}

/* ═══════════════════════════════════════════════════════════════
   LAYOUT DOS COLUMNAS
   ═══════════════════════════════════════════════════════════════ */
.layout {
    display: grid;
    grid-template-columns: 300px minmax(0, 1fr);
    gap: 28px;
    align-items: start;
}

/* ═══════════════════════════════════════════════════════════════
   TABLA DE CONTENIDO (sidebar)
   ═══════════════════════════════════════════════════════════════ */
.toc-card {
    position: sticky;
    top: 20px;
    background: var(--paper);
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    overflow: hidden;
    box-shadow: var(--shadow-sm);
}

.toc-card-header {
    background: linear-gradient(135deg, var(--brand-deeper), var(--brand-mid));
    padding: 16px 18px 14px;
}

.toc-card h2 {
    margin: 0 0 3px;
    border: none;
    color: #ffffff;
    font-size: 15px;
    font-weight: 700;
    padding: 0;
    letter-spacing: .01em;
}

.toc-card-header p {
    margin: 0;
    color: rgba(255,255,255,.62);
    font-size: 11px;
    font-weight: 400;
}

.toc-card ul {
    list-style: none;
    margin: 0;
    padding: 10px 0;
    max-height: 72vh;
    overflow-y: auto;
    overflow-x: hidden;
}

/* Scrollbar fina */
.toc-card ul::-webkit-scrollbar { width: 4px; }
.toc-card ul::-webkit-scrollbar-track { background: transparent; }
.toc-card ul::-webkit-scrollbar-thumb { background: var(--brand-border); border-radius: 4px; }

.toc-level-2 > a,
.toc-level-3 > a {
    display: flex;
    align-items: baseline;
    gap: 8px;
    text-decoration: none;
    padding: 6px 16px;
    border-radius: 0;
    transition: background .14s, color .14s;
    color: var(--ink-2);
    font-size: 12.5px;
    font-weight: 500;
    line-height: 1.45;
    border-left: 3px solid transparent;
}

.toc-level-2 > a:hover {
    background: var(--brand-soft);
    color: var(--brand-dark);
    border-left-color: var(--brand);
}

.toc-level-3 > a {
    padding-left: 32px;
    font-weight: 400;
    font-size: 11.5px;
    color: var(--ink-3);
}

.toc-level-3 > a:hover {
    background: var(--brand-soft);
    color: var(--brand-dark);
    border-left-color: var(--brand-border);
}

.toc-chip {
    display: inline-block;
    min-width: 28px;
    text-align: center;
    background: var(--brand-soft);
    color: var(--brand-dark);
    border-radius: 6px;
    font-size: 9.5px;
    padding: 1px 5px;
    font-weight: 700;
    border: 1px solid var(--brand-border);
    line-height: 1.55;
    flex-shrink: 0;
    font-variant-numeric: tabular-nums;
}

.toc-level-3 .toc-chip {
    background: transparent;
    border-color: #d0ddd8;
    color: var(--ink-3);
    font-size: 9px;
}

/* ═══════════════════════════════════════════════════════════════
   AREA DE CONTENIDO
   ═══════════════════════════════════════════════════════════════ */
.content {
    min-width: 0;
    background: var(--paper);
    border: 1px solid var(--line);
    border-radius: var(--radius-lg);
    padding: 44px 52px 60px;
    box-shadow: var(--shadow-sm);
}

/* ── Tipografia del contenido ───────────────────────────────── */
h1, h2, h3, h4, h5, h6 {
    font-family: var(--font-body);
    font-weight: 700;
    color: var(--ink);
    page-break-after: avoid;
    line-height: 1.25;
}

h1 {
    font-size: 28px;
    font-weight: 700;
    color: var(--brand-deeper);
    border-bottom: 2px solid var(--brand-border);
    padding-bottom: 12px;
    margin: 0 0 24px;
    letter-spacing: -.02em;
}

h2 {
    position: relative;
    margin-top: 48px;
    margin-bottom: 16px;
    font-size: 21px;
    font-weight: 700;
    color: var(--brand-dark);
    padding: 12px 16px 12px 20px;
    background: linear-gradient(90deg, var(--brand-soft), transparent);
    border-left: 4px solid var(--brand);
    border-radius: 0 var(--radius) var(--radius) 0;
    letter-spacing: -.01em;
}

h3 {
    margin-top: 32px;
    margin-bottom: 10px;
    font-size: 16.5px;
    font-weight: 600;
    color: var(--brand-deep, #1a5235);
    border-bottom: 1px dashed var(--line);
    padding-bottom: 6px;
}

h4 {
    margin-top: 22px;
    margin-bottom: 8px;
    font-size: 14.5px;
    font-weight: 600;
    color: var(--ink-2);
    text-transform: uppercase;
    letter-spacing: .05em;
    font-size: 12px;
}

h5, h6 {
    margin-top: 18px;
    font-size: 13px;
    font-weight: 600;
    color: var(--ink-3);
}

p {
    color: var(--ink-2);
    margin: 0 0 14px;
    orphans: 3;
    widows: 3;
}

/* ── Listas ─────────────────────────────────────────────────── */
ul, ol {
    padding-left: 24px;
    margin: 6px 0 14px;
}

li {
    color: var(--ink-2);
    margin-bottom: 5px;
    line-height: 1.65;
}

ul li::marker { color: var(--brand); }
ol li::marker { color: var(--brand-dark); font-weight: 600; }

li > ul, li > ol { margin-top: 4px; margin-bottom: 4px; }

/* ── Enlace ─────────────────────────────────────────────────── */
a {
    color: var(--brand-dark);
    text-decoration: none;
    border-bottom: 1px solid var(--brand-border);
    transition: border-color .14s, color .14s;
}
a:hover { color: var(--brand); border-bottom-color: var(--brand); }

/* ── Código en línea ────────────────────────────────────────── */
code {
    font-family: var(--font-mono);
    font-size: .82em;
    background: var(--brand-soft);
    color: #065c28;
    border: 1px solid #b8dfc7;
    border-radius: 5px;
    padding: 1px 6px;
}

/* ── Bloque de código ───────────────────────────────────────── */
pre {
    background: var(--code-bg);
    color: var(--code-ink);
    font-family: var(--font-mono);
    font-size: 12.5px;
    line-height: 1.6;
    padding: 18px 20px;
    border-radius: var(--radius);
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-all;
    border: 1px solid #1e3028;
    margin: 18px 0;
    position: relative;
    box-shadow: inset 0 1px 0 rgba(255,255,255,.04);
}

pre::before {
    content: 'CODE';
    position: absolute;
    top: 8px;
    right: 12px;
    font-size: 9px;
    font-weight: 700;
    letter-spacing: .12em;
    color: rgba(255,255,255,.2);
    font-family: var(--font-body);
}

pre code {
    background: transparent;
    border: none;
    color: inherit;
    padding: 0;
    font-size: inherit;
}

/* ── Tablas ─────────────────────────────────────────────────── */
table {
    width: 100%;
    border-collapse: separate;
    border-spacing: 0;
    margin: 20px 0;
    font-size: 13px;
    border: 1px solid var(--line);
    border-radius: var(--radius);
    overflow: hidden;
    box-shadow: var(--shadow-sm);
}

thead tr {
    background: linear-gradient(90deg, var(--brand-deeper), var(--brand-dark));
}

th {
    color: #ffffff;
    font-weight: 600;
    font-size: 12px;
    letter-spacing: .05em;
    text-transform: uppercase;
    padding: 11px 14px;
    text-align: left;
    border-bottom: none;
    white-space: nowrap;
}

td {
    padding: 10px 14px;
    vertical-align: top;
    border-top: 1px solid var(--line);
    color: var(--ink-2);
}

tbody tr:nth-child(even) td { background: #f7fbf9; }
tbody tr:hover td { background: var(--brand-soft); }

/* ── Blockquote ─────────────────────────────────────────────── */
blockquote {
    position: relative;
    margin: 20px 0;
    padding: 14px 18px 14px 48px;
    background: var(--brand-soft);
    border-left: 4px solid var(--brand);
    border-radius: 0 var(--radius) var(--radius) 0;
    color: var(--ink-2);
    font-style: italic;
}

blockquote::before {
    content: '"';
    position: absolute;
    left: 12px;
    top: 4px;
    font-size: 36px;
    line-height: 1;
    color: var(--brand);
    font-family: Georgia, serif;
    font-style: normal;
    opacity: .5;
}

blockquote p { margin: 0; color: inherit; }

/* ── HR ─────────────────────────────────────────────────────── */
hr {
    border: none;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--brand-border), transparent);
    margin: 36px 0;
}

/* ── Cajas de aviso / nota ──────────────────────────────────── */
.note-box, blockquote.note {
    background: #eaf7f1;
    border-left: 4px solid var(--brand);
    border-radius: 0 var(--radius) var(--radius) 0;
    padding: 12px 16px;
    margin: 18px 0;
}

strong { color: var(--ink); font-weight: 600; }
em { color: var(--ink-2); }

/* ═══════════════════════════════════════════════════════════════
   ENCABEZADO Y PIE — Impresion / PDF
   ═══════════════════════════════════════════════════════════════ */
@page {
    size: A4;
    margin: 22mm 18mm 24mm 18mm;

    @top-left {
        content: 'SIGEA SENA';
        font-family: 'Inter', Arial, sans-serif;
        font-size: 8pt;
        font-weight: 700;
        color: #00963f;
        letter-spacing: .06em;
        text-transform: uppercase;
        vertical-align: middle;
        padding-top: 6mm;
    }

    @top-right {
        content: 'Documentacion Tecnica';
        font-family: 'Inter', Arial, sans-serif;
        font-size: 8pt;
        color: #5a6e67;
        vertical-align: middle;
        padding-top: 6mm;
    }

    @top-center {
        content: '';
        border-bottom: .5pt solid #b2ddc3;
        display: block;
        width: 100%;
        padding-top: 11mm;
    }

    @bottom-center {
        content: 'Pagina ' counter(page) ' de ' counter(pages);
        font-family: 'Inter', Arial, sans-serif;
        font-size: 8pt;
        color: #5a6e67;
        vertical-align: middle;
        padding-bottom: 4mm;
    }

    @bottom-left {
        content: 'Servicio Nacional de Aprendizaje — SENA';
        font-family: 'Inter', Arial, sans-serif;
        font-size: 7.5pt;
        color: #8aaa98;
        vertical-align: middle;
        padding-bottom: 4mm;
    }

    @bottom-right {
        content: 'Generado el ' string(generated-date);
        font-family: 'Inter', Arial, sans-serif;
        font-size: 7.5pt;
        color: #8aaa98;
        vertical-align: middle;
        padding-bottom: 4mm;
    }
}

@page :first {
    @top-left   { content: none; }
    @top-right  { content: none; }
    @top-center { content: none; }
    @bottom-center { content: none; }
    @bottom-left   { content: none; }
    @bottom-right  { content: none; }
}

/* ═══════════════════════════════════════════════════════════════
   MEDIA PRINT
   ═══════════════════════════════════════════════════════════════ */
@media print {
    body { background: #ffffff; font-size: 10.5pt; }

    .page-wrapper { max-width: none; margin: 0; padding: 0; }

    .cover {
        min-height: 100vh;
        border-radius: 0;
        display: flex;
        flex-direction: column;
        justify-content: center;
        margin: 0;
        page-break-after: always;
        box-shadow: none;
    }

    .layout { display: block; }

    .toc-card {
        border-radius: 0;
        position: static;
        box-shadow: none;
        page-break-after: always;
        max-height: none;
    }

    .toc-card ul { max-height: none; overflow: visible; }

    .content {
        border: none;
        border-radius: 0;
        padding: 0;
        box-shadow: none;
    }

    pre { white-space: pre-wrap; word-break: break-all; }
    table { page-break-inside: avoid; }
    h2, h3 { page-break-after: avoid; }

    a { color: var(--brand-dark); border: none; text-decoration: none; }
    a[href^="http"]::after { content: ' (' attr(href) ')'; font-size: .7em; color: #888; }
}

/* ═══════════════════════════════════════════════════════════════
   RESPONSIVE
   ═══════════════════════════════════════════════════════════════ */
@media (max-width: 1100px) {
    .page-wrapper { margin: 16px; }
    .layout { grid-template-columns: 1fr; }
    .toc-card { position: static; max-height: none; }
    .toc-card ul { max-height: 60vh; }
    .content { padding: 28px 24px 40px; }
    .cover { padding: 36px 28px 32px; }
    .cover h1 { font-size: 30px; }
}
    """
    today_str = date.today().isoformat()
    return (
        "<!doctype html>\n<html lang='es'>\n<head>\n"
        "<meta charset='utf-8'>\n"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>\n"
        "<meta name='author' content='SIGEA SENA'>\n"
        "<meta name='generator' content='generate_sigea_docs.py'>\n"
        f"<meta name='date' content='{today_str}'>\n"
        "<title>SIGEA SENA — Documentacion Tecnica</title>\n"
        "<style>" + escape(css, quote=False) + "</style>\n"
        "</head>\n<body>\n"
        "<div class='page-wrapper'>\n"
        # ── Portada ──────────────────────────────────────────────
        "<section class='cover'>\n"
        "  <div class='cover-header'>\n"
        f"    <img class='cover-logo' src='{logo_path}' alt='Logo SENA' />\n"
        "    <div class='cover-org'>\n"
        "      <p class='cover-org-name'>Servicio Nacional de Aprendizaje</p>\n"
        "      <p class='cover-org-sub'>Sistema de Informacion de Gestion de Equipos y Activos</p>\n"
        "    </div>\n"
        "  </div>\n"
        "  <div class='cover-badge'>Documentacion Tecnica</div>\n"
        "  <h1>SIGEA&nbsp;SENA</h1>\n"
        "  <p class='cover-subtitle'>Arquitectura, diseno, API REST, modelo de datos y guia de desarrollo</p>\n"
        "  <div class='cover-footer'>\n"
        f"    <div class='cover-meta-item'><span class='cover-meta-label'>Fecha</span><span class='cover-meta-value'>{today_str}</span></div>\n"
        "    <div class='cover-meta-item'><span class='cover-meta-label'>Formato</span><span class='cover-meta-value'>HTML / PDF</span></div>\n"
        "    <div class='cover-meta-item'><span class='cover-meta-label'>Uso</span><span class='cover-meta-value'>Interno — SENA</span></div>\n"
        "  </div>\n"
        "</section>\n"
        # ── Layout ────────────────────────────────────────────────
        "<div class='layout'>\n"
        + toc_html + "\n"
        "<article class='content'>\n"
        + html_body + "\n"
        "</article>\n"
        "</div>\n"
        "</div>\n"
        "</body>\n</html>"
    )


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    backend_files, frontend_files = collect_files()
    file_docs = [parse_file(path) for path in backend_files + frontend_files]
    markdown_text = build_markdown(file_docs, backend_files, frontend_files)
    html_text = render_html(markdown_text)
    MD_OUTPUT.write_text(markdown_text, encoding="utf-8")
    HTML_OUTPUT.write_text(html_text, encoding="utf-8")
    print(f"Markdown generado en: {MD_OUTPUT}")
    print(f"HTML generado en: {HTML_OUTPUT}")


if __name__ == "__main__":
    main()