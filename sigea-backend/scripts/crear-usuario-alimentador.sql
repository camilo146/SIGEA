-- ============================================================
-- Crear usuario con rol ALIMENTADOR_EQUIPOS en SIGEA.
-- Ejecutar directamente en la base de datos MariaDB del servidor.
--
-- Contraseña temporal: password
-- (Cámbiala desde la app después de entrar.)
--
-- Ajusta nombre, documento, correo y teléfono si lo necesitas.
-- ============================================================

-- Cómo ejecutar desde el servidor (Docker):
--   sudo docker exec -i sigea-db mariadb -uroot -pTU_ROOT_PASSWORD sigea_db < scripts/crear-usuario-alimentador.sql
--
-- O conectarte directamente y pegar el INSERT debajo.
-- ============================================================

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
    estado_aprobacion,
    intentos_fallidos,
    cuenta_bloqueada_hasta,
    fecha_creacion,
    fecha_actualizacion
) VALUES (
    'Alimentador SIGEA',
    'CC',
    '11111111',
    'alimentador@sigea.local',
    NULL,
    NULL,
    NULL,
    -- Hash BCrypt (strength 10) de la contraseña: alimentador2026
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOa3Zx0EsBGnCVAFRvRkJiX0JGWzn0yCi',
    'ALIMENTADOR_EQUIPOS',
    0,
    1,
    'APROBADO',
    0,
    NULL,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    nombre_completo = VALUES(nombre_completo),
    telefono = VALUES(telefono),
    programa_formacion = VALUES(programa_formacion),
    ficha = VALUES(ficha),
    contrasena_hash = VALUES(contrasena_hash),
    rol = 'ALIMENTADOR_EQUIPOS',
    es_super_admin = 0,
    activo = 1,
    estado_aprobacion = 'APROBADO',
    intentos_fallidos = 0,
    cuenta_bloqueada_hasta = NULL,
    fecha_actualizacion = NOW();

-- ============================================================
-- NOTAS IMPORTANTES:
--
-- 1. Si el correo 'alimentador@sigea.local' ya existe, cambia el
--    correo y el numero_documento por valores únicos.
--
-- 2. Contraseña del hash: "password"
--    Para usar otra contraseña, genera un hash BCrypt strength 10
--    con cualquiera de estos métodos:
--
--    a) Desde Java/Spring (en un test unitario):
--       new BCryptPasswordEncoder().encode("nueva_contraseña")
--
--    b) Online (solo para pruebas locales):
--       https://bcrypt-generator.com  (rounds: 10)
--
--    c) Desde el servidor con Python:
--       python3 -c "import bcrypt; print(bcrypt.hashpw(b'nueva_contraseña', bcrypt.gensalt(10)).decode())"
--
-- 3. Para crear múltiples usuarios alimentadores, duplica el
--    INSERT cambiando correo y numero_documento (deben ser únicos).
-- ============================================================
