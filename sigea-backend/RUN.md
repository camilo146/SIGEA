# Ejecutar el backend

## Puerto por defecto (8080)
```bash
mvn spring-boot:run
```

## Otro puerto (ej. 8082) en PowerShell
En PowerShell el argumento `-D` debe ir entre comillas para que Maven lo interprete bien:

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8082"
```

Si usas el perfil `altport2` (definido en `application.properties` o perfiles):

```powershell
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=altport2"
```

## En CMD (no PowerShell)
```cmd
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

## Configurar Gmail SMTP para SIGEA

SIGEA usa SMTP nativo de Spring Boot, así que no necesita APIs de pago.

1. En la cuenta sigeasena@gmail.com activa la verificación en dos pasos.
2. Genera una contraseña de aplicación para Correo.
3. Configura estas variables antes de iniciar el backend:

```powershell
$env:SIGEA_SMTP_HOST="smtp.gmail.com"
$env:SIGEA_SMTP_PORT="587"
$env:SIGEA_SMTP_USERNAME="sigeasena@gmail.com"
$env:SIGEA_SMTP_PASSWORD="TU_CONTRASENA_DE_APLICACION"
$env:SIGEA_SMTP_AUTH="true"
$env:SIGEA_SMTP_STARTTLS="true"
$env:SIGEA_SMTP_SSL_TRUST="smtp.gmail.com"
$env:SIGEA_MAIL_FROM_NAME="SIGEA SENA"
$env:SIGEA_AUTH_REQUIRE_EMAIL_VERIFICATION="true"
```

4. Inicia el backend.
5. Valida el envío con un administrador autenticado usando:

```http
POST /api/v1/notificaciones/probar-correo
Authorization: Bearer <token-admin>
Content-Type: application/json

{
	"destinatario": "tu-correo@dominio.com"
}
```

Si no envías `destinatario`, se usa el correo del administrador autenticado.
