package co.edu.sena.sigea.common.exception;

public class RecursoNoEncontradoException extends RuntimeException{
    

    //Creamos un constructor que recibe un mensaje de error 
    //y los pasa al constructor  de la clase padre RuntimeException
    //para que pueda ser manejado por el controlador de excepciones global.
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }


    //inicializamos el constructor con el nombre de la entidad y del id 
    //para generar un mensaje de error claro y especifico 
    public RecursoNoEncontradoException(String entidad, long id ) {
        super("Recurso no encontrado: " + entidad + " con ID " + id);
    }
}