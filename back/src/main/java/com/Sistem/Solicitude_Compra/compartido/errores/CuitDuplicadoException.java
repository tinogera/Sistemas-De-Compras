package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class CuitDuplicadoException extends ErrorNegocioException {
    public CuitDuplicadoException(String cuit) {
        super(HttpStatus.CONFLICT, "CUIT_DUPLICADO", "Ya existe un proveedor con el CUIT " + cuit + ".");
    }
}
