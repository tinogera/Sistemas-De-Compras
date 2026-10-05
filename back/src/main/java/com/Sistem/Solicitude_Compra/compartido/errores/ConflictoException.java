package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class ConflictoException extends ErrorNegocioException {
    public ConflictoException(String codigo, String mensaje) {
        super(HttpStatus.CONFLICT, codigo, mensaje);
    }
}
