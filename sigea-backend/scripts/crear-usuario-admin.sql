-- Crear un segundo usuario administrador en SIGEA.
-- Ejecutar en la base de datos que usa la aplicación (MariaDB/MySQL).
--
-- Contraseña temporal: password
-- (Cámbiala desde la app después de entrar.)
--
-- Ajusta nombre, documento, correo y teléfono si lo necesitas.

INSERT INTO usuario (
    nombre_completo,
    tipo_documento,
    numero_documento,
    correo_electronico,
    telefono,
    programa_formacion,
    ficha,
    contrasena_hash,
    rol,
    es_super_admin,
    activo,
    intentos_fallidos,
    cuenta_bloqueada_hasta,
    fecha_creacion,
    fecha_actualizacion
) VALUES (
    'Administrador SIGEA',
    'CC',
    '999999999',
    'admin2@sigea.local',
    NULL,
    NULL,
    NULL,
    '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPGga31lW',
    'ADMINISTRADOR',
    0,
    1,
    0,
    NULL,
    NOW(),
    NOW()
);

-- Si el correo ya existe, cambia 'admin2@sigea.local' por otro único.
-- Contraseña del hash anterior: "password" (cámbiala al entrar).
-- Para otra contraseña, genera un hash BCrypt (strength 10) y reemplaza contrasena_hash.
