package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;

import java.math.BigDecimal;

/** Gasto de un sector, categoría o proveedor en una moneda. Total y envío van separados. */
public record GastoAgrupado(Long clave, String nombre, Moneda moneda, BigDecimal total, BigDecimal envio,
                            long cantidadOrdenes) {
}
