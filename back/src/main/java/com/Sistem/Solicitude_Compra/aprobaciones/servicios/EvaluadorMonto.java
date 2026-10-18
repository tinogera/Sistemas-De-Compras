package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.ReglaCondicion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EvaluadorMonto implements EvaluadorCondicion {

    @Override
    public TipoCondicion tipo() {
        return TipoCondicion.MONTO;
    }

    /**
     * Se cumple solo si la moneda del monto es la de la condición (sin conversión, decisión 8)
     * y el importe cumple el operador contra el valor de la condición.
     * Compara siempre con compareTo: con equals, 500.0 y 500.00 serían distintos.
     */
    @Override
    public boolean cumple(ReglaCondicion condicion, ContextoEvaluacion contexto) {
        Monto monto = contexto.monto();
        BigDecimal valor = condicion.getValorNumerico();

        if (monto == null || monto.importe() == null || monto.moneda() == null
                || valor == null || condicion.getOperador() == null) {
            return false;
        }
        if (condicion.getMoneda() != monto.moneda()) {
            return false;
        }

        int comparacion = monto.importe().compareTo(valor);
        return switch (condicion.getOperador()) {
            case MAYOR -> comparacion > 0;
            case MAYOR_IGUAL -> comparacion >= 0;
            case MENOR -> comparacion < 0;
            case MENOR_IGUAL -> comparacion <= 0;
        };
    }
}