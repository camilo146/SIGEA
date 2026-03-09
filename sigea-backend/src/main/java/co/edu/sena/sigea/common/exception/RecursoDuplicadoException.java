package co.edu.sena.sigea.common.exception;

public class RecursoDuplicadoException extends RuntimeException {

    // Excepción personalizada para indicar que un recurso ya existe (RF-GEN-01).
    // Se lanza cuando se intenta crear un recurso que ya existe en el sistema, 
    // como un usuario con el mismo correo o un equipo con el mismo número de serie.
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}