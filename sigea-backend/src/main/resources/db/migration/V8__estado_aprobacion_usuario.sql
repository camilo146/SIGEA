-- ==============================================================================
-- V8: Estado de aprobación de usuario (flujo de aprobación por administrador)
-- ==============================================================================
-- Objetivo:
--   Los usuarios que se registran por su cuenta quedan en estado PENDIENTE.
--   El administrador puede APROBAR o RECHAZAR el registro.
--   Si es rechazado, el registro es eliminado físicamente.
--   Si lleva más de 1 día sin acción, una tarea programada lo elimina.
--   Los usuarios creados directamente por el admin quedan en APROBADO.
-- ==============================================================================

ALTER TABLE usuario
    ADD COLUMN estado_aprobacion VARCHAR(10) NOT NULL DEFAULT 'APROBADO'
        COMMENT 'PENDIENTE=esperando aprovacion admin | APROBADO=acceso permitido | RECHAZADO=acceso denegado (no se usa: se elimina)';

-- Los superadmins ya existentes deben tener APROBADO (el DEFAULT ya los cubre).
-- No es necesario ningún UPDATE adicional.
