package com.Sistem.Solicitude_Compra.reportes.servicio;

import java.util.Map;

/** Resultado del reporte: total y desglose por urgencia (ALTA, MEDIA, BAJA). El promedio es null si no hay ítems. */
public record TiemposResolucion(long cantidadItems, Double promedioHoras, Map<String, PorUrgencia> porUrgencia) {

    public record PorUrgencia(long cantidad, Double promedioHoras) {
    }
}
