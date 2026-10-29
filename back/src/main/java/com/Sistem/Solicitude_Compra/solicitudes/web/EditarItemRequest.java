package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Cuerpo del PATCH de un ítem. Todos los campos son opcionales salvo {@code version}:
 * lo que viene en null no se modifica.
 */
public record EditarItemRequest(
        Long categoriaId,
        @Size(max = 120, message = "El nombre de la categoría no puede pasar los 120 caracteres") String categoriaNueva,
        @DecimalMin(value = "0.00", inclusive = false, message = "La cantidad debe ser mayor a 0")
        @Digits(integer = 10, fraction = 2, message = "La cantidad admite hasta 10 enteros y 2 decimales") BigDecimal cantidad,
        String detalle,
        @DecimalMin(value = "0.00", inclusive = false, message = "El precio estimado debe ser mayor a 0")
        @Digits(integer = 14, fraction = 2, message = "El precio estimado tiene demasiados dígitos") BigDecimal precioEstimado,
        Moneda monedaEstimada,
        @NotNull(message = "La versión es obligatoria") Long version) {

    @AssertTrue(message = "Indicá una categoría existente o una categoría nueva, pero no las dos")
    public boolean isCategoriaValida() {
        return categoriaId == null || !tieneCategoriaNueva();
    }

    @AssertTrue(message = "Indicá al menos un campo para modificar")
    public boolean isHayCambios() {
        return categoriaId != null || tieneCategoriaNueva() || cantidad != null || tieneDetalle()
                || precioEstimado != null || monedaEstimada != null;
    }

    public boolean tieneCategoriaNueva() {
        return categoriaNueva != null && !categoriaNueva.isBlank();
    }

    public boolean tieneDetalle() {
        return detalle != null && !detalle.isBlank();
    }
}
