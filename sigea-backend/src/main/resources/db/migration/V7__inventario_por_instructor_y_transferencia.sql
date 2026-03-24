-- V7: Inventario por instructor y transferencias entre inventarios.
-- Nota: el proyecto indica que los datos actuales son de prueba, por eso
-- se rehace la tabla transferencia para reflejar el nuevo dominio.

ALTER TABLE ambiente
    ADD COLUMN direccion VARCHAR(250) NULL AFTER descripcion;

ALTER TABLE equipo
    ADD COLUMN propietario_id BIGINT NULL AFTER ambiente_id,
    ADD COLUMN inventario_actual_instructor_id BIGINT NULL AFTER propietario_id;

-- Inicialización de datos existentes (solo datos de prueba).
UPDATE equipo e
LEFT JOIN ambiente a ON a.id = e.ambiente_id
SET e.propietario_id = a.instructor_responsable_id,
    e.inventario_actual_instructor_id = a.instructor_responsable_id
WHERE e.propietario_id IS NULL OR e.inventario_actual_instructor_id IS NULL;

UPDATE equipo e
SET e.propietario_id = COALESCE(e.propietario_id, (SELECT u.id FROM usuario u ORDER BY u.id LIMIT 1)),
    e.inventario_actual_instructor_id = COALESCE(e.inventario_actual_instructor_id, (SELECT u.id FROM usuario u ORDER BY u.id LIMIT 1))
WHERE e.propietario_id IS NULL OR e.inventario_actual_instructor_id IS NULL;

ALTER TABLE equipo
    MODIFY COLUMN propietario_id BIGINT NOT NULL,
    MODIFY COLUMN inventario_actual_instructor_id BIGINT NOT NULL;

ALTER TABLE equipo
    ADD CONSTRAINT fk_equipo_propietario
        FOREIGN KEY (propietario_id) REFERENCES usuario(id),
    ADD CONSTRAINT fk_equipo_inventario_actual_instructor
        FOREIGN KEY (inventario_actual_instructor_id) REFERENCES usuario(id);

DROP TABLE transferencia;

CREATE TABLE transferencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    inventario_origen_instructor_id BIGINT NOT NULL,
    inventario_destino_instructor_id BIGINT NOT NULL,
    propietario_equipo_id BIGINT NOT NULL,
    ubicacion_destino_id BIGINT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    administrador_autoriza_id BIGINT NOT NULL,
    motivo TEXT,
    fecha_transferencia DATETIME NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_transferencia_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id),
    CONSTRAINT fk_transferencia_inv_origen FOREIGN KEY (inventario_origen_instructor_id) REFERENCES usuario(id),
    CONSTRAINT fk_transferencia_inv_destino FOREIGN KEY (inventario_destino_instructor_id) REFERENCES usuario(id),
    CONSTRAINT fk_transferencia_propietario FOREIGN KEY (propietario_equipo_id) REFERENCES usuario(id),
    CONSTRAINT fk_transferencia_ubicacion_destino FOREIGN KEY (ubicacion_destino_id) REFERENCES ambiente(id),
    CONSTRAINT fk_transferencia_admin FOREIGN KEY (administrador_autoriza_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
