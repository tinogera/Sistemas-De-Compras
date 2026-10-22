package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PrecioTotalActualizado(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        BigDecimal precioTotal,
        Moneda moneda,
        Long cotizacionId,
        String proveedorRazonSocial,
        Long usuarioId) {
}
