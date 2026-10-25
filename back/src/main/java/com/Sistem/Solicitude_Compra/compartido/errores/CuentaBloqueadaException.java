package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class CuentaBloqueadaException extends ErrorNegocioException {
    public CuentaBloqueadaException(long minutos) {
        super(HttpStatus.TOO_MANY_REQUESTS,
                "CUENTA_BLOQUEADA", "Demasiados intentos fallidos. Probá de nuevo en " + minutos + " minutos");
    }
}
