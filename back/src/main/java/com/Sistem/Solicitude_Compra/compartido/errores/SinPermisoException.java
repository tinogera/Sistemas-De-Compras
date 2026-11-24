package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;


public class SinPermisoException extends ErrorNegocioException {
    public SinPermisoException(String message) {
        super(HttpStatus.FORBIDDEN,"SIN_PERMISO",message);
    }

    /** Un 403 con un código propio (por ejemplo PEDIDO_PROPIO). */
    public SinPermisoException(String codigo, String message) {
        super(HttpStatus.FORBIDDEN, codigo, message);
    }

}
