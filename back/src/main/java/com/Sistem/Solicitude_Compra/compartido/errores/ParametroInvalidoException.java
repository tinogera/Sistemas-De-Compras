package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class ParametroInvalidoException extends ErrorNegocioException {
    public ParametroInvalidoException(String mensaje) {
        super(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO", mensaje);
    }
}
