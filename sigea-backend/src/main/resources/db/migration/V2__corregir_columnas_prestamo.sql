-- ==============================================================================
-- V2: Corregir nombres de columnas en tabla prestamo
-- ==============================================================================
-- Las columnas originales usaban nombres cortos (fecha_solicitud, fecha_aprobacion...)
-- pero las entidades JPA usan nombres más descriptivos (fecha_hora_solicitud, etc.)
-- Flyway no permite modificar migraciones ya ejecutadas (V1), así que se crea V2.
-- ==============================================================================

-- Renombrar fecha_solicitud → fecha_hora_solicitud
ALTER TABLE prestamo CHANGE COLUMN fecha_solicitud fecha_hora_solicitud DATETIME NOT NULL;

-- Renombrar fecha_aprobacion → fecha_hora_aprobacion
ALTER TABLE prestamo CHANGE COLUMN fecha_aprobacion fecha_hora_aprobacion DATETIME;

-- Renombrar fecha_entrega → fecha_hora_salida
ALTER TABLE prestamo CHANGE COLUMN fecha_entrega fecha_hora_salida DATETIME;

-- Renombrar fecha_devolucion_esperada → fecha_hora_devolucion_estimada
ALTER TABLE prestamo CHANGE COLUMN fecha_devolucion_esperada fecha_hora_devolucion_estimada DATETIME NOT NULL;

-- Renombrar fecha_devolucion_real → fecha_hora_devolucion_real
ALTER TABLE prestamo CHANGE COLUMN fecha_devolucion_real fecha_hora_devolucion_real DATETIME;
