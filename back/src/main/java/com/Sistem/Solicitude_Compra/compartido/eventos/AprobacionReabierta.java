package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record AprobacionReabierta(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        String motivo) {
}
