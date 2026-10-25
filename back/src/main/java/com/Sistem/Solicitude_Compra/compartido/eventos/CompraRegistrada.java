package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompraRegistrada(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long sectorId,
        Long categoriaId,
        TipoItem tipo,
        String detalle,
        BigDecimal cantidad,
        Long proveedorId,
        String proveedorRazonSocial,
        Long cotizacionId,
        Moneda moneda,
        BigDecimal precioTotal,
        Long encargadoId) {
}
