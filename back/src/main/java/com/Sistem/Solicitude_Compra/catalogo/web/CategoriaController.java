package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.servicio.CategoriaService;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {
    private final CategoriaService categorias;

    public CategoriaController(CategoriaService categorias) {
        this.categorias = categorias;
    }

    @GetMapping
    public Object buscar(
            @RequestParam(required = false) TipoItem tipo,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoCategoria estado,
            Pageable pageable) {
        // Con q presente se usa el modo autocompletado, incluso si esta vacio.
        return q != null ? categorias.buscar(tipo, q) : categorias.buscar(estado, pageable);
    }
}
