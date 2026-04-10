#!/usr/bin/env bash
# ==============================================================================
# SIGEA - Script de despliegue para servidor SENA
# ==============================================================================
# Uso:
#   Primera vez:  bash deploy.sh
#   Actualizar:   bash deploy.sh update
#
# Nota:
#   Este script asume publicación interna del frontend en puerto 443 con certificado
#   interno/autofirmado servido por Caddy.
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

frontend_url() {
  local host domain frontend_port
  host="$(hostname -I | awk '{print $1}')"
  domain="$(grep -E '^SIGEA_DOMAIN=' .env 2>/dev/null | tail -1 | cut -d= -f2 | tr -d ' \r\n')"
  frontend_port="$(grep -E '^FRONTEND_PORT=' .env 2>/dev/null | tail -1 | cut -d= -f2 | tr -d ' \r\n')"
  [ -n "$frontend_port" ] || frontend_port="443"

  if [ -n "$domain" ] && [ "$domain" != "localhost" ]; then
    echo "https://$domain"
  elif [ "$frontend_port" = "443" ]; then
    echo "https://$host"
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

  # Advertencia de seguridad: APP_URL debe usar HTTPS en producción
  if grep -Eq '^SIGEA_APP_URL=http://' .env; then
    warning "SIGEA_APP_URL usa http://. Para proteger credenciales en tránsito, usa HTTPS (https://...)."
  fi

  if ! grep -Eq '^SIGEA_DOMAIN=' .env; then
    warning "SIGEA_DOMAIN no está definido. El acceso por dominio interno y el certificado autofirmado dependen de ese valor."
  elif grep -Eq '^SIGEA_DOMAIN=localhost$' .env; then
    warning "SIGEA_DOMAIN=localhost no sirve para acceso remoto por dominio. Se usará acceso directo por IP y FRONTEND_PORT."
  fi

  # Construir y levantar
  info "Construyendo imágenes y levantando contenedores (puede tardar 5-10 min la primera vez)..."
  docker compose up -d --build

  # Verificar
  info "Esperando que los contenedores inicien..."
  sleep 15
  docker compose ps

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

  # Guardar cambios locales (si los hay)
  git stash 2>/dev/null || true

  # Obtener última versión
  info "Descargando cambios del repositorio..."
  git pull origin main

  # Reconstruir y reiniciar
  info "Reconstruyendo contenedores..."
  docker compose up -d --build

  info "Verificando estado..."
  docker compose ps

  echo ""
  info "✓ SIGEA actualizado correctamente."
  info "  Frontend: $(frontend_url)"

else
  echo "Uso: bash deploy.sh [install|update]"
  exit 1
fi
