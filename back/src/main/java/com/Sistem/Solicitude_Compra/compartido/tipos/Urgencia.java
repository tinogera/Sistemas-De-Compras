package com.Sistem.Solicitude_Compra.compartido.tipos;

public enum Urgencia {
    BAJA, MEDIA, ALTA;

    public int prioridad() {
        return switch (this) {
            case BAJA -> 3;
            case MEDIA -> 2;
            case ALTA -> 1;
        };
    }
}
