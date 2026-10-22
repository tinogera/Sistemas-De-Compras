package com.Sistem.Solicitude_Compra.catalogo.servicio;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.ActualizarProveedorRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.AltaProveedorRequest;
import com.Sistem.Solicitude_Compra.catalogo.web.ProveedorResponse;
import com.Sistem.Solicitude_Compra.compartido.errores.CategoriaInvalidaException;
import com.Sistem.Solicitude_Compra.compartido.errores.CuitDuplicadoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ProveedorNoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProveedorService {

    private static final Sort ORDEN_POR_DEFECTO = Sort.by("razonSocial");

    private final ProveedorRepository proveedores;
    private final CategoriaRepository categorias;

    public ProveedorService(ProveedorRepository proveedores, CategoriaRepository categorias) {
        this.proveedores = proveedores;
        this.categorias = categorias;
    }

    @Transactional(readOnly = true)
    public Pagina<ProveedorResponse> listar(String q, Long categoriaId, Boolean activo, Pageable pageable) {
        Pageable pagina = pageable.getSort().isUnsorted()
                ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_POR_DEFECTO)
                : pageable;
        String texto = textoDeBusqueda(q);
        Page<ProveedorResponse> resultado = proveedores.buscar(texto, categoriaId, activo, pagina)
                .map(ProveedorResponse::de);
        return Pagina.de(resultado);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.de(buscarOFallar(id));
    }

    public ProveedorResponse alta(AltaProveedorRequest pedido) {
        String cuit = textoOpcional(pedido.cuit());
        asegurarCuitLibre(cuit, null);

        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial(pedido.razonSocial().trim());
        proveedor.setCuit(cuit);
        proveedor.setTelefono(textoOpcional(pedido.telefono()));
        proveedor.setEmail(textoOpcional(pedido.email()));
        proveedor.setContacto(textoOpcional(pedido.contacto()));
        proveedor.setActivo(true);
        proveedor.setCategorias(new HashSet<>(categoriasDe(pedido.categoriaIds())));
        return ProveedorResponse.de(proveedores.save(proveedor));
    }

    public ProveedorResponse actualizar(Long id, ActualizarProveedorRequest pedido) {
        Proveedor proveedor = buscarOFallar(id);

        if (pedido.razonSocial() != null) {
            proveedor.setRazonSocial(pedido.razonSocial().trim());
        }
        if (pedido.cuit() != null) {
            String cuit = textoOpcional(pedido.cuit());
            asegurarCuitLibre(cuit, id);
            proveedor.setCuit(cuit);
        }
        if (pedido.telefono() != null) {
            proveedor.setTelefono(textoOpcional(pedido.telefono()));
        }
        if (pedido.email() != null) {
            proveedor.setEmail(textoOpcional(pedido.email()));
        }
        if (pedido.contacto() != null) {
            proveedor.setContacto(textoOpcional(pedido.contacto()));
        }
        if (pedido.activo() != null) {
            proveedor.setActivo(pedido.activo());
        }
        if (pedido.categoriaIds() != null) {
            proveedor.setCategorias(new HashSet<>(categoriasDe(pedido.categoriaIds())));
        }
        return ProveedorResponse.de(proveedores.save(proveedor));
    }

    private Proveedor buscarOFallar(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new ProveedorNoEncontradoException(id));
    }

    private void asegurarCuitLibre(String cuit, Long idActual) {
        if (cuit == null) {
            return;
        }
        boolean repetido = idActual == null
                ? proveedores.existsByCuit(cuit)
                : proveedores.existsByCuitAndIdNot(cuit, idActual);
        if (repetido) {
            throw new CuitDuplicadoException(cuit);
        }
    }

    private List<Categoria> categoriasDe(List<Long> categoriaIds) {
        if (categoriaIds == null || categoriaIds.isEmpty()) {
            throw new CategoriaInvalidaException("El proveedor tiene que tener al menos una categoría.");
        }
        Set<Long> ids = new LinkedHashSet<>(categoriaIds);
        List<Categoria> encontradas = categorias.findAllById(ids);
        if (encontradas.size() != ids.size()) {
            throw new CategoriaInvalidaException("Hay una categoría que no existe.");
        }
        return encontradas;
    }

    private static String textoDeBusqueda(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        return q.trim();
    }

    private static String textoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
