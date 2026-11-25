package com.Sistem.Solicitude_Compra.reportes.repositorio;

/** Fila del reporte de tiempos de resolución: cuántos ítems y cuánto tardaron en promedio, por urgencia. */
public interface TiempoPorUrgencia {

    String getUrgencia();

    long getCantidad();

    Double getPromedioHoras();
}
