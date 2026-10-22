package com.Sistem.Solicitude_Compra.compartido.eventos;

import java.time.Instant;
import java.util.UUID;

public record SolicitudCerrada(
        UUID eventoId,
        Instant ocurridoEn,
        Long solicitudId) {
}
