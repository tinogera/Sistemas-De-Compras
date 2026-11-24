package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.math.BigDecimal;
import java.util.List;

public record ReglaResponse(
        Long id,
        String nombre,
        int prioridad,
        boolean activa,
        boolean porDefecto,
        Integer version,
        List<CondicionResponse> condiciones,
        List<PasoResponse> pasos) {

    public record CondicionResponse(TipoCondicion tipo, OperadorCondicion operador, BigDecimal valor, Moneda moneda) {
    }

    /** {@code sinAprobadores}: nadie activo puede decidir este paso (caso borde 24); se calcula al leer. */
    public record PasoResponse(int orden, TipoAprobador tipoAprobador, Rol rol, Long sectorId,
                               String aprobadorNombre, boolean sinAprobadores) {
    }
}
