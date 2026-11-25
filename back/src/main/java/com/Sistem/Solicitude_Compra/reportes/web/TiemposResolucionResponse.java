package com.Sistem.Solicitude_Compra.reportes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion;

import java.util.Map;
import java.util.stream.Collectors;

/** { cantidadItems, promedioHoras, porUrgencia: { ALTA: { cantidad, promedioHoras }, MEDIA: ..., BAJA: ... } } */
public record TiemposResolucionResponse(long cantidadItems, Double promedioHoras,
                                        Map<Urgencia, PorUrgencia> porUrgencia) {

    public record PorUrgencia(long cantidad, Double promedioHoras) {
    }

    public static TiemposResolucionResponse de(TiemposResolucion t) {
        Map<Urgencia, PorUrgencia> porUrgencia = t.porUrgencia().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> new PorUrgencia(e.getValue().cantidad(), e.getValue().promedioHoras()),
                        (a, b) -> a, java.util.LinkedHashMap::new));
        return new TiemposResolucionResponse(t.cantidadItems(), t.promedioHoras(), porUrgencia);
    }
}
