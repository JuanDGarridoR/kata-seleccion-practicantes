package com.juangarrido.practicantes.config;

import com.juangarrido.practicantes.archivo.ArchivoInvalidoException;
import com.juangarrido.practicantes.archivo.ArchivoNoEncontradoException;
import com.juangarrido.practicantes.practicante.CambioEstadoInvalidoException;
import com.juangarrido.practicantes.practicante.CorreoDuplicadoException;
import com.juangarrido.practicantes.practicante.PracticanteNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** Todas las respuestas de error de la API salen con el mismo formato: {@link RespuestaError}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record RespuestaError(int status, String error, String mensaje,
                                 Map<String, String> campos, LocalDateTime fecha) {
    }

    /** Cubre @Valid en @ModelAttribute (registro) y en @RequestBody (cambio de estado). */
    @ExceptionHandler(BindException.class)
    ResponseEntity<RespuestaError> validacion(BindException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getFieldErrors()) {
            String mensaje = error.isBindingFailure() ? "Valor con formato inválido" : error.getDefaultMessage();
            campos.putIfAbsent(error.getField(), mensaje);
        }
        return responder(HttpStatus.BAD_REQUEST, "Hay campos con errores", campos);
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    ResponseEntity<RespuestaError> archivoInvalido(ArchivoInvalidoException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of("hojaVida", ex.getMessage()));
    }

    @ExceptionHandler(CambioEstadoInvalidoException.class)
    ResponseEntity<RespuestaError> estadoInvalido(CambioEstadoInvalidoException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of("estado", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<RespuestaError> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<RespuestaError> parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + ex.getName() + "'", null);
    }

    @ExceptionHandler({PracticanteNoEncontradoException.class, ArchivoNoEncontradoException.class})
    ResponseEntity<RespuestaError> noEncontrado(RuntimeException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    ResponseEntity<RespuestaError> correoDuplicado(CorreoDuplicadoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), Map.of("correo", "Este correo ya está registrado"));
    }

    /** Respaldo por si dos registros con el mismo correo llegan a la vez y los frena la restricción UNIQUE. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<RespuestaError> integridad(DataIntegrityViolationException ex) {
        return responder(HttpStatus.CONFLICT, "El registro entra en conflicto con uno existente", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<RespuestaError> archivoMuyGrande(MaxUploadSizeExceededException ex) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "La hoja de vida no puede superar 5 MB",
                Map.of("hojaVida", "El archivo supera 5 MB"));
    }

    /**
     * Las excepciones propias de Spring MVC (405, 415, 404 de recursos...) ya traen su código HTTP;
     * se respeta. Cualquier otra cosa es un error real del servidor y se registra en el log.
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<RespuestaError> inesperado(Exception ex) {
        if (ex instanceof ErrorResponse errorSpring) {
            HttpStatusCode codigo = errorSpring.getStatusCode();
            return responder(HttpStatus.valueOf(codigo.value()), errorSpring.getBody().getDetail(), null);
        }
        log.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado", null);
    }

    private static ResponseEntity<RespuestaError> responder(HttpStatus status, String mensaje, Map<String, String> campos) {
        RespuestaError cuerpo = new RespuestaError(status.value(), status.getReasonPhrase(), mensaje, campos, LocalDateTime.now());
        return ResponseEntity.status(status).body(cuerpo);
    }
}
