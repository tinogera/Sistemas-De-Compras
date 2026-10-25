package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;

/** Paso que debe cumplirse según la regla elegida. Con él se generan los PasoItem. */
public record PasoRequerido(
        int orden,
        TipoAprobador tipoAprobador,
        Rol rol,
        Long sectorId,
        String nombre) {
}