package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PasoAprobacionActivado(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long pasoId,
        String tipoAprobador,
        Rol rol,
        Long sectorId,
        String aprobadorNombre,
        BigDecimal monto,
        Moneda moneda) {
}
