package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.enums.EstadoCategoria;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;

public record CategoriaResponse(Long id, String nombre, TipoItem tipo, EstadoCategoria estado) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNombre(),
                categoria.getTipo(), categoria.getEstado());
    }
}
