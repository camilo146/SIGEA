-- ============================================================================
-- Crear/actualizar usuario ADMINISTRADOR en SIGEA (idempotente).
-- ============================================================================
-- Ejecución recomendada en servidor Docker:
--   sudo docker exec -i sigea-db mariadb -uroot -pTU_ROOT_PASSWORD sigea_db < scripts/crear-usuario-admin.sql
--
-- Credenciales por defecto:
--   correo: admin2@sigea.local
--   contraseña: password
-- ============================================================================

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
) VALUES (
    'Administrador SIGEA',
    'CC',
    '999999999',
    'admin2@sigea.local',
    NULL,
    NULL,
    NULL,
    '$2y$10$ciOUrt4OBKgPDFPimsQXfOdvQwmN.qEGl0YOpc7E/yRo5CgmGaj0G',
    'ADMINISTRADOR',
    0,
    1,
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
    rol = 'ADMINISTRADOR',
    es_super_admin = VALUES(es_super_admin),
    activo = 1,
    email_verificado = 1,
    estado_aprobacion = 'APROBADO',
    intentos_fallidos = 0,
    cuenta_bloqueada_hasta = NULL,
    fecha_actualizacion = NOW();

-- Verificación rápida:
-- SELECT id, nombre_completo, correo_electronico, rol, activo, estado_aprobacion
-- FROM usuario WHERE correo_electronico = 'admin2@sigea.local';
