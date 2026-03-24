-- Vincula el préstamo con la reserva cuando el préstamo se creó desde "equipo recogido".
-- Permite al registrar devolución marcar la reserva como COMPLETADA.
ALTER TABLE prestamo ADD COLUMN reserva_id BIGINT NULL;
ALTER TABLE prestamo ADD CONSTRAINT fk_prestamo_reserva FOREIGN KEY (reserva_id) REFERENCES reserva(id);
