package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ItemRequest(
        @NotNull(message = "El tipo es obligatorio") TipoItem tipo,
        Long categoriaId,
        @Size(max = 120, message = "El nombre de la categoría no puede pasar los 120 caracteres") String categoriaNueva,
        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.00", inclusive = false, message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 10 enteros y 2 decimales") BigDecimal cantidad,
        String detalle,
        @NotNull(message = "El precio estimado es obligatorio")
        @DecimalMin(value = "0.00", inclusive = false, message = "El precio estimado debe ser mayor a 0")
        @Digits(integer = 14, fraction = 2, message = "El precio estimado tiene demasiados dígitos") BigDecimal precioEstimado,
        @NotNull(message = "La moneda del precio estimado es obligatoria") Moneda monedaEstimada) {

    @AssertTrue(message = "Indicá una categoría existente o una categoría nueva, pero no las dos")
    public boolean isCategoriaValida() {
        boolean nueva = categoriaNueva != null && !categoriaNueva.isBlank();
        return (categoriaId != null) != nueva;
    }
}
