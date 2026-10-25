package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record ReglaModificada(
        UUID eventoId,
        Instant ocurridoEn,
        Long reglaId,
        String nombre,
        String accion,
        Long usuarioId) {
}
