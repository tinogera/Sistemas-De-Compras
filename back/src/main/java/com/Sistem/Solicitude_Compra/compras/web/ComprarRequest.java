package com.Sistem.Solicitude_Compra.compras.web;

import jakarta.validation.constraints.NotNull;

/** El precio no viaja: se compra al precio total que tiene la gestión, el mismo que miraron los aprobadores. */
public record ComprarRequest(Long proveedorId, @NotNull Long version) {
}
