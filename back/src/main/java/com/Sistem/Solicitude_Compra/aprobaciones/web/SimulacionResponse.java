package com.Sistem.Solicitude_Compra.aprobaciones.web;

import java.util.List;

public record SimulacionResponse(ReglaElegida regla, boolean automatica, List<PasoSimulado> pasos) {

    public record ReglaElegida(Long id, String nombre) {
    }

    public record PasoSimulado(int orden, String aprobadorNombre) {
    }
}
