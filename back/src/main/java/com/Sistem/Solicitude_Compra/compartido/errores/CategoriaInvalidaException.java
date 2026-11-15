package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class CategoriaInvalidaException extends ErrorNegocioException {
    public CategoriaInvalidaException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "CATEGORIA_INVALIDA", mensaje);
    }
}