package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CotizacionCargada(
        UUID eventoId,
        Instant ocurridoEn,
        Long cotizacionId,
        Long itemId,
        Long proveedorId,
        Moneda moneda,
        BigDecimal precioTotal,
        Long usuarioId) {
}
