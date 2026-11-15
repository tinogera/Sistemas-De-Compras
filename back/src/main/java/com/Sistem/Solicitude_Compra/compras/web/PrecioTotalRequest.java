package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PrecioTotalRequest(
        @NotNull @Positive @Digits(integer = 14, fraction = 2) BigDecimal precioTotal,
        @NotNull Moneda moneda,
        @NotNull Long version) {
}
