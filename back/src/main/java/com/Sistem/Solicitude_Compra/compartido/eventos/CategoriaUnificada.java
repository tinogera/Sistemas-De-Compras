package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record CategoriaUnificada(
        UUID eventoId,
        Instant ocurridoEn,
        Long origenId,
        Long destinoId,
        Long usuarioId) {
}
