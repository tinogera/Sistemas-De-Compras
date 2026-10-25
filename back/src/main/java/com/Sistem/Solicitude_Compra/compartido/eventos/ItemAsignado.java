package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record ItemAsignado(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        Long encargadoAnteriorId,
        Long encargadoNuevoId) {
}
