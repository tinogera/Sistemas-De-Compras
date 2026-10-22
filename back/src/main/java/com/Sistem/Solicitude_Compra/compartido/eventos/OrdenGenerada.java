package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrdenGenerada(
        UUID eventoId,
        Instant ocurridoEn,
        Long ordenId,
        Long numero,
        Long itemId,
        Long solicitudId,
        Long sectorId,
        Long categoriaId,
        Long proveedorId,
        Moneda moneda,
        BigDecimal precioTotal) {
}
