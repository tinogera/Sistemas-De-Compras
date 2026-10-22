package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SolicitudCancelada(
        UUID eventoId,
        Instant ocurridoEn,
        Long solicitudId,
        List<Long> itemIds,
        Long usuarioId) {
}
