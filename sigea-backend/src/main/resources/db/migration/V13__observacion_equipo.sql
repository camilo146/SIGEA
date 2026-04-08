-- =====================================================================
-- V13: Tabla de observaciones de devolución de equipos
-- =====================================================================
-- Registra el historial de observaciones al devolver un equipo.
-- Se crea al momento de devolver el equipo prestado.
-- =====================================================================

CREATE TABLE observacion_equipo (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestamo_id             BIGINT NOT NULL,
    equipo_id               BIGINT NOT NULL,
    usuario_duenio_id       BIGINT NOT NULL,
    usuario_prestatario_id  BIGINT NOT NULL,
    observaciones           TEXT NOT NULL,
    estado_devolucion       INT NOT NULL,
    fecha_registro          DATETIME NOT NULL,
    fecha_creacion          DATETIME NOT NULL,
    fecha_actualizacion     DATETIME,

    CONSTRAINT fk_obs_eq_prestamo    FOREIGN KEY (prestamo_id)            REFERENCES prestamo(id),
    CONSTRAINT fk_obs_eq_equipo      FOREIGN KEY (equipo_id)              REFERENCES equipo(id),
    CONSTRAINT fk_obs_eq_duenio      FOREIGN KEY (usuario_duenio_id)      REFERENCES usuario(id),
    CONSTRAINT fk_obs_eq_prestatario FOREIGN KEY (usuario_prestatario_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_obs_eq_equipo_id    ON observacion_equipo(equipo_id);
CREATE INDEX idx_obs_eq_prestamo_id  ON observacion_equipo(prestamo_id);
CREATE INDEX idx_obs_eq_fecha_reg    ON observacion_equipo(fecha_registro);
