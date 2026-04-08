# Guía Completa de Despliegue SIGEA en Servidor del SENA

## Requisitos del Servidor

### Hardware Mínimo Recomendado
- **CPU**: 2 núcleos
- **RAM**: 4 GB
- **Disco**: 20 GB SSD
- **SO**: Rocky Linux 8/9 (recomendado) o Ubuntu 20.04+ (Linux)

### Software Requerido
- **Docker**: 24.0+
- **Docker Compose**: 2.0+ (plugin integrado)
- **Git**: 2.25+
- **Acceso SSH**: Con clave privada

**Nota importante**: No necesitas instalar Java, Maven, Node.js ni ninguna otra tecnología en el servidor. Todo está containerizado en Docker.

## Paso a Paso de Despliegue

### Paso 1: Preparación del Servidor

#### 1.1 Conectar al Servidor
```bash
ssh usuario@ip_del_servidor
```

#### 1.2 Actualizar el Sistema (Rocky Linux)
```bash
sudo dnf update -y
```

#### 1.3 Instalar Dependencias Básicas
```bash
sudo dnf install -y curl wget git vim htop
```

#### 1.4 Instalar Docker (Rocky Linux)
```bash
# Remover versiones anteriores si existen
sudo dnf remove -y docker docker-client docker-client-latest docker-common docker-latest docker-latest-logrotate docker-logrotate docker-engine podman-docker runc

# Habilitar utilidades de repositorio
sudo dnf install -y dnf-plugins-core

# Agregar repositorio oficial de Docker
sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

# Instalar Docker Engine + Compose plugin
sudo dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Iniciar y habilitar Docker
sudo systemctl enable docker
sudo systemctl start docker

# Verificar instalación
docker --version
```

#### 1.5 Verificar Docker Compose (Plugin)
```bash
docker compose version
```

#### 1.6 Configurar Usuario para Docker (Opcional pero recomendado)
```bash
# Agregar usuario actual al grupo docker
sudo usermod -aG docker $USER

# Aplicar cambios (requiere logout/login)
newgrp docker
```

#### 1.7 Configurar Firewall (UFW)
```bash
# Habilitar UFW
sudo ufw enable

# Permitir SSH
sudo ufw allow ssh

# Permitir puertos de la aplicación
sudo ufw allow 80
sudo ufw allow 443
sudo ufw allow 4043  # Frontend

# Verificar estado
sudo ufw status
```

### Paso 2: Clonar y Configurar el Proyecto

#### 2.1 Clonar el Repositorio
```bash
cd /home/usuario  # O directorio preferido
git clone https://github.com/camilo146/SIGEA.git
cd SIGEA
```

#### 2.2 Crear Archivo de Variables de Entorno
```bash
# Crear archivo .env con configuraciones de producción
nano .env
```

Contenido del archivo `.env`:
```env
# Base de Datos
MARIADB_DATABASE=sigea_db
MARIADB_USER=sigea_user
MARIADB_PASSWORD=tu_password_segura_db
MARIADB_ROOT_PASSWORD=tu_root_password_segura

# Spring Boot
SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
SPRING_DATASOURCE_USERNAME=sigea_user
SPRING_DATASOURCE_PASSWORD=tu_password_segura_db
SPRING_PROFILES_ACTIVE=prod

# JWT y Seguridad
JWT_SECRET=tu_jwt_secret_muy_seguro_de_al_menos_256_bits
SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION=true

# Correo Electrónico (SMTP)
SIGEA_SMTP_HOST=smtp.gmail.com
SIGEA_SMTP_PORT=587
SIGEA_SMTP_USERNAME=tu_correo@gmail.com
SIGEA_SMTP_PASSWORD=tu_app_password
SIGEA_SMTP_AUTH=true
SIGEA_SMTP_STARTTLS=true

# URLs de la Aplicación
SIGEA_APP_URL=https://tu-dominio-sena.edu.co:4043

# Frontend publicado en el host
FRONTEND_PORT=4043

# Configuración de Archivos
SIGEA_UPLOADS_PATH=/app/uploads
```

**Importante**: Cambia todas las contraseñas por valores seguros. Para Gmail, usa "App Passwords".

#### 2.3 Generar JWT Secret Seguro
```bash
# Generar un secret aleatorio de 256 bits
openssl rand -hex 32
# Copia el resultado al JWT_SECRET en .env
```

### Paso 3: Despliegue Inicial

#### 3.1 Verificar Docker Compose
```bash
# Ver configuración
docker compose config

# Ver servicios
docker compose ps
```

#### 3.2 Iniciar Servicios
```bash
# Construir e iniciar en background
docker compose up -d --build

# Ver logs de construcción
docker compose logs -f
```

