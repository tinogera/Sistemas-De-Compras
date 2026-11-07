package com.Sistem.Solicitude_Compra.catalogo.servicio;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.ActualizarCategoriaRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.CategoriaResponse;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository categorias;
    private final ProveedorRepository proveedores;
    private final ApplicationEventPublisher eventos;

    public CategoriaService(CategoriaRepository categorias, ProveedorRepository proveedores,
                            ApplicationEventPublisher eventos) {
        this.categorias = categorias;
        this.proveedores = proveedores;
        this.eventos = eventos;
    }

    public List<CategoriaResponse> buscar(TipoItem tipo, String q) {
        if (tipo == null || q == null) {
            throw new ParametroInvalidoException("La busqueda requiere tipo y q.");
        }
        String texto = q.trim();
        if (texto.length() < 2) {
            return List.of();
        }
        return categorias.findTop10ByTipoAndNombreContainingIgnoreCaseAndEstadoNot(
                tipo, texto, EstadoCategoria.UNIFICADA, Sort.by("nombre"))
                .stream().map(CategoriaResponse::de).toList();
    }

    public Pagina<CategoriaResponse> buscar(EstadoCategoria estado, Pageable pageable) {
        if (estado == null) {
            throw new ParametroInvalidoException("Debe indicar q o estado.");
        }
        Pageable pagina = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("nombre"))
                : pageable;
        return Pagina.de(categorias.findByEstado(estado, pagina).map(CategoriaResponse::de));
    }

    /** T06.6: NUEVA pasa a CONOCIDA y, si viene, se corrige el nombre. */
    @Transactional
    public CategoriaResponse confirmar(Long id, ActualizarCategoriaRequest pedido) {
        Categoria categoria = buscarPorId(id);
        if (pedido.estado() != EstadoCategoria.CONOCIDA) {
            throw new ReglaNegocioException("TRANSICION_INVALIDA",
                    "Solo se puede pasar una categoría de NUEVA a CONOCIDA.");
        }
        String nombre = null;
        if (pedido.nombre() != null) {
            nombre = pedido.nombre().trim();
            if (nombre.isEmpty()) {
                throw new ParametroInvalidoException("El nombre no puede estar vacío.");
            }
            // Si choca con otra categoría del mismo tipo corresponde unificarlas (T06.8).
            categorias.findByTipoAndNombreIgnoreCase(categoria.getTipo(), nombre)
                    .filter(otra -> !otra.getId().equals(categoria.getId()))
                    .ifPresent(otra -> {
                        throw new ConflictoException("DUPLICADO",
                                "Ya existe la categoría \"" + otra.getNombre() + "\"; unificalas en vez de renombrar.");
                    });
        }
        // Recién acá se cambia el estado: si algo de arriba falla, la entidad queda intacta.
        categoria.confirmar();
        if (nombre != null) {
            categoria.setNombre(nombre);
        }
        return CategoriaResponse.de(categorias.saveAndFlush(categoria));
    }

    /**
     * T06.8: la categoría origen queda UNIFICADA en la destino. Los proveedores pasan a la destino y los ítems
     * viejos no se modifican (siguen guardando el categoriaId que tenían).
     */
    @Transactional
    public CategoriaResponse unificar(Long origenId, Long destinoId, Long usuarioId) {
        Categoria origen = buscarPorId(origenId);
        Categoria destino = buscarPorId(destinoId);
        origen.unificarEn(destino);

        // Para que unificadaEn siga siendo de un solo salto: lo que ya apuntaba al origen apunta ahora al destino.
        categorias.findByUnificadaEn(origen).forEach(previa -> previa.setUnificadaEn(destino));
        // Set de categorías: si el proveedor ya tenía la destino no se duplica la fila.
        proveedores.findByCategorias_Id(origen.getId()).forEach(proveedor -> {
            proveedor.getCategorias().remove(origen);
            proveedor.getCategorias().add(destino);
        });

        categorias.saveAndFlush(origen);
        eventos.publishEvent(new CategoriaUnificada(UUID.randomUUID(), Instant.now(),
                origen.getId(), destino.getId(), usuarioId));
        return CategoriaResponse.de(origen);
    }

    private Categoria buscarPorId(Long id) {
        return categorias.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la categoría " + id));
    }
}
