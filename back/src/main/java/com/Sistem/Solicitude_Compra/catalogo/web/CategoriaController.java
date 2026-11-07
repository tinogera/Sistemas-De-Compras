package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.servicio.CategoriaService;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {
    private final CategoriaService categorias;
    private final UsuarioActual usuarioActual;

    public CategoriaController(CategoriaService categorias, UsuarioActual usuarioActual) {
        this.categorias = categorias;
        this.usuarioActual = usuarioActual;
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

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ENCARGADO')")
    public CategoriaResponse confirmar(@PathVariable Long id, @Valid @RequestBody ActualizarCategoriaRequest pedido) {
        return categorias.confirmar(id, pedido);
    }

    @PostMapping("/{id}/unificar")
    @PreAuthorize("hasRole('ENCARGADO')")
    public CategoriaResponse unificar(@PathVariable Long id, @Valid @RequestBody UnificarCategoriaRequest pedido) {
        return categorias.unificar(id, pedido.destinoId(), usuarioActual.id());
    }
}
