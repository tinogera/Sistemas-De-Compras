package com.Sistem.Solicitude_Compra.solicitudes.dominio;

public enum EstadoItem {
    PENDIENTE, EN_COTIZACION, COMPRADO, RECHAZADO, CANCELADO;

    public boolean esFinal() {
        return this == COMPRADO || this == RECHAZADO || this == CANCELADO;
    }
}
