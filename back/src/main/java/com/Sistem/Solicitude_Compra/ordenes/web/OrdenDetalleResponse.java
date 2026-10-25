package com.Sistem.Solicitude_Compra.ordenes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;

import java.math.BigDecimal;
import java.time.Instant;

public record OrdenDetalleResponse(
        Long id,
        Long numero,
        Long itemId,
        Long solicitudId,
        Long proveedorId,
        Long cotizacionId,
        TipoItem tipo,
        String detalle,
        String proveedorRazonSocial,
        BigDecimal cantidad,
        Moneda moneda,
        BigDecimal precioUnitario,
        BigDecimal precioTotal,
        BigDecimal montoEnvio,
        EstadoOrden estado,
        Instant emitidaEn,
        Long enviadaPor,
        String enviadaPorNombre,
        Instant enviadaEn) {

    public static OrdenDetalleResponse de(OrdenPedido o, String enviadaPorNombre) {
        return new OrdenDetalleResponse(o.getId(), o.getNumero(), o.getItemId(), o.getSolicitudId(),
                o.getProveedorId(), o.getCotizacionId(), o.getTipo(), o.getDetalle(),
                o.getProveedorRazonSocial(), o.getCantidad(), o.getMoneda(), o.getPrecioUnitario(),
                o.getPrecioTotal(), o.getMontoEnvio(), o.getEstado(), o.getEmitidaEn(),
                o.getEnviadaPor(), enviadaPorNombre, o.getEnviadaEn());
    }
}
