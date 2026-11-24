package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;

import java.math.BigDecimal;

/** Una fila del reporte de gasto, tal como sale de la consulta agrupada. */
public interface FilaGasto {

    /** Id del sector, la categoría o el proveedor, según cómo se agrupó. */
    Long getClave();

    Moneda getMoneda();

    /** Suma de los importes de las órdenes. */
    BigDecimal getTotal();

    /** Suma de los montos de envío. */
    BigDecimal getEnvio();

    Long getCantidad();
}
