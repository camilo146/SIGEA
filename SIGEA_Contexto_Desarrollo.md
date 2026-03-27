# SIGEA - Contexto de Desarrollo y Operacion (Actualizado)

> Documento maestro para retomar trabajo tecnico (backend, frontend, infraestructura y despliegue).
> Si se pierde el hilo con Copilot, abrir este archivo y pedir: "lee SIGEA_Contexto_Desarrollo.md y continua desde el estado actual".

---

## 1) Identidad del Proyecto

- Proyecto: SIGEA (Sistema Integral de Gestion de Equipos y Activos)
- Institucion: SENA - Centro Industrial de Mantenimiento Integral (CIMI)
- Responsable: Camilo Lopez Romero
- Repositorio: https://github.com/camilo146/SIGEA.git
- Rama principal: `main`

Objetivo: controlar inventario, prestamos, reservas, mantenimientos, usuarios y trazabilidad de activos de ambientes de formacion.

---

## 2) Stack Tecnologico Real (Estado Actual)

### Backend

- Java 21
- Spring Boot 3.5.10
- Spring Security + JWT
- Spring Data JPA + Hibernate
- Flyway para migraciones
- MariaDB JDBC
- Swagger/OpenAPI (springdoc)
- Maven 3.9.x

### Frontend

- Angular 18.2.x
- RxJS 7.8.x
- Chart.js 4.x
- Nginx (contenedor) para servir build y proxear API

### Infraestructura / Runtime

- Docker Engine + Docker Compose plugin v2
- Docker Compose con 3 servicios: `db`, `backend`, `frontend`
- Despliegue objetivo validado en Rocky Linux (entorno servidor)

---

## 3) Arquitectura de Despliegue en Produccion

Topologia actual:

1. `frontend` (Nginx) expone puerto host `4043` -> contenedor `80`
2. `frontend` proxea `/api/v1/` a `backend:8080/api/v1/`
3. `backend` NO expone puerto al host (solo red interna Docker)
4. `backend` se conecta a `db` (`mariadb:11.4`) por `db:3306`
5. `db` expone `3305` en host para administracion/diagnostico

Seguridad de red actual:

- API no publicada directamente al host
- Entrada principal por frontend Nginx
- JWT para endpoints protegidos
- Healthchecks en `db` y `backend`
- `frontend` depende de `backend` en estado healthy

---

## 4) Estado de Funcionalidad del Sistema

### Backend completado

- Persistencia y base del dominio (entidades, enums, repositories)
- Manejo global de excepciones (400/403/404/409)
- Seguridad JWT y autenticacion
- Registro y login
- Verificacion de email por codigo de 6 digitos
- Usuarios (CRUD admin, roles, activar/desactivar, desbloqueo)
- Ambientes (CRUD)
- Equipos (CRUD + fotos + estados + baja/activar)
- Prestamos (solicitud/aprobacion/rechazo/salida/devolucion)
- Reservas (creacion, cancelacion, expiracion automatica)
- Notificaciones y tareas programadas
- Auditoria
- Mantenimientos (crear/editar/cerrar/eliminar con reglas)

### Frontend completado (modulos principales)

- Login/registro
- Guardado de sesion JWT
- Rutas protegidas por rol
- Inventario
- Ambientes
- Reservas
- Prestamos
- Mantenimientos
- Usuarios
- Notificaciones

### Pendientes principales

- Reportes (backend + frontend, cierre funcional)
- Transferencias entre ambientes (cierre funcional)
- Dashboard de metricas (cierre funcional)
- Mejoras UX puntuales en algunos listados

---

## 5) Estado Operativo de Login (Importante)

El endpoint de login activo es:

- `POST /api/v1/auth/login`

Body correcto:

```json
{
  "correoElectronico": "usuario@dominio",
  "contrasena": "Clave123*"
}
```

Notas criticas:

- El campo es `correoElectronico` (no `correo`).
- Respuesta `403` en login puede significar:
  - credenciales invalidas
  - cuenta bloqueada por intentos
  - cuenta pendiente de aprobacion
  - email no verificado (si aplica)

Frontend actualizado para mostrar el mensaje real de backend en errores de login (en lugar de mensaje generico).

---

## 6) Cambios Recientes Clave (Linea de Tiempo Tecnica)

Commits recientes en `main`:

- `0fa396b` - Frontend login muestra mensaje real del backend
- `01d49fa` - Login evita 404 para usuario inexistente (responde flujo de autenticacion)
- `432a803` - Deploy: healthcheck backend + dependencia healthy + proxy `/api/v1/`
- `3e7cbe4` - Dockerfile backend: `dependency:resolve` en vez de `go-offline`
- `08556ae` - Security/CORS: permitir `SIGEA_APP_URL` como origen
- `6133c90` - Docker build backend robustecido ante fallos de red Maven
- `7318152` - Hardening deploy Rocky Linux + puerto 4043 + backend interno

