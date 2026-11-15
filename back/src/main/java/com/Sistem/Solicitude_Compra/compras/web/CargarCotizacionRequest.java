package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CargarCotizacionRequest(
        @NotNull Long proveedorId,
        @NotNull Moneda moneda,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal precioUnitario,
        @NotNull LocalDate fechaValidez,
        LocalDate entregaEstimada,
        @Size(max = 2000) String observaciones) {
}
