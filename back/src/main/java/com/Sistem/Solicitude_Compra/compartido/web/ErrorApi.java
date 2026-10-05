package com.Sistem.Solicitude_Compra.compartido.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorApi(String codigo, String mensaje, Map<String, String> campos) {

    public ErrorApi(String codigo, String mensaje) {
        this(codigo, mensaje, null);
    }

}
