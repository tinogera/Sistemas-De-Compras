package com.Sistem.Solicitude_Compra.reportes.dominio;

import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;

/** Por qué dato se agrupa el reporte de gasto. */
public enum Agrupacion {
    SECTOR, CATEGORIA, PROVEEDOR;

    /** Acepta "sector", "categoria" y "proveedor" en cualquier combinación de mayúsculas. */
    public static Agrupacion desde(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ParametroInvalidoException("Debe indicar por qué agrupar: sector, categoria o proveedor.");
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ParametroInvalidoException(
                    "No se puede agrupar por '" + texto + "'. Opciones: sector, categoria o proveedor.");
        }
    }
}
