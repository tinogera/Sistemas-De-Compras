package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaCondicion;

/**
 * Patrón Strategy: una implementación por cada TipoCondicion.
 * Para sumar un tipo nuevo: agregar el valor al enum y crear su EvaluadorXxx (@Component).
 * El motor no cambia.
 */
public interface EvaluadorCondicion {

    /** Tipo de condición que sabe evaluar esta implementación. */
    TipoCondicion tipo();

    /** true si la condición se cumple para el contexto dado. */
    boolean cumple(ReglaCondicion condicion, ContextoEvaluacion contexto);
}