Impacto de estos cambios:

1. Se redujeron errores 502 por arranque desordenado
2. Se corrigio CORS para entorno por IP/puerto del servidor
3. Se acelero y estabilizo build de backend en redes lentas
4. Se mejoro trazabilidad real de errores de login en UI

---

## 7) Configuracion de Produccion (.env)

Variables criticas:

- `FRONTEND_PORT=4043`
- `SIGEA_APP_URL=http://<ip-servidor>:4043` (actualmente en uso HTTP)
- `SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION=false` (modo operativo inicial)
- `SPRING_DATASOURCE_URL=jdbc:mariadb://db:3306/sigea_db?...`
- `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`
- `MARIADB_ROOT_PASSWORD` y credenciales de app

Observacion:

- Cambiar solo puerto NO cifra credenciales. Para cifrado real en transito se requiere HTTPS/TLS.

---

## 8) Comandos Operativos Estandar (Runbook)

### Despliegue/actualizacion

```bash
cd /opt/sigea
git pull origin main
docker compose down
docker compose up -d --build
docker compose ps
```

### Logs utiles

```bash
sudo docker compose logs --tail=200 backend
sudo docker compose logs --tail=200 frontend
sudo docker compose logs --tail=200 db
```

### Healthcheck API desde host

```bash
curl -i http://localhost:4043/api/v1/actuator/health
```

### Prueba login por API

```bash
curl -i -X POST http://localhost:4043/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correoElectronico":"admin@dominio","contrasena":"Clave123*"}'
```

---

## 9) Incidencias Reales Resueltas (Produccion/Preproduccion)

### A) Fallo build Maven dentro de Docker (entorno Rocky)

Sintoma:

- Build se quedaba mucho tiempo o fallaba en descarga de artefactos/plugins

Acciones aplicadas:

- Dockerfile backend con reintentos
- `maven.test.skip=true` en build
- prefetch de dependencias con `dependency:resolve`

Resultado:

- Build mas predecible y rapido en red inestable

### B) 502 en login

Causa:

- frontend intentaba proxear cuando backend aun no estaba disponible

Fix:

- `backend` healthcheck + `frontend` depende de `service_healthy`

### C) 403 por CORS

Causa:

- origen servidor (IP:4043) no permitido

Fix:

- CORS toma `sigea.app.url` (de `SIGEA_APP_URL`) como origen permitido

### D) 404/errores ambiguos en login

Mejoras:

- Backend unifico flujo de error de autenticacion para no exponer existencia de cuentas
- Frontend muestra mensaje real de backend para diagnostico funcional

---

## 10) Estado Actual del Problema de Credenciales Admin

Estado observado en entorno servidor:

- API responde correctamente
- Login retorna `403 Credenciales invalidas`

Interpretacion tecnica:

- El problema ya no es red, proxy, CORS ni disponibilidad
- Es dato/autenticacion: usuario, hash o estado de cuenta

Procedimiento recomendado:

1. Generar hash BCrypt conocido
2. Insert/Upsert de admin de rescate en `usuario`
3. Forzar estado valido: `ADMINISTRADOR`, `activo=1`, `APROBADO`, `email_verificado=1`, `intentos=0`, `cuenta_bloqueada_hasta=NULL`
4. Validar login por `curl`

---

## 11) Estructura de Documentacion del Proyecto

- Requerimientos: `SIGEA_Documento_Requerimientos.md`
- Diseno y diagramas: `SIGEA_Diagramas_Diseno.md`
- Contexto operativo/desarrollo: `SIGEA_Contexto_Desarrollo.md` (este archivo)
- Guia despliegue servidor: `DESPLIEGUE_SENA.md`

---

## 12) Checklist Rapido para Retomar Desarrollo

Antes de continuar cualquier modulo:

1. `git pull origin main`
2. `docker compose up -d --build`
3. validar `GET /api/v1/actuator/health`
4. validar login via API
5. revisar `docker compose logs --tail=150 backend`

Si estos 5 pasos pasan, el entorno esta listo para continuar desarrollo funcional.

---

## 13) Proximas Prioridades Sugeridas

1. Cerrar modulo Reportes (Excel/PDF) end-to-end
2. Cerrar modulo Transferencias end-to-end
3. Consolidar Dashboard con metricas y filtros
4. Endurecer seguridad de produccion con HTTPS real
5. Generar usuario admin de respaldo documentado (runbook oficial)

---

## 14) Nota de Version del Contexto

- Fecha de actualizacion: 27 de marzo de 2026
- Estado: documento consolidado con operacion real de despliegue, fixes de infraestructura y comportamiento actual de autenticacion/login.
