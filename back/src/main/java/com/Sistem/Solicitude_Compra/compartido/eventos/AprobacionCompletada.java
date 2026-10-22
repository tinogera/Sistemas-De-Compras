package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record AprobacionCompletada(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        boolean automatica) {
}
