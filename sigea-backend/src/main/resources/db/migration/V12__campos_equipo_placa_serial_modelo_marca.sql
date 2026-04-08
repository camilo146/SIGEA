-- =====================================================================
-- V12: Nuevos campos en equipo (placa, serial, modelo, marca, escala)
-- =====================================================================
-- Agrega los campos requeridos por el cambio 2 del sistema SIGEA.
-- Se agregan como NULL para no romper datos existentes.
-- La placa tiene constraint UNIQUE (solo aplica a no-NULL).
-- =====================================================================

ALTER TABLE equipo
    ADD COLUMN placa         VARCHAR(20)  NULL AFTER nombre,
    ADD COLUMN serial        VARCHAR(50)  NULL AFTER placa,
    ADD COLUMN modelo        VARCHAR(100) NULL AFTER serial,
    ADD COLUMN marca_id      BIGINT       NULL AFTER modelo,
    ADD COLUMN estado_equipo_escala INT  NULL AFTER marca_id;

ALTER TABLE equipo
    ADD CONSTRAINT uq_equipo_placa UNIQUE (placa);

ALTER TABLE equipo
    ADD CONSTRAINT fk_equipo_marca
        FOREIGN KEY (marca_id) REFERENCES marca(id)
        ON UPDATE CASCADE
        ON DELETE SET NULL;

CREATE INDEX idx_equipo_marca_id ON equipo(marca_id);
