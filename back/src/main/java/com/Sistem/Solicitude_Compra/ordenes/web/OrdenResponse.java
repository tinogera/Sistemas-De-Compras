package com.Sistem.Solicitude_Compra.ordenes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;

import java.math.BigDecimal;
import java.time.Instant;

public record OrdenResponse(
        Long id,
        Long numero,
        Long itemId,
        Long solicitudId,
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
        Instant enviadaEn) {

    public static OrdenResponse de(OrdenPedido o) {
        return new OrdenResponse(o.getId(), o.getNumero(), o.getItemId(), o.getSolicitudId(), o.getTipo(),
                o.getDetalle(), o.getProveedorRazonSocial(), o.getCantidad(), o.getMoneda(),
                o.getPrecioUnitario(), o.getPrecioTotal(), o.getMontoEnvio(), o.getEstado(),
                o.getEmitidaEn(), o.getEnviadaEn());
    }
}
