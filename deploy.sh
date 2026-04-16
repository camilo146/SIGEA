#!/usr/bin/env bash
# ==============================================================================
# SIGEA - Script de despliegue para servidor SENA
# ==============================================================================
# Uso:
#   Primera vez:  bash deploy.sh
#   Actualizar:   bash deploy.sh update
#
# El frontend se publica en HTTP puerto 80 mediante Nginx dentro de Docker.
# No se usa HTTPS ni Caddy. El puerto 80 del host debe estar libre.
# ==============================================================================

set -euo pipefail

REPO_URL="https://github.com/camilo146/SIGEA.git"
APP_DIR="/opt/sigea/SIGEA"
ACTION="${1:-install}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[SIGEA]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

free_port_80() {
  # 1. Detener servicios del host que puedan ocupar el puerto 80.
  for svc in nginx apache2 httpd caddy; do
    if systemctl is-active --quiet "$svc" 2>/dev/null; then
      warning "Deteniendo $svc del host (ocupa el puerto 80)..."
      sudo systemctl stop "$svc" || true
      sudo systemctl disable "$svc" || true
    fi
  done

  # 2. Esperar a que el kernel libere el socket (Docker tarda unos segundos).
  sleep 4

  # 3. Si el puerto sigue ocupado, matar el proceso directamente.
  if ss -tlnp 2>/dev/null | grep -q ':80 '; then
    warning "Puerto 80 todavía en uso. Intentando liberar con fuser..."
    sudo fuser -k 80/tcp 2>/dev/null || true
    sleep 2
  fi

  # 4. Verificación final.
  if ss -tlnp 2>/dev/null | grep -q ':80 '; then
    # Mostrar qué proceso sigue usando el puerto para facilitar el diagnóstico.
    error "Puerto 80 aún en uso: $(ss -tlnp | grep ':80 ')"
  fi
}

