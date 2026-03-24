ALTER TABLE equipo
    ADD COLUMN tipo_uso VARCHAR(20) NOT NULL DEFAULT 'NO_CONSUMIBLE' AFTER cantidad_disponible;

UPDATE equipo
SET tipo_uso = 'NO_CONSUMIBLE'
WHERE tipo_uso IS NULL OR tipo_uso = '';