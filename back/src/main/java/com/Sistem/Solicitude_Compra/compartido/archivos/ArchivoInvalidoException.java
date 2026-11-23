package com.Sistem.Solicitude_Compra.compartido.archivos;

import com.Sistem.Solicitude_Compra.compartido.errores.ErrorNegocioException;
import org.springframework.http.HttpStatus;

public class ArchivoInvalidoException extends ErrorNegocioException {

    private ArchivoInvalidoException(HttpStatus status, String codigo, String mensaje) {
        super(status, codigo, mensaje);
    }

    public static ArchivoInvalidoException tipoNoPermitido() {
        return new ArchivoInvalidoException(HttpStatus.UNPROCESSABLE_CONTENT, "TIPO_NO_PERMITIDO",
                "Solo se aceptan archivos PDF, JPG o PNG.");
    }

    public static ArchivoInvalidoException demasiadoGrande() {
        return new ArchivoInvalidoException(HttpStatus.CONTENT_TOO_LARGE, "ARCHIVO_DEMASIADO_GRANDE",
                "El archivo supera los 10 MB.");
    }

    public static ArchivoInvalidoException vacio() {
        return new ArchivoInvalidoException(HttpStatus.UNPROCESSABLE_CONTENT, "ARCHIVO_VACIO",
                "El archivo está vacío.");
    }
}
