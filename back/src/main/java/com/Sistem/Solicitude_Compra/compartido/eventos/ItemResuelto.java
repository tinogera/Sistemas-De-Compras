package com.Sistem.Solicitude_Compra.compartido.eventos;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.time.Instant;
import java.util.UUID;

public record ItemResuelto(
        UUID eventoId,
        Instant ocurridoEn,
        Long itemId,
        Long solicitudId,
        String resultado,
        String proveedorRazonSocial,
        String motivoRechazo,
        String rechazadoPor,
        Urgencia urgencia,
        Instant solicitadoEn,
        Instant resueltoEn,
        Long encargadoId) {
}
