package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompraActualizada(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        BigDecimal precioTotalAnterior,
        BigDecimal precioTotal,
        BigDecimal montoEnvioAnterior,
        BigDecimal montoEnvio,
        Moneda moneda,
        Long usuarioId) {
}
