# SIGEA - Frontend (Angular)

Frontend del **Sistema Integral de Gestión de Equipos y Activos** (SIGEA). Desarrollado con **Angular 18**, según documento de requerimientos.

## Requisitos

- Node.js 18+
- npm

## Instalación

```bash
npm install
```

## Desarrollo

1. **Iniciar el backend** (Spring Boot) en `http://localhost:8080` desde la raíz del monorepo:
   ```bash
   cd ../sigea-backend && mvn spring-boot:run
   ```

2. **Iniciar el frontend** (el proxy redirige `/api` al backend):
   ```bash
   npm start
   ```

3. Abrir **http://localhost:4200**. La API se llama a través del proxy en `http://localhost:4200/api/v1/...`.

## Credenciales de prueba

- **Correo:** `admin@sena.edu.co`
- **Contraseña:** `Admin2026!`

## Estructura del proyecto

- **`src/app/core`**: Auth (servicio, guards, interceptor JWT), modelos, servicios compartidos (dashboard).
- **`src/app/layout`**: Layout principal (header, sidebar, menú usuario, notificaciones).
- **`src/app/pages`**: Páginas por módulo: login, dashboard, inventario, préstamos, reservas, ambientes, usuarios, reportes, transferencias, mantenimientos.
- **`src/environments`**: `apiUrl` (desarrollo usa proxy `/api/v1`, producción `/api/v1`).

## Build para producción

```bash
ng build
```

Los artefactos se generan en `dist/sigea-frontend/`. En producción, configurar el servidor (p. ej. Nginx) para servir estos estáticos y hacer proxy de `/api/v1` al backend.

## Funcionalidades implementadas

- Login con JWT (correo + contraseña).
- Dashboard con estadísticas desde `GET /dashboard/estadisticas` (total equipos, disponibles, prestados, en mora, categorías, ambientes, usuarios, reservas, mantenimientos, stock bajo).
- Navegación por rol: **Administrador** ve Inventario, Préstamos, Reservas, Ambientes, Usuarios, Transferencias, Mantenimientos, Reportes; **Usuario estándar** ve Dashboard, Inventario (como “Equipos disponibles”), Préstamos (Mis préstamos), Reservas (Mis reservas).
- Placeholders para Inventario, Préstamos, Reservas, Ambientes, Usuarios, Reportes, Transferencias y Mantenimientos (listos para conectar con los endpoints del backend).
