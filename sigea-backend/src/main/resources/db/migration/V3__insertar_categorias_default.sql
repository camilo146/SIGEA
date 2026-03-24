-- ==============================================================================
-- V3: Insertar categorías por defecto para inventario
-- ==============================================================================
-- Herramientas, Herrajes, Cables, Computadores, Otros.
-- INSERT IGNORE evita error si alguna ya existe (uq_categoria_nombre).
-- ==============================================================================

INSERT IGNORE INTO categoria (nombre, descripcion, activo, fecha_creacion, fecha_actualizacion) VALUES
('Computadores', 'Equipos de cómputo, laptops, PCs, tablets', 1, NOW(), NOW()),
('Herramientas', 'Herramientas manuales y de taller', 1, NOW(), NOW()),
('Herrajes', 'Herrajes, tornillería, elementos de fijación', 1, NOW(), NOW()),
('Cables', 'Cables, conectores, adaptadores de red y eléctricos', 1, NOW(), NOW()),
('Equipos de medición', 'Multímetros, osciloscopios, generadores de señal', 1, NOW(), NOW()),
('Otros', 'Equipos que no encajan en las categorías anteriores', 1, NOW(), NOW());
