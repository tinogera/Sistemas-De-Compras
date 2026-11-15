package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;

public record CategoriaResumenResponse(Long id, String nombre) {

    public static CategoriaResumenResponse de(Categoria categoria) {
        return new CategoriaResumenResponse(categoria.getId(), categoria.getNombre());
    }
}
