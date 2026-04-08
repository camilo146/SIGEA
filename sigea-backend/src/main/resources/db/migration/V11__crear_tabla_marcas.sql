-- =====================================================================
-- V11: Tabla de marcas de equipos
-- =====================================================================
-- Permite catalogar las marcas de los equipos del inventario.
-- El campo marca del equipo pasa de texto libre a referencia FK.
-- =====================================================================

CREATE TABLE marca (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    descripcion     TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_marca_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices de rendimiento
CREATE INDEX idx_marca_activo ON marca(activo);
