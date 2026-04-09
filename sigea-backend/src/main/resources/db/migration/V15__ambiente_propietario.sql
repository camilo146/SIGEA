ALTER TABLE ambiente
    ADD COLUMN propietario_id BIGINT NULL AFTER instructor_responsable_id;

UPDATE ambiente
SET propietario_id = instructor_responsable_id
WHERE propietario_id IS NULL;

ALTER TABLE ambiente
    ADD CONSTRAINT fk_ambiente_propietario
    FOREIGN KEY (propietario_id) REFERENCES usuario(id);

CREATE INDEX idx_ambiente_propietario_id ON ambiente(propietario_id);