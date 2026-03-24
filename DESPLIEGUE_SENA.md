# SIGEA - Flujo recomendado: Docker -> GitHub -> Servidor SENA

## 1) Probar local con Docker

Requisito: Docker Desktop iniciado.

En la raiz del proyecto:

```powershell
docker compose up -d --build
```

Ver estado:

```powershell
docker compose ps
```

Ver logs:

```powershell
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

URLs de prueba:
- Frontend: http://localhost:8083
- Backend API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/api/v1/swagger-ui.html

Apagar servicios:

```powershell
docker compose down
```

Apagar y borrar volumenes de datos (reinicio total):

```powershell
docker compose down -v
```

## 2) Subir a GitHub

Configurar remoto (si aun no existe):

```powershell
git remote add origin https://github.com/TU_ORG_O_USUARIO/SIGEA_SENA.git
```

Crear rama de despliegue:

```powershell
git checkout -b deploy/docker-sena
```

Agregar cambios:

```powershell
git add docker-compose.yml sigea-backend/Dockerfile sigea-backend/.dockerignore sigea-frontend/Dockerfile sigea-frontend/nginx.conf sigea-frontend/.dockerignore DESPLIEGUE_SENA.md
```

Commit:

```powershell
git commit -m "chore: dockerizacion completa para prueba local y despliegue"
```

Push:

```powershell
git push -u origin deploy/docker-sena
```

Crear Pull Request hacia `main`.

## 3) Despliegue en servidor SENA (Linux)

### 3.1 Instalar Docker y Compose plugin

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git
sudo systemctl enable docker
sudo systemctl start docker
```

2007### 3.2 Clonar repositorio

```bash
git clone https://github.com/TU_ORG_O_USUARIO/SIGEA_SENA.git
cd SIGEA_SENA
```

### 3.3 Variables seguras para produccion

Crear archivo `.env` en raiz (ejemplo):

```env
MARIADB_DATABASE=sigea_db
MARIADB_USER=sigea_user
MARIADB_PASSWORD=CAMBIAR_PASSWORD_DB
MARIADB_ROOT_PASSWORD=CAMBIAR_ROOT_DB
SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/sigea_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=America/Bogota
SPRING_DATASOURCE_USERNAME=sigea_user
SPRING_DATASOURCE_PASSWORD=CAMBIAR_PASSWORD_DB
SIGEA_UPLOADS_PATH=/app/uploads
SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION=true
SIGEA_SMTP_HOST=SMTP_REAL
SIGEA_SMTP_PORT=587
SIGEA_SMTP_USERNAME=correo@sena.edu.co
SIGEA_SMTP_PASSWORD=CAMBIAR_SMTP_PASSWORD
SIGEA_SMTP_AUTH=true
SIGEA_SMTP_STARTTLS=true
SIGEA_APP_URL=https://DOMINIO_SIGEA
```

Recomendacion: no subir `.env` al repositorio.

### 3.4 Levantar en servidor

```bash
docker compose up -d --build
```

### 3.5 Actualizar version en servidor

```bash
git pull
docker compose up -d --build
```

## 4) Recomendaciones de produccion

- Usar dominio interno institucional y TLS (nginx reverse proxy o traefik).
- Cambiar credenciales por defecto de DB y JWT.
- Habilitar backup de volumen `sigea_db_data` y `sigea_uploads_data`.
- Mantener estrategia de ramas: `main` estable, `develop` para cambios.
- Monitorear logs con `docker compose logs -f` y alertas de salud.
