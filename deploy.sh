#!/usr/bin/env bash
# ==============================================================================
# SIGEA - Script de despliegue para servidor SENA
# ==============================================================================
# Uso:
#   Primera vez:  bash deploy.sh
#   Actualizar:   bash deploy.sh update
# ==============================================================================

set -euo pipefail

REPO_URL="https://github.com/camilo146/SIGEA.git"
APP_DIR="/opt/sigea"
ACTION="${1:-install}"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[SIGEA]${NC} $1"; }
warning() { echo -e "${YELLOW}[WARN]${NC}  $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ── Verificar Docker ────────────────────────────────────────────────────────────
command -v docker >/dev/null 2>&1 || error "Docker no está instalado."
docker compose version >/dev/null 2>&1 || error "Docker Compose plugin no encontrado. Instálalo con: sudo apt install docker-compose-plugin"

# ══════════════════════════════════════════════════════════════════════════════
# INSTALACIÓN (primera vez)
# ══════════════════════════════════════════════════════════════════════════════
if [ "$ACTION" = "install" ]; then

  info "Instalando SIGEA en $APP_DIR ..."

  # Crear directorio
  sudo mkdir -p "$APP_DIR"
  sudo chown "$USER:$USER" "$APP_DIR"

  # Clonar repositorio
  if [ -d "$APP_DIR/.git" ]; then
    warning "El directorio ya existe. Usa 'bash deploy.sh update' para actualizar."
    exit 0
  fi

  info "Clonando repositorio..."
  git clone "$REPO_URL" "$APP_DIR"
  cd "$APP_DIR"

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

  # Construir y levantar
  info "Construyendo imágenes y levantando contenedores (puede tardar 5-10 min la primera vez)..."
  docker compose up -d --build

  # Verificar
  info "Esperando que los contenedores inicien..."
  sleep 15
  docker compose ps

  echo ""
  info "✓ SIGEA instalado correctamente."
  info "  Frontend: http://$(hostname -I | awk '{print $1}'):${FRONTEND_PORT:-8083}"
  info "  Backend:  http://$(hostname -I | awk '{print $1}'):8080"
  echo ""
  info "Credenciales por defecto: admin@sigea.edu.co / Admin2026*"

# ══════════════════════════════════════════════════════════════════════════════
# ACTUALIZACIÓN
# ══════════════════════════════════════════════════════════════════════════════
elif [ "$ACTION" = "update" ]; then

  [ -d "$APP_DIR/.git" ] || error "SIGEA no está instalado en $APP_DIR. Ejecuta: bash deploy.sh"
  cd "$APP_DIR"

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

else
  echo "Uso: bash deploy.sh [install|update]"
  exit 1
fi