#### 3.3 Verificar que Todo Funciona
```bash
# Ver estado de contenedores
docker compose ps

# Ver logs de cada servicio
docker compose logs backend
docker compose logs frontend
docker compose logs db
```

#### 3.4 Probar Conexiones
```bash
# Probar frontend publicado
curl -I http://localhost:4043

# Probar backend desde red interna de Docker
docker compose exec frontend wget -qO- http://backend:8080/actuator/health || true
```

### Paso 4: Configuración de Producción

#### 4.1 Configurar Nginx como Reverse Proxy (Recomendado)
```bash
# Instalar Nginx
sudo dnf install -y nginx

# Crear configuración para SIGEA
sudo nano /etc/nginx/sites-available/sigea
```

Contenido del archivo de configuración:
```nginx
server {
    listen 80;
    server_name tu-dominio-sena.edu.co;

    # Redirect HTTP to HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name tu-dominio-sena.edu.co;

    # SSL Configuration (obtener certificado Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/tu-dominio-sena.edu.co/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/tu-dominio-sena.edu.co/privkey.pem;

    # Frontend (incluye /api hacia backend por proxy interno del contenedor frontend)
    location / {
        proxy_pass http://localhost:4043;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
# Habilitar sitio
sudo ln -s /etc/nginx/sites-available/sigea /etc/nginx/sites-enabled/

# Remover configuración por defecto
sudo rm /etc/nginx/sites-enabled/default

# Probar configuración
sudo nginx -t

# Reiniciar Nginx
sudo systemctl restart nginx
sudo systemctl enable nginx
```

#### 4.2 Obtener Certificado SSL (Let's Encrypt)
```bash
# Instalar Certbot
sudo dnf install -y certbot python3-certbot-nginx

# Obtener certificado
sudo certbot --nginx -d tu-dominio-sena.edu.co

# Configurar renovación automática
sudo crontab -e
# Agregar: 0 12 * * * /usr/bin/certbot renew --quiet
```

### Paso 5: Actualizaciones y Mantenimiento

#### 5.1 Actualizar la Aplicación
```bash
cd /ruta/al/SIGEA

# Obtener cambios
git pull origin main

# Reiniciar servicios con nueva versión
docker compose down
docker compose up -d --build

# Verificar
docker compose ps
```

#### 5.2 Backup de Base de Datos
```bash
# Crear script de backup
nano backup-db.sh
```

Contenido:
```bash
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec sigea-db mysqldump -u root -p"$MARIADB_ROOT_PASSWORD" sigea_db > backup_$DATE.sql
echo "Backup creado: backup_$DATE.sql"
```

```bash
chmod +x backup-db.sh

# Ejecutar backup
./backup-db.sh
```

#### 5.3 Monitoreo
```bash
# Ver logs en tiempo real
docker compose logs -f

# Ver uso de recursos
docker stats

# Ver estado de contenedores
docker compose ps
```

### Paso 6: Solución de Problemas Comunes

#### 6.1 Puerto ya en uso
```bash
# Ver qué proceso usa el puerto
sudo lsof -i :4043

# Matar proceso si es necesario
sudo kill -9 PID
```

#### 6.2 Error de conexión a DB
```bash
# Ver logs de base de datos
docker compose logs db

# Verificar variables de entorno
docker compose exec backend env | grep DB
```

#### 6.3 Error de build
```bash
# Limpiar imágenes y reconstruir
docker compose down
docker system prune -f
docker compose up -d --build
```

#### 6.4 Reinicio completo
```bash
# Detener todo y limpiar
docker compose down -v
docker system prune -f

# Reiniciar desde cero
docker compose up -d --build
```

### Paso 7: Configuración de Usuario Admin

Después del despliegue inicial, el usuario admin se crea automáticamente con:
- **Número de documento**: 999999999
- **Contraseña**: password

**Importante**: Cambia la contraseña inmediatamente después del primer login.

## Checklist Final

- [ ] Servidor actualizado
- [ ] Docker y Docker Compose instalados
- [ ] Repositorio clonado
- [ ] Archivo .env configurado con contraseñas seguras
- [ ] Servicios iniciados correctamente
- [ ] Nginx configurado como reverse proxy
- [ ] Certificado SSL instalado
- [ ] Usuario admin probado
- [ ] Backup configurado
- [ ] Monitoreo activo

## URLs de Acceso

- **Aplicación**: https://tu-dominio-sena.edu.co
- **API Backend**: https://tu-dominio-sena.edu.co/api/v1
- **Swagger**: https://tu-dominio-sena.edu.co/swagger-ui.html

## Contacto de Soporte

Si hay problemas durante el despliegue, contacta al desarrollador con:
- Logs de `docker compose logs`
- Salida de `docker compose ps`
- Contenido del archivo `.env` (sin contraseñas)
