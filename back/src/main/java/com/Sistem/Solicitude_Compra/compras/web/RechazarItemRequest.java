package com.Sistem.Solicitude_Compra.compras.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RechazarItemRequest(@NotBlank @Size(max = 1000) String motivo, @NotNull Long version) {
}
