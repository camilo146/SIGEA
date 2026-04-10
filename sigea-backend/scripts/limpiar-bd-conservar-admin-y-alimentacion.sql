-- SIGEA - Limpieza total de datos operativos conservando 2 usuarios base
-- Motor: MariaDB
-- Conserva únicamente los usuarios cuyos correos se indican abajo.
-- Ajusta estas variables si en tu entorno esos correos son distintos.

SET @correo_admin = 'admin2@sigea.local';
SET @correo_alimentacion = 'alimentacion@sigea.local';

DROP TEMPORARY TABLE IF EXISTS usuarios_preservados;
CREATE TEMPORARY TABLE usuarios_preservados AS
SELECT id, correo_electronico
FROM usuario
WHERE correo_electronico IN (@correo_admin, @correo_alimentacion);

START TRANSACTION;
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM reporte_dano WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM observacion_equipo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM extension_prestamo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM detalle_prestamo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM prestamo_ambiente WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM prestamo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM reserva WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM transferencia WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM mantenimiento WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM foto_equipo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM equipo WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM ambiente WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM marca WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM categoria WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM notificacion WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;
DELETE FROM log_auditoria WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2;

-- Si también quieres borrar parámetros del sistema, descomenta la línea siguiente.
-- DELETE FROM configuracion;

DELETE FROM usuario
WHERE (SELECT COUNT(*) FROM usuarios_preservados) = 2
    AND id NOT IN (SELECT id FROM usuarios_preservados);

UPDATE usuario
SET activo = TRUE,
    estado_aprobacion = 'APROBADO',
    intentos_fallidos = 0,
    cuenta_bloqueada_hasta = NULL,
    token_verificacion = NULL,
    token_verificacion_expira = NULL
WHERE id IN (SELECT id FROM usuarios_preservados);

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

SELECT COUNT(*) AS usuarios_preservados_encontrados FROM usuarios_preservados;

SELECT id, nombre_completo, correo_electronico, rol, es_super_admin, activo
FROM usuario
ORDER BY id;