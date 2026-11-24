package com.Sistem.Solicitude_Compra.reportes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.reportes.servicio.GastoAgrupado;

import java.math.BigDecimal;

/** total es lo comprado y envio el costo de envío: van separados, el front muestra también su suma. */
public record GastoResponse(Long clave, String nombre, Moneda moneda, BigDecimal total, BigDecimal envio,
                            long cantidadOrdenes) {

    public static GastoResponse de(GastoAgrupado g) {
        return new GastoResponse(g.clave(), g.nombre(), g.moneda(), g.total(), g.envio(), g.cantidadOrdenes());
    }
}
