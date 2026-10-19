package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class SesionInvalidaException extends ErrorNegocioException {
    public SesionInvalidaException() {
        super(HttpStatus.UNAUTHORIZED, "SESION_INVALIDA", "Tenés que iniciar sesión");
    }
}
