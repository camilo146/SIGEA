package co.edu.sena.sigea.common.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import co.edu.sena.sigea.common.dto.ErrorRespuesta;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    // 1. Recurso no encontrado (404) 

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuesta> manejarRecursoNoEncontrado(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        ErrorRespuesta error = ErrorRespuesta.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso No Encontrado")
                .message(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 2. Recurso duplicado (409)

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorRespuesta> manejarRecursoDuplicado(
            RecursoDuplicadoException ex, HttpServletRequest request) {

        ErrorRespuesta error = ErrorRespuesta.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Recurso Duplicado")
                .message(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    //3. Operación no permitida (403) 

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<ErrorRespuesta> manejarOperacionNoPermitida(
            OperacionNoPermitidaException ex, HttpServletRequest request) {

        ErrorRespuesta error = ErrorRespuesta.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Operación No Permitida")
                .message(ex.getMessage())
                .ruta(request.getRequestURI())
                .build();

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    //  Errores de validación (400) 

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> detalles = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        ErrorRespuesta error = ErrorRespuesta.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Error de Validación")
                .message("Los datos enviados no son válidos")
                .ruta(request.getRequestURI())
                .detalles(detalles)
                .build();

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}