validate_smtp_config() {
  local require_verification smtp_host smtp_port smtp_user smtp_password
  require_verification="$(grep -E '^SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  smtp_host="$(grep -E '^SIGEA_SMTP_HOST=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  smtp_port="$(grep -E '^SIGEA_SMTP_PORT=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  smtp_user="$(grep -E '^SIGEA_SMTP_USERNAME=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  smtp_password="$(grep -E '^SIGEA_SMTP_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"

  if [ -n "$smtp_host" ] && [ -n "$smtp_port" ] && [ -n "$smtp_user" ] && [ -n "$smtp_password" ]; then
    info "SMTP configurado para $smtp_user en $smtp_host:$smtp_port"
    return 0
  fi

  if [ "$require_verification" = "true" ]; then
    error "SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION=true pero la configuración SMTP está incompleta en .env. Define SIGEA_SMTP_HOST, SIGEA_SMTP_PORT, SIGEA_SMTP_USERNAME y SIGEA_SMTP_PASSWORD."
  fi

  warning "SMTP incompleto en .env. Los correos de prueba, notificaciones, mora y verificación pueden fallar."
}

ensure_db_user_credentials() {
  local db_name db_user db_password root_password
  db_name="$(grep -E '^MARIADB_DATABASE=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  db_user="$(grep -E '^MARIADB_USER=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  db_password="$(grep -E '^MARIADB_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  root_password="$(grep -E '^MARIADB_ROOT_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"

  [ -n "$db_name" ] || db_name="sigea_db"
  [ -n "$db_user" ] || db_user="sigea_user"
  [ -n "$db_password" ] || db_password="sigea_password_2026"
  [ -n "$root_password" ] || root_password="root_sigea_2026"

  info "Asegurando credenciales de base de datos para $db_user ..."
  if ! docker compose -f docker-compose.yml exec -T db \
    mariadb -uroot -p"$root_password" <<SQL
CREATE DATABASE IF NOT EXISTS \`$db_name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE OR REPLACE USER '$db_user'@'%' IDENTIFIED BY '$db_password';
CREATE OR REPLACE USER '$db_user'@'localhost' IDENTIFIED BY '$db_password';
GRANT ALL PRIVILEGES ON \`$db_name\`.* TO '$db_user'@'%';
GRANT ALL PRIVILEGES ON \`$db_name\`.* TO '$db_user'@'localhost';
FLUSH PRIVILEGES;
SQL
  then
    warning "No se pudo acceder con la clave root declarada; intentando reparación por socket local..."
    docker compose -f docker-compose.yml exec -T db \
      mariadb -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`$db_name\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE OR REPLACE USER '$db_user'@'%' IDENTIFIED BY '$db_password';
CREATE OR REPLACE USER '$db_user'@'localhost' IDENTIFIED BY '$db_password';
GRANT ALL PRIVILEGES ON \`$db_name\`.* TO '$db_user'@'%';
GRANT ALL PRIVILEGES ON \`$db_name\`.* TO '$db_user'@'localhost';
FLUSH PRIVILEGES;
SQL
  fi

  docker compose -f docker-compose.yml exec -T db \
    mariadb -u"$db_user" -p"$db_password" -e "USE \`$db_name\`; SELECT 1;" >/dev/null
}

ensure_default_admin_user() {
  local db_name root_password total_users admin_count sql_file
  db_name="$(grep -E '^MARIADB_DATABASE=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  root_password="$(grep -E '^MARIADB_ROOT_PASSWORD=' .env 2>/dev/null | tail -1 | cut -d= -f2- | tr -d '\r' || true)"
  [ -n "$db_name" ] || db_name="sigea_db"
  [ -n "$root_password" ] || root_password="root_sigea_2026"
  sql_file="sigea-backend/scripts/crear-usuario-admin.sql"

  if [ ! -f "$sql_file" ]; then
    error "No se encontró el script de restauración de admin: $sql_file"
  fi

  info "Asegurando usuario administrador por defecto..."
  docker compose -f docker-compose.yml exec -T db mariadb -uroot -p"$root_password" "$db_name" < "$sql_file"

  total_users="$(docker compose -f docker-compose.yml exec -T db mariadb -N -s -uroot -p"$root_password" "$db_name" -e "SELECT COUNT(*) FROM usuario;" 2>/dev/null || echo 0)"
  admin_count="$(docker compose -f docker-compose.yml exec -T db mariadb -N -s -uroot -p"$root_password" "$db_name" -e "SELECT COUNT(*) FROM usuario WHERE rol='ADMINISTRADOR';" 2>/dev/null || echo 0)"

  if [ "${total_users:-0}" = "0" ] || [ "${admin_count:-0}" = "0" ]; then
    error "No fue posible garantizar el usuario administrador por defecto."
  fi

  info "Administrador asegurado: documento 999999999 / password"
}

compose_up() {
  # Eliminar contenedores huérfanos (ej: sigea-caddy de deploys anteriores)
  # que podrían estar ocupando el puerto 80 antes de levantar los nuevos.
  docker rm -f sigea-caddy 2>/dev/null || true
  docker compose -f docker-compose.yml down --remove-orphans 2>/dev/null || true
  free_port_80

  # Levantar primero la base de datos y reconciliar las credenciales del usuario de aplicación.
  docker compose -f docker-compose.yml up -d db
  info "Esperando que la base de datos esté saludable..."
  for _ in $(seq 1 30); do
    if docker compose -f docker-compose.yml ps db | grep -q "healthy"; then
      break
    fi
    sleep 2
  done
  ensure_db_user_credentials

  docker compose -f docker-compose.yml up -d --build --remove-orphans backend frontend
}

wait_for_stack_health() {
  local code=""

  info "Verificando que el backend responda..."
  for _ in $(seq 1 24); do
    code="$(curl -s -o /tmp/sigea_backend_health.txt -w '%{http_code}' http://127.0.0.1:8082/api/v1/actuator/health || true)"
    if [ "$code" = "200" ] || [ "$code" = "401" ] || [ "$code" = "403" ]; then
      info "Backend operativo (HTTP $code)."
      break
    fi
    sleep 5
  done

  if [ "$code" != "200" ] && [ "$code" != "401" ] && [ "$code" != "403" ]; then
    docker compose -f docker-compose.yml ps || true
    docker compose -f docker-compose.yml logs --tail=200 backend db || true
    error "El backend no respondió correctamente después del despliegue. Último HTTP: ${code:-sin respuesta}"
  fi

  ensure_default_admin_user

  info "Verificando login directo del backend..."
  for _ in $(seq 1 12); do
    code="$(curl -s -o /tmp/sigea_backend_login.txt -w '%{http_code}' \
      -H 'Content-Type: application/json' \
      -d '{"numeroDocumento":"999999999","contrasena":"password"}' \
      http://127.0.0.1:8082/api/v1/auth/login || true)"
    if [ "$code" = "200" ]; then
      info "Login backend operativo (HTTP 200)."
      break
    fi
    sleep 3
  done

  if [ "$code" != "200" ]; then
    docker compose -f docker-compose.yml logs --tail=200 backend db || true
    error "El backend no aceptó el login del administrador por defecto. Último HTTP: ${code:-sin respuesta}"
  fi

  info "Verificando que el login por el proxy del frontend funcione..."
  for _ in $(seq 1 24); do
    code="$(curl -s -o /tmp/sigea_proxy_login.txt -w '%{http_code}' \
      -H 'Content-Type: application/json' \
      -d '{"numeroDocumento":"999999999","contrasena":"password"}' \
      http://127.0.0.1/api/v1/auth/login || true)"
    if [ "$code" = "200" ]; then
      info "Proxy operativo (HTTP 200 en login)."
      return 0
    fi
    sleep 5
  done

  docker compose -f docker-compose.yml ps || true
  docker compose -f docker-compose.yml logs --tail=200 frontend backend || true
  error "El login a través del proxy del frontend no respondió 200. Revisa nginx y backend."
}

frontend_url() {
  local host domain frontend_port
  host="$(hostname -I | awk '{print $1}')"
  domain="$(grep -E '^SIGEA_DOMAIN=' .env 2>/dev/null | tail -1 | cut -d= -f2 | tr -d ' \r\n')"
  frontend_port="$(grep -E '^FRONTEND_PORT=' .env 2>/dev/null | tail -1 | cut -d= -f2 | tr -d ' \r\n')"
  [ -n "$frontend_port" ] || frontend_port="80"

  if [ -n "$domain" ] && [ "$domain" != "localhost" ]; then
    echo "http://$domain"
  elif [ "$frontend_port" = "80" ]; then
    echo "http://$host"
  else
    echo "http://$host:$frontend_port"
  fi
}

# ── Verificar Docker ────────────────────────────────────────────────────────────
command -v docker >/dev/null 2>&1 || error "Docker no está instalado."
docker compose version >/dev/null 2>&1 || error "Docker Compose plugin no encontrado. Instálalo con tu gestor de paquetes (Ubuntu/Debian: apt, Rocky/RHEL: dnf)."

# ══════════════════════════════════════════════════════════════════════════════
# INSTALACIÓN (primera vez)
# ══════════════════════════════════════════════════════════════════════════════
if [ "$ACTION" = "install" ]; then

  info "Instalando SIGEA en $APP_DIR ..."

  # Si ya se clonó el repo (flujo recomendado), usarlo sin salir.
  if [ -d "$APP_DIR/.git" ]; then
    info "Repositorio detectado en $APP_DIR. Continuando instalación en directorio existente..."
    cd "$APP_DIR"
  elif [ -d "$SCRIPT_DIR/.git" ] && [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    warning "No se encontró repo en $APP_DIR; se usará el repositorio actual: $SCRIPT_DIR"
    APP_DIR="$SCRIPT_DIR"
    cd "$APP_DIR"
  else
    # Crear directorio y clonar solo si no existe repo en ruta estándar ni en directorio actual.
    sudo mkdir -p "$APP_DIR"  
    sudo chown "$USER:$USER" "$APP_DIR"

    info "Clonando repositorio..."
    git clone "$REPO_URL" "$APP_DIR"
    cd "$APP_DIR"
  fi

  [ -f "docker-compose.yml" ] || error "No se encontró docker-compose.yml en $APP_DIR"

  # Crear .env si no existe
  if [ ! -f ".env" ]; then
    cp .env.example .env
    warning "Se creó .env desde .env.example."
    warning "IMPORTANTE: Edita el archivo antes de continuar:"
    warning "  nano $APP_DIR/.env"
    echo ""
    read -r -p "¿Editaste el .env y estás listo para continuar? (s/N): " confirm
    [[ "$confirm" =~ ^[sS]$ ]] || { warning "Abortado. Edita el .env y ejecuta: bash deploy.sh update"; exit 0; }
  fi

  if ! grep -Eq '^SIGEA_DOMAIN=' .env; then
    warning "SIGEA_DOMAIN no está definido. El acceso será por IP directa."
  elif grep -Eq '^SIGEA_DOMAIN=localhost$' .env; then
    warning "SIGEA_DOMAIN=localhost no sirve para acceso remoto. Se usará IP directa."
  fi

  validate_smtp_config

  # Construir y levantar
  info "Construyendo imágenes y levantando contenedores (puede tardar 5-10 min la primera vez)..."
  compose_up

  # Verificar
  info "Esperando que los contenedores inicien..."
  sleep 15
  docker compose ps
  wait_for_stack_health

  echo ""
  info "✓ SIGEA instalado correctamente."
  info "  Frontend: $(frontend_url)"
  info "  Backend:  servicio interno (no expuesto públicamente)"
  echo ""
  info "Credenciales por defecto: documento 999999999 / password"

# ══════════════════════════════════════════════════════════════════════════════
# ACTUALIZACIÓN
# ══════════════════════════════════════════════════════════════════════════════
elif [ "$ACTION" = "update" ]; then 

  if [ -d "$APP_DIR/.git" ]; then
    cd "$APP_DIR"
  elif [ -d "$SCRIPT_DIR/.git" ] && [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    warning "No se encontró instalación en $APP_DIR; se usará el repositorio actual: $SCRIPT_DIR"
    APP_DIR="$SCRIPT_DIR"
    cd "$APP_DIR"
  else
    error "SIGEA no está instalado en $APP_DIR. Ejecuta: bash deploy.sh"
  fi

  info "Actualizando SIGEA..."

  if [ "${SIGEA_SKIP_PULL:-0}" != "1" ]; then
    # Guardar cambios locales (si los hay)
    git stash 2>/dev/null || true

    # Obtener última versión
    info "Descargando cambios del repositorio..."
    git pull origin main
  fi

  validate_smtp_config

  # Reconstruir y reiniciar
  info "Reconstruyendo contenedores..."
  compose_up

  info "Verificando estado..."
  docker compose ps
  wait_for_stack_health

  echo ""
  info "✓ SIGEA actualizado correctamente."
  info "  Frontend: $(frontend_url)"

else
  echo "Uso: bash deploy.sh [install|update]"
  exit 1
fi
