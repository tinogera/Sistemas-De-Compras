package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemSpecs;
import com.Sistem.Solicitude_Compra.compras.web.BandejaItemResponse;
import com.Sistem.Solicitude_Compra.compras.web.GestionItemDetalleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@Transactional(readOnly = true)
public class BandejaComprasService {

    private static final Sort ORDEN_BANDEJA = Sort.by(
            Sort.Order.asc("prioridad"),
            Sort.Order.asc("fechaNecesaria"),
            Sort.Order.asc("itemId"));

    private final GestionItemRepository repository;
    private final CatalogoApi catalogoApi;
    private final AuthApi authApi;

    public BandejaComprasService(GestionItemRepository repository, CatalogoApi catalogoApi, AuthApi authApi) {
        this.repository = repository;
        this.catalogoApi = catalogoApi;
        this.authApi = authApi;
    }

    public Page<BandejaItemResponse> listar(Boolean asignadosAMi, Urgencia urgencia, Boolean paraComprar,
                                            Long usuarioId, Pageable pageable) {
        Long encargadoId = Boolean.TRUE.equals(asignadosAMi) ? usuarioId : null;
        Short prioridad = urgencia == null ? null : (short) urgencia.prioridad();
        Specification<GestionItem> filtros =
                GestionItemSpecs.bandeja(encargadoId, prioridad, Boolean.TRUE.equals(paraComprar));
        Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ORDEN_BANDEJA);

        Map<Long, String> categorias = new HashMap<>();
        Map<Long, String> encargados = new HashMap<>();
        return repository.findAll(filtros, ordenado)
                .map(item -> aBandejaItem(item,
                        nombreDe(item.getCategoriaId(), categorias, catalogoApi::nombreCategoria),
                        nombreDe(item.getEncargadoId(), encargados, id -> authApi.obtenerUsuario(id).nombre())));
    }

    public GestionItemDetalleResponse obtener(Long itemId) {
        GestionItem item = repository.findById(itemId)
                .orElseThrow(() -> new NoEncontradoException("GestionItem no encontrado"));
        return aDetalle(item);
    }

    private static String nombreDe(Long id, Map<Long, String> cache, Function<Long, String> buscador) {
        if (id == null) {
            return null;
        }
        return cache.computeIfAbsent(id, buscador);
    }

    private static BandejaItemResponse aBandejaItem(GestionItem i, String categoriaNombre, String encargadoNombre) {
        return new BandejaItemResponse(i.getItemId(), i.getSolicitudId(), i.getTipo(), i.getCategoriaId(),
                categoriaNombre, i.getDetalle(), i.getCantidad(), i.getUrgencia(), i.getFechaNecesaria(),
                i.getPrecioEstimado(), i.getMonedaEstimada(), i.getPrecioTotal(), i.getMonedaTotal(),
                i.getEstado(), i.getEstadoAprobacion(), i.getEncargadoId(), encargadoNombre, i.getVersion());
    }

    private GestionItemDetalleResponse aDetalle(GestionItem i) {
        String categoriaNombre = i.getCategoriaId() == null ? null : catalogoApi.nombreCategoria(i.getCategoriaId());
        String encargadoNombre = i.getEncargadoId() == null ? null : authApi.obtenerUsuario(i.getEncargadoId()).nombre();
        return new GestionItemDetalleResponse(i.getItemId(), i.getSolicitudId(), i.getTipo(), i.getCategoriaId(),
                categoriaNombre, i.getDetalle(), i.getCantidad(), i.getUrgencia(), i.getFechaNecesaria(),
                i.getSolicitadoEn(), i.getPrecioEstimado(), i.getMonedaEstimada(), i.getPrecioTotal(),
                i.getMonedaTotal(), i.getMontoEnvio(), i.getEstado(), i.getEstadoAprobacion(), i.getEncargadoId(),
                encargadoNombre, i.getCotizacionElegidaId(), i.getVersion());
    }
}