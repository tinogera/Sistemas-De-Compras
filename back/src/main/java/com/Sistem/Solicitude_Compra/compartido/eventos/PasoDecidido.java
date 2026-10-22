package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record PasoDecidido(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long pasoId,
        String decision,
        Long usuarioId,
        String motivo) {
}
