package com.Sistem.Solicitude_Compra.compartido.errores;

import org.springframework.http.HttpStatus;

public class ProveedorNoEncontradoException extends ErrorNegocioException {
    public ProveedorNoEncontradoException(Long id) {
        super(HttpStatus.NOT_FOUND, "PROVEEDOR_NO_ENCONTRADO", "No existe el proveedor " + id + ".");
    }
}