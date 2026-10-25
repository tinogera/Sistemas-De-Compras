package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record OrdenEnviada(
        UUID eventoId,
        Instant ocurridoEn,
        Long ordenId,
        Long itemId,
        Long solicitudId,
        Long usuarioId) {
}
