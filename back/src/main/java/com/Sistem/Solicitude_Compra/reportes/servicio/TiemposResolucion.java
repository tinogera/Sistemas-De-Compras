package com.Sistem.Solicitude_Compra.reportes.servicio;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;

import java.util.Map;

/**
 * Tiempo de resolución de los ítems resueltos en un rango. promedioHoras es null (no 0) cuando no hay ítems:
 * 0 parecería "resuelto al instante".
 */
public record TiemposResolucion(long cantidadItems, Double promedioHoras, Map<Urgencia, TiempoUrgencia> porUrgencia) {

    public record TiempoUrgencia(long cantidad, Double promedioHoras) {
    }
}
