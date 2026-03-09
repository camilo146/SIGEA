package co.edu.sena.sigea.common.exception;

public class OperacionNoPermitidaException extends RuntimeException {
    // Excepción personalizada para indicar que una operación no está permitida (RF-GEN-02).
    // Se lanza cuando se intenta realizar una acción que no está autorizada
    // o que viola las reglas del negocio, como cancelar una reserva que ya fue completada o realizar un préstamo sin disponibilidad.
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
