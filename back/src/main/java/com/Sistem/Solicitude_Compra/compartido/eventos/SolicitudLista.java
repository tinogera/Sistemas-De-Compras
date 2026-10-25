package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SolicitudLista(
        UUID eventoId,
        Instant ocurridoEn,
        Long solicitudId,
        Long solicitanteId,
        List<ResultadoItem> resultados) {

    public record ResultadoItem(
            Long itemId,
            Long categoriaId,
            String detalle,
            String estado,
            String proveedorRazonSocial,
            String motivoRechazo,
            String rechazadoPor) {
    }
}
