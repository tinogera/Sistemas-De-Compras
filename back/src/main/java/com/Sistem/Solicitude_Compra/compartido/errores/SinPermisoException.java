package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;


public class SinPermisoException extends ErrorNegocioException {
    public SinPermisoException(String message) {
        super(HttpStatus.FORBIDDEN,"SIN_PERMISO",message);
    }
}
