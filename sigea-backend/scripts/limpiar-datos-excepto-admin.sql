-- ==============================================================================
-- SIGEA - Script de limpieza de datos
-- Elimina TODA la información de la BD, conservando únicamente:
--   1. El usuario super-administrador: admin2@sigea.local
--   2. Las categorías de inventario por defecto (V3)
--   3. Las configuraciones del sistema (V1 seed data)
-- ==============================================================================
-- ADVERTENCIA: Este script es IRREVERSIBLE.
-- Ejecutar únicamente en entorno de desarrollo/pruebas.
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------
-- 1. Tablas derivadas de préstamos
-- -----------------------------------------------------------------------
TRUNCATE TABLE reporte_dano;
TRUNCATE TABLE extension_prestamo;
TRUNCATE TABLE detalle_prestamo;
TRUNCATE TABLE prestamo;

-- -----------------------------------------------------------------------
-- 2. Reservas
-- -----------------------------------------------------------------------
TRUNCATE TABLE reserva;

-- -----------------------------------------------------------------------
-- 3. Transferencias
-- -----------------------------------------------------------------------
TRUNCATE TABLE transferencia;

-- -----------------------------------------------------------------------
-- 4. Mantenimientos
-- -----------------------------------------------------------------------
TRUNCATE TABLE mantenimiento;

-- -----------------------------------------------------------------------
-- 5. Fotos de equipos y equipos
-- -----------------------------------------------------------------------
TRUNCATE TABLE foto_equipo;
TRUNCATE TABLE equipo;

-- -----------------------------------------------------------------------
-- 6. Ambientes / Ubicaciones
-- -----------------------------------------------------------------------
TRUNCATE TABLE ambiente;

-- -----------------------------------------------------------------------
-- 7. Notificaciones y auditoría
-- -----------------------------------------------------------------------
TRUNCATE TABLE notificacion;
TRUNCATE TABLE log_auditoria;

-- -----------------------------------------------------------------------
-- 8. Usuarios (conservar solo el super-administrador)
-- -----------------------------------------------------------------------
DELETE FROM usuario
WHERE correo_electronico != 'admin2@sigea.local';

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------
-- Verificación final
-- -----------------------------------------------------------------------
SELECT 'Usuarios restantes:' AS info, COUNT(*) AS total FROM usuario;
SELECT id, nombre_completo, correo_electronico, rol, es_super_admin, activo
FROM usuario;

SELECT 'Categorias (conservadas):' AS info, COUNT(*) AS total FROM categoria;
SELECT 'Configuraciones (conservadas):' AS info, COUNT(*) AS total FROM configuracion;

SELECT 'Equipos:' AS info, COUNT(*) AS total FROM equipo;
SELECT 'Prestamos:' AS info, COUNT(*) AS total FROM prestamo;
SELECT 'Reservas:' AS info, COUNT(*) AS total FROM reserva;
SELECT 'Mantenimientos:' AS info, COUNT(*) AS total FROM mantenimiento;
SELECT 'Transferencias:' AS info, COUNT(*) AS total FROM transferencia;
SELECT 'Ambientes:' AS info, COUNT(*) AS total FROM ambiente;
