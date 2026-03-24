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
