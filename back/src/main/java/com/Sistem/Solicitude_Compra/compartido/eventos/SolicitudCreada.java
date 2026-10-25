package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SolicitudCreada(
        UUID eventoId,
        Instant ocurridoEn,
        Long solicitudId,
        Long solicitanteId,
        Long sectorId,
        Urgencia urgencia,
        LocalDate fechaNecesaria,
        List<ItemCreado> items) {

    public record ItemCreado(
            Long itemId,
            TipoItem tipo,
            Long categoriaId,
            BigDecimal cantidad,
            String detalle,
            BigDecimal precioEstimado,
            Moneda monedaEstimada) {
    }
}
