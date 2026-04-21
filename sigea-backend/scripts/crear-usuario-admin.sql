-- ============================================================================
-- Crear/actualizar usuario ADMINISTRADOR en SIGEA (idempotente).
-- ============================================================================
-- Ejecución recomendada en servidor Docker:
--   sudo docker exec -i sigea-db mariadb -uroot -pTU_ROOT_PASSWORD sigea_db < scripts/crear-usuario-admin.sql
--
-- Credenciales por defecto:
--   documento: 999999999
--   contraseña: password
-- ============================================================================
-- NOTA: Se usan UPDATE + INSERT separados en lugar de ON DUPLICATE KEY UPDATE
-- para evitar el comportamiento ambiguo de MariaDB cuando múltiples claves UNIQUE
-- colisionan con filas distintas (lo que puede actualizar el usuario equivocado).
-- ============================================================================

-- Paso 1: Liberar el correo admin2@sigea.local si está en otro usuario (distinto documento).
-- Esto evita violación de UNIQUE en el INSERT posterior.
UPDATE usuario
SET correo_electronico = CONCAT('_conflict_', id, '@sigea.invalid')
WHERE correo_electronico = 'admin2@sigea.local'
  AND numero_documento != '999999999';

-- Paso 2: Actualizar todos los campos del admin si ya existe por numero_documento.
UPDATE usuario SET
    nombre_completo       = 'Administrador SIGEA',
    tipo_documento        = 'CC',
    correo_electronico    = 'admin2@sigea.local',
    contrasena_hash       = '$2b$10$pM4aX9wYVhPAKzysl3cY9.sBDLSHOya8mscT02Fc786RLGunWVKEG',
    rol                   = 'ADMINISTRADOR',
    es_super_admin        = 0,
    activo                = 1,
    email_verificado      = 1,
    estado_aprobacion     = 'APROBADO',
    intentos_fallidos     = 0,
    cuenta_bloqueada_hasta = NULL,
    fecha_actualizacion   = NOW()
WHERE numero_documento = '999999999';

-- Paso 3: Insertar el admin solo si no existe por numero_documento.
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
    email_verificado,
    estado_aprobacion,
    intentos_fallidos,
    cuenta_bloqueada_hasta,
    fecha_creacion,
    fecha_actualizacion
)
SELECT
    'Administrador SIGEA',
    'CC',
    '999999999',
    'admin2@sigea.local',
    NULL,
    NULL,
    NULL,
    '$2b$10$pM4aX9wYVhPAKzysl3cY9.sBDLSHOya8mscT02Fc786RLGunWVKEG',
    'ADMINISTRADOR',
    0,
    1,
    1,
    'APROBADO',
    0,
    NULL,
    NOW(),
    NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM usuario WHERE numero_documento = '999999999'
);

-- Verificación rápida:
-- SELECT id, nombre_completo, numero_documento, correo_electronico, rol, activo, estado_aprobacion, intentos_fallidos, cuenta_bloqueada_hasta
-- FROM usuario WHERE numero_documento = '999999999';
