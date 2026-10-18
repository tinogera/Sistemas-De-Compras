package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Regla;

import java.util.List;

/**
 * Resultado de evaluar las reglas: la regla que se aplicó y sus pasos.
 * Una lista de pasos vacía significa aprobación automática.
 */
public record ResultadoEvaluacion(Regla regla, List<PasoRequerido> pasos) {
}