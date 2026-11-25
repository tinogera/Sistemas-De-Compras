package com.Sistem.Solicitude_Compra.reportes.repositorio;

/** Resultado de la consulta de tiempos: una fila por urgencia que tenga ítems resueltos en el rango. */
public interface TiempoPorUrgencia {

    String getUrgencia();

    long getCantidad();

    /** Promedio de (resuelto_en - creado_en) en horas, sin redondear. */
    Double getPromedioHoras();
}
