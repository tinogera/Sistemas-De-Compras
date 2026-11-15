package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body de {@code PATCH /api/v1/categorias/{id}}: el estado es obligatorio; el nombre, opcional. */
public record ActualizarCategoriaRequest(
        @NotNull EstadoCategoria estado,
        @Size(min = 1, max = 120) String nombre) {
}
