-- Restaurar rol ADMINISTRADOR a un usuario que quedó con rol APRENDIZ (o otro).
-- Ejecutar en la base de datos que usa la aplicación (MariaDB/MySQL).
--
-- OPCIÓN 1: Por correo (reemplaza con el correo del administrador)
-- UPDATE usuario SET rol = 'ADMINISTRADOR' WHERE correo_electronico = 'tu-admin@ejemplo.com';

-- OPCIÓN 2: Restaurar rol a quien tenga la bandera de super admin
UPDATE usuario SET rol = 'ADMINISTRADOR' WHERE es_super_admin = 1;

-- Verificar (opcional): listar id, correo y rol
-- SELECT id, correo_electronico, rol, es_super_admin FROM usuario;
