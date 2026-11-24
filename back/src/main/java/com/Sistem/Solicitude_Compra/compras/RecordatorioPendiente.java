package com.Sistem.Solicitude_Compra.compras;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.time.LocalDate;

/** Un ítem sin resolver al que le toca recordatorio. {@code encargadoId} es null si nadie lo tomó. */
public record RecordatorioPendiente(
        Long itemId,
        Long solicitudId,
        String detalle,
        Urgencia urgencia,
        LocalDate fechaNecesaria,
        long diasSinResolver,
        Long encargadoId) {
}
