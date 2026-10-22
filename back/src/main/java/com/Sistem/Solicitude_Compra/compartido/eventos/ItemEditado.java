package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemEditado(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long categoriaId,
        BigDecimal cantidad,
        String detalle,
        BigDecimal precioEstimado,
        Moneda monedaEstimada,
        Long usuarioId) {
}
