package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;

/** Texto con el que se muestra a un aprobador de tipo rol ("Supervisor", "Administrador"). */
public final class NombreAprobador {

    private NombreAprobador() {
    }

    public static String de(Rol rol) {
        return switch (rol) {
            case SOLICITANTE -> "Solicitante";
            case ENCARGADO -> "Encargado";
            case ADMIN -> "Administrador";
            case SUPERVISOR -> "Supervisor";
        };
    }
}
