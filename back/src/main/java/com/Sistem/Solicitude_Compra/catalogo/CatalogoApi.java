package com.Sistem.Solicitude_Compra.catalogo;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoApi {
    private final CategoriaRepository categorias;
    private final ProveedorRepository proveedores;
    private final ApplicationEventPublisher eventos;

    public CatalogoApi(CategoriaRepository categorias, ProveedorRepository proveedores,
                       ApplicationEventPublisher eventos) {
        this.categorias = categorias;
        this.proveedores = proveedores;
        this.eventos = eventos;
    }

    public boolean existeCategoria(Long id) {
        return id != null && categorias.existsById(id);
    }

    @Transactional
    public Long obtenerOCrearCategoria(TipoItem tipo, String nombre) {
        if (tipo == null || nombre == null || nombre.isBlank() || nombre.trim().length() > 120) {
            throw new ParametroInvalidoException("Debe indicar tipo y un nombre de categoria de hasta 120 caracteres.");
        }
        String texto = nombre.trim();
        return categorias.findByTipoAndNombreIgnoreCase(tipo, texto)
                .map(categoria -> destinoDe(categoria).getId())
                .orElseGet(() -> {
                    Categoria nueva = new Categoria();
                    nueva.setTipo(tipo);
                    nueva.setNombre(texto);
                    nueva.setEstado(EstadoCategoria.NUEVA);
                    Categoria guardada = categorias.save(nueva);
                    eventos.publishEvent(new CategoriaCreada(UUID.randomUUID(), Instant.now(),
                            guardada.getId(), guardada.getNombre(), guardada.getTipo()));
                    return guardada.getId();
                });
    }

    public String nombreCategoria(Long id) {
        return buscarCategoria(id).getNombre();
    }

    public ProveedorResumen obtenerProveedor(Long id) {
        if (id == null) {
            throw new NoEncontradoException("Debe indicar el proveedor.");
        }
        return resumenDe(proveedores.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe el proveedor " + id)));
    }

    public List<ProveedorResumen> proveedoresActivosDe(Long categoriaId) {
        Long destinoId = destinoDe(buscarCategoria(categoriaId)).getId();
        return proveedores.findByActivoTrueAndCategorias_Id(destinoId)
                .stream().map(CatalogoApi::resumenDe).toList();
    }

    private Categoria buscarCategoria(Long id) {
        if (id == null) {
            throw new NoEncontradoException("Debe indicar la categoria.");
        }
        return categorias.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la categoria " + id));
    }

    private Categoria destinoDe(Categoria categoria) {
        Set<Long> visitadas = new HashSet<>();
        while (categoria.getEstado() == EstadoCategoria.UNIFICADA) {
            if (!visitadas.add(categoria.getId()) || categoria.getUnificadaEn() == null) {
                throw new IllegalStateException("La categoria unificada no tiene un destino valido.");
            }
            categoria = categoria.getUnificadaEn();
        }
        return categoria;
    }

    private static ProveedorResumen resumenDe(Proveedor proveedor) {
        return new ProveedorResumen(proveedor.getId(), proveedor.getRazonSocial(), proveedor.isActivo());
    }
}