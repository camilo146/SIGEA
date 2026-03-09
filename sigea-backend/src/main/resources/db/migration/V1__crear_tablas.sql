-- SIGEA . Sistema integral de de gestion de equipos y activos
-- Migracion V-1: creacion de todas las tablas 
-- Motor: MariaDB 11+
-- charset: utf8mb4 (soporta emojis y caracteres especiales)

CREATE TABLE usuario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(150) NOT NULL,
    tipo_documento VARCHAR(5) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    correo_electronico VARCHAR(100),
    telefono VARCHAR(20),
    programa_formacion VARCHAR(200),
    ficha VARCHAR(20),
    contrasena_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL,
    es_super_admin BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    intentos_fallidos INT NOT NULL DEFAULT 0,
    cuenta_bloqueada_hasta DATETIME,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_usuario_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT uq_usuario_correo UNIQUE (correo_electronico)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE categoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,
    
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ambiente (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ubicacion VARCHAR(200) NOT NULL,
    descripcion TEXT,
    instructor_responsable_id BIGINT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_ambiente_nombre UNIQUE (nombre),
    CONSTRAINT fk_ambiente_instructor FOREIGN KEY (instructor_responsable_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    codigo_unico VARCHAR(50) NOT NULL,
    categoria_id BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL,
    cantidad_total INT NOT NULL DEFAULT 1,
    cantidad_disponible INT NOT NULL DEFAULT 1,
    ambiente_id BIGINT,
    umbral_minimo INT NOT NULL DEFAULT 1,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_equipo_codigo UNIQUE (codigo_unico),
    CONSTRAINT fk_equipo_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT fk_equipo_ambiente FOREIGN KEY (ambiente_id) REFERENCES ambiente(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE foto_equipo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_archivo VARCHAR(500) NOT NULL,
    tamano_bytes BIGINT,
    fecha_subida DATETIME NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_foto_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE prestamo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_solicitante_id BIGINT NOT NULL,
    administrador_aprueba_id BIGINT,
    administrador_recibe_id BIGINT,
    fecha_solicitud DATETIME NOT NULL,
    fecha_aprobacion DATETIME,
    fecha_entrega DATETIME,
    fecha_devolucion_esperada DATETIME,
    fecha_devolucion_real DATETIME,
    estado VARCHAR(20) NOT NULL,
    observaciones_generales TEXT,
    extensiones_realizadas INT NOT NULL DEFAULT 0,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_prestamo_solicitante FOREIGN KEY (usuario_solicitante_id) REFERENCES usuario(id),
    CONSTRAINT fk_prestamo_aprueba FOREIGN KEY (administrador_aprueba_id) REFERENCES usuario(id),
    CONSTRAINT fk_prestamo_recibe FOREIGN KEY (administrador_recibe_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE detalle_prestamo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestamo_id BIGINT NOT NULL,
    equipo_id BIGINT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    estado_equipo_entrega VARCHAR(20),
    estado_equipo_devolucion VARCHAR(20),
    observaciones_entrega TEXT,
    observaciones_devolucion TEXT,
    devuelto BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_detalle_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamo(id),
    CONSTRAINT fk_detalle_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE extension_prestamo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prestamo_id BIGINT NOT NULL,
    fecha_solicitud DATETIME NOT NULL,
    nueva_fecha_devolucion DATETIME NOT NULL,
    administrador_aprueba_id BIGINT,
    estado VARCHAR(20) NOT NULL,
    motivo TEXT NOT NULL,
    fecha_respuesta DATETIME,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_extension_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamo(id),
    CONSTRAINT fk_extension_admin FOREIGN KEY (administrador_aprueba_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reporte_dano (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    detalle_prestamo_id BIGINT NOT NULL,
    descripcion TEXT NOT NULL,
    foto_ruta VARCHAR(500),
    fecha_reporte DATETIME NOT NULL,
    reportado_por_id BIGINT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_reporte_detalle UNIQUE (detalle_prestamo_id),
    CONSTRAINT fk_reporte_detalle FOREIGN KEY (detalle_prestamo_id) REFERENCES detalle_prestamo(id),
    CONSTRAINT fk_reporte_usuario FOREIGN KEY (reportado_por_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reserva (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    equipo_id BIGINT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    fecha_hora_inicio DATETIME NOT NULL,
    fecha_hora_fin DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_reserva_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    CONSTRAINT fk_reserva_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE transferencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    ambiente_origen_id BIGINT NOT NULL,
    ambiente_destino_id BIGINT NOT NULL,
    cantidad INT NOT NULL DEFAULT 1,
    administrador_autoriza_id BIGINT NOT NULL,
    motivo TEXT,
    fecha_transferencia DATETIME NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_transferencia_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id),
    CONSTRAINT fk_transferencia_origen FOREIGN KEY (ambiente_origen_id) REFERENCES ambiente(id),
    CONSTRAINT fk_transferencia_destino FOREIGN KEY (ambiente_destino_id) REFERENCES ambiente(id),
    CONSTRAINT fk_transferencia_admin FOREIGN KEY (administrador_autoriza_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE mantenimiento (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    responsable VARCHAR(200) NOT NULL,
    observaciones TEXT,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_mantenimiento_equipo FOREIGN KEY (equipo_id) REFERENCES equipo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notificacion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_destino_id BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    titulo VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    medio_envio VARCHAR(20) NOT NULL,
    estado_envio VARCHAR(20) NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_envio DATETIME,
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT fk_notificacion_usuario FOREIGN KEY (usuario_destino_id) REFERENCES usuario(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE log_auditoria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT,
    accion VARCHAR(100) NOT NULL,
    entidad_afectada VARCHAR(100) NOT NULL,
    entidad_id BIGINT,
    detalles TEXT,
    direccion_ip VARCHAR(45),
    fecha_hora DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE configuracion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    clave VARCHAR(100) NOT NULL,
    valor VARCHAR(500) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    descripcion VARCHAR(500),
    fecha_creacion DATETIME NOT NULL,
    fecha_actualizacion DATETIME,

    CONSTRAINT uq_configuracion_clave UNIQUE (clave)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- DATOS INICIALES (Seed Data)
-- Configuraciones por defecto del sistema
INSERT INTO configuracion (clave, valor, tipo, descripcion, fecha_creacion) VALUES
('prestamo.dias.maximo', '5', 'INTEGER', 'Cantidad máxima de días para un préstamo', NOW()),
('prestamo.extensiones.maximo', '2', 'INTEGER', 'Cantidad máxima de extensiones permitidas por préstamo', NOW()),
('equipo.umbral.minimo.defecto', '1', 'INTEGER', 'Umbral mínimo de stock por defecto para alertas', NOW()),
('correo.notificacion.activo', 'true', 'BOOLEAN', 'Habilitar o deshabilitar envío de correos', NOW());