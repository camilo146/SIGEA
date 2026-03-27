-- =====================================================================
-- V10: Rol ALIMENTADOR_EQUIPOS, sub-ubicaciones jerárquicas e índices
-- =====================================================================

-- 1. Referencia jerárquica en ambiente (sub-ubicaciones)
ALTER TABLE ambiente
    ADD COLUMN padre_id BIGINT DEFAULT NULL
    AFTER instructor_responsable_id;

ALTER TABLE ambiente
    ADD CONSTRAINT fk_ambiente_padre
    FOREIGN KEY (padre_id) REFERENCES ambiente(id)
    ON DELETE SET NULL;

-- 2. Sub-ubicación en equipo (opcional, debe ser hijo del ambiente principal)
ALTER TABLE equipo
    ADD COLUMN sub_ubicacion_id BIGINT NULL
    AFTER ambiente_id;

ALTER TABLE equipo
    ADD CONSTRAINT fk_equipo_sub_ubicacion
    FOREIGN KEY (sub_ubicacion_id) REFERENCES ambiente(id)
    ON DELETE SET NULL;

-- 3. Índices de rendimiento para soporte de 30+ usuarios concurrentes
CREATE INDEX idx_equipo_nombre           ON equipo(nombre);
CREATE INDEX idx_equipo_estado           ON equipo(estado);
CREATE INDEX idx_equipo_propietario_id   ON equipo(propietario_id);
CREATE INDEX idx_equipo_ambiente_id      ON equipo(ambiente_id);
CREATE INDEX idx_equipo_sub_ubicacion_id ON equipo(sub_ubicacion_id);
CREATE INDEX idx_ambiente_nombre         ON ambiente(nombre);
CREATE INDEX idx_ambiente_padre_id       ON ambiente(padre_id);
CREATE INDEX idx_ambiente_activo         ON ambiente(activo);
