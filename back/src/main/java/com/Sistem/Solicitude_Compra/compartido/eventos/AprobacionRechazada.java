package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record AprobacionRechazada(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long pasoId,
        Long usuarioId,
        String aprobadorNombre,
        String motivo) {
}
