package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;

/**
 * Datos del ítem con los que el motor evalúa las reglas.
 * Se pasa completo aunque hoy solo se use el monto: así, una condición nueva
 * (categoría, sector, etc.) no obliga a cambiar la firma.
 */
public record ContextoEvaluacion(
        Monto monto,
        Long sectorId,
        Long categoriaId,
        Long solicitanteId,
        Urgencia urgencia) {
}