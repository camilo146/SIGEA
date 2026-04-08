-- =====================================================================
-- V14: Tabla de préstamos de ambientes de formación
-- =====================================================================
-- Sistema de préstamo de ambientes separado del préstamo de equipos.
-- Estados: SOLICITADO, APROBADO, RECHAZADO, ACTIVO, DEVUELTO, CANCELADO
-- Actividades: CLASE, TALLER, EVALUACION, REUNION, OTRO
-- =====================================================================

CREATE TABLE prestamo_ambiente (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    ambiente_id                 BIGINT NOT NULL,
    solicitante_id              BIGINT NOT NULL,
    propietario_ambiente_id     BIGINT NOT NULL,
    fecha_inicio                DATE NOT NULL,
    fecha_fin                   DATE NOT NULL,
    hora_inicio                 TIME NOT NULL,
    hora_fin                    TIME NOT NULL,
    proposito                   TEXT NOT NULL,
    numero_participantes        INT NOT NULL,
    tipo_actividad              VARCHAR(20) NOT NULL,
    observaciones_solicitud     TEXT,
    estado                      VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO',
    observaciones_devolucion    TEXT,
    estado_devolucion_ambiente  INT,
    fecha_solicitud             DATETIME NOT NULL,
    fecha_aprobacion            DATETIME,
    fecha_devolucion            DATETIME,
    fecha_creacion              DATETIME NOT NULL,
    fecha_actualizacion         DATETIME,

    CONSTRAINT fk_pa_ambiente   FOREIGN KEY (ambiente_id)              REFERENCES ambiente(id),
    CONSTRAINT fk_pa_solicitante FOREIGN KEY (solicitante_id)          REFERENCES usuario(id),
    CONSTRAINT fk_pa_propietario FOREIGN KEY (propietario_ambiente_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pa_ambiente_id    ON prestamo_ambiente(ambiente_id);
CREATE INDEX idx_pa_estado         ON prestamo_ambiente(estado);
CREATE INDEX idx_pa_solicitante_id ON prestamo_ambiente(solicitante_id);
CREATE INDEX idx_pa_fecha_inicio   ON prestamo_ambiente(fecha_inicio);
