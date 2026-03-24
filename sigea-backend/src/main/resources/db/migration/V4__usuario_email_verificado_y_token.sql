-- ==============================================================================
-- V4: Verificación de email en registro
-- ==============================================================================
-- Campos para marcar si el correo fue verificado y token con expiración para el enlace.
-- ==============================================================================

ALTER TABLE usuario
    ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN token_verificacion VARCHAR(255) NULL,
    ADD COLUMN token_verificacion_expira DATETIME NULL;
