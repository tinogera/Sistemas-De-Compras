package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SimulacionRequest(
        @NotNull @DecimalMin("0") BigDecimal monto,
        @NotNull Moneda moneda,
        Long sectorId,
        Long categoriaId) {
}
