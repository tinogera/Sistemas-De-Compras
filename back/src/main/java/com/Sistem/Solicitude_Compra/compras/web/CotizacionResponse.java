package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionResponse(
        Long id,
        Long proveedorId,
        String proveedorRazonSocial,
        Moneda moneda,
        BigDecimal precioUnitario,
        BigDecimal precioTotal,
        LocalDate fechaValidez,
        LocalDate entregaEstimada,
        String observaciones,
        EstadoCotizacion estado,
        boolean vigente) {

    public static CotizacionResponse de(Cotizacion c, LocalDate hoy) {
        return new CotizacionResponse(c.getId(), c.getProveedorId(), c.getProveedorRazonSocial(), c.getMoneda(),
                c.getPrecioUnitario(), c.getPrecioTotal(), c.getFechaValidez(), c.getEntregaEstimada(),
                c.getObservaciones(), c.getEstado(), c.estaVigente(hoy));
    }
}
