package com.Sistem.Solicitude_Compra.compartido.web;

import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ErrorNegocioException.class)
    ResponseEntity<ErrorApi> negocio(ErrorNegocioException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorApi(ex.getCodigo(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Valor inválido",
                        (primero, segundo) -> primero));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorApi("VALIDACION", "Hay datos inválidos.", campos));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ErrorApi> conflictoVersion(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorApi("CONFLICTO_VERSION", "Otro usuario modificó este dato. Recargá la página."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> duplicado(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorApi("DUPLICADO", "Ya existe un registro con esos datos."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorApi("CUERPO_INVALIDO", "El cuerpo de la petición está mal formado o tiene valores inválidos."));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HandlerMethodValidationException.class
    })
    ResponseEntity<ErrorApi> parametroInvalido(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorApi("PARAMETRO_INVALIDO", "Falta un parámetro o tiene un valor inválido."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorApi> rutaNoEncontrada(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorApi("NO_ENCONTRADO", "La ruta no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorApi> metodoNoPermitido(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorApi("METODO_NO_PERMITIDO", "Método HTTP no permitido para esta ruta."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> generico(Exception ex) {
        log.error("Error no manejado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorApi("ERROR_INTERNO", "Ocurrió un error inesperado."));
    }
}
