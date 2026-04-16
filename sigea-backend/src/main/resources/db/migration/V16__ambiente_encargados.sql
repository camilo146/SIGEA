CREATE TABLE IF NOT EXISTS ambiente_encargado (
    ambiente_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    PRIMARY KEY (ambiente_id, usuario_id),
    CONSTRAINT fk_ambiente_encargado_ambiente FOREIGN KEY (ambiente_id) REFERENCES ambiente(id) ON DELETE CASCADE,
    CONSTRAINT fk_ambiente_encargado_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id) ON DELETE CASCADE
);

CREATE INDEX idx_ambiente_encargado_usuario ON ambiente_encargado(usuario_id);
