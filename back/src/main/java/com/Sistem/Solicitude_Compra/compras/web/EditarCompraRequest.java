package com.Sistem.Solicitude_Compra.compras.web;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Los dos importes son opcionales (al menos uno). Un envío en 0 deja el ítem sin envío. */
public record EditarCompraRequest(
        @Positive @Digits(integer = 14, fraction = 2) BigDecimal precioTotal,
        @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal montoEnvio,
        @NotNull Long version) {
}
