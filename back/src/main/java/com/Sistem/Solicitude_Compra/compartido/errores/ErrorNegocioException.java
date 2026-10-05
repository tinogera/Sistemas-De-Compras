package com.Sistem.Solicitude_Compra.compartido.errores;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ErrorNegocioException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;

    protected ErrorNegocioException(HttpStatus status, String codigo, String mensaje) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
}