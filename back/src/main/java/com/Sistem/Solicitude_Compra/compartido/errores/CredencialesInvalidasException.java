package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends ErrorNegocioException {
    public CredencialesInvalidasException() {
        super(HttpStatus.UNAUTHORIZED,
                "CREDENCIALES_INVALIDAS", "Email o contraseña incorrectos");
    }
}
