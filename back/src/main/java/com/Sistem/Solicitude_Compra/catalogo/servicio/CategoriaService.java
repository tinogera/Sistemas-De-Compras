package com.Sistem.Solicitude_Compra.catalogo.servicio;

import com.Sistem.Solicitude_Compra.catalogo.dominio.enums.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.web.CategoriaResponse;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {
    private final CategoriaRepository categorias;

    public CategoriaService(CategoriaRepository categorias) {
        this.categorias = categorias;
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
}
