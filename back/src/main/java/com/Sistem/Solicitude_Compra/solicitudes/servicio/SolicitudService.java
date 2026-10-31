package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.ConflictoException;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCancelada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.EditarItemRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.SolicitudDetalleResponse;
import com.Sistem.Solicitude_Compra.solicitudes.web.SolicitudDetalleResponse.ItemDetalle;
import com.Sistem.Solicitude_Compra.solicitudes.web.SolicitudResumenResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SolicitudService {

    private final SolicitudRepository solicitudes;
    private final ItemRepository items;
    private final AuthApi authApi;
    private final CatalogoApi catalogoApi;
    private final ApplicationEventPublisher eventos;

    public SolicitudService(SolicitudRepository solicitudes, ItemRepository items, AuthApi authApi,
                            CatalogoApi catalogoApi, ApplicationEventPublisher eventos) {
        this.solicitudes = solicitudes;
        this.items = items;
        this.authApi = authApi;
        this.catalogoApi = catalogoApi;
        this.eventos = eventos;
    }

    @Transactional
    public Solicitud crear(Long solicitanteId, CrearSolicitudRequest request) {
        Solicitud solicitud = new Solicitud();
        solicitud.setSolicitanteId(solicitanteId);
        // El sector se copia: si el usuario cambia de sector, las solicitudes viejas no cambian.
        solicitud.setSectorId(authApi.sectorDe(solicitanteId));
        solicitud.setUrgencia(request.urgencia());
        solicitud.setFechaNecesaria(request.fechaNecesaria());
        solicitud.setObservaciones(request.observaciones());
        request.items().forEach(item -> solicitud.agregarItem(armarItem(item)));

        Solicitud guardada = solicitudes.saveAndFlush(solicitud);
        eventos.publishEvent(new SolicitudCreada(UUID.randomUUID(), Instant.now(), guardada.getId(),
                guardada.getSolicitanteId(), guardada.getSectorId(), guardada.getUrgencia(),
                guardada.getFechaNecesaria(),
                guardada.getItems().stream().map(i -> new SolicitudCreada.ItemCreado(i.getId(), i.getTipo(),
                        i.getCategoriaId(), i.getCantidad(), i.getDetalle(), i.getPrecioEstimado(),
                        i.getMonedaEstimada())).toList()));
        return guardada;
    }

    public Page<SolicitudResumenResponse> listar(Long usuarioId, Rol rol, boolean mias, EstadoSolicitud estado,
                                                 Pageable pageable) {
        // Un Solicitante siempre ve solo las suyas.
        boolean soloMias = mias || rol == Rol.SOLICITANTE;
        Page<Solicitud> pagina;
        if (soloMias) {
            pagina = estado == null
                    ? solicitudes.findBySolicitanteId(usuarioId, pageable)
                    : solicitudes.findBySolicitanteIdAndEstado(usuarioId, estado, pageable);
        } else {
            pagina = estado == null ? solicitudes.findAll(pageable) : solicitudes.findByEstado(estado, pageable);
        }
        return pagina.map(s -> new SolicitudResumenResponse(s.getId(), s.getCreadaEn(), s.getUrgencia(),
                s.getFechaNecesaria(), s.getEstado(), s.getItems().size()));
    }

    public SolicitudDetalleResponse obtener(Long id, Long usuarioId, Rol rol) {
        Solicitud s = solicitudes.findConItemsById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la solicitud " + id));
        if (rol == Rol.SOLICITANTE && !s.getSolicitanteId().equals(usuarioId)) {
            throw new SinPermisoException("No tenés permiso para ver esta solicitud");
        }
        UsuarioResumen solicitante = authApi.obtenerUsuario(s.getSolicitanteId());
        return new SolicitudDetalleResponse(s.getId(),
                new SolicitudDetalleResponse.Solicitante(solicitante.id(), solicitante.nombre()),
                authApi.nombreSector(s.getSectorId()), s.getUrgencia(), s.getFechaNecesaria(),
                s.getObservaciones(), s.getEstado(), s.getCreadaEn(), s.getVersion(),
                s.getItems().stream().map(this::detalleDe).toList());
    }

    /**
     * Edita un ítem mientras siga PENDIENTE. Solo el dueño de la solicitud puede hacerlo y la
     * versión que manda el cliente tiene que ser la actual (si no, alguien lo cambió antes).
     */
    @Transactional
    public ItemDetalle editarItem(Long solicitudId, Long itemId, Long usuarioId, EditarItemRequest request) {
        Item item = buscarItemEditable(solicitudId, itemId, usuarioId, request.version());

        if (request.categoriaId() != null) {
            if (!catalogoApi.existeCategoria(request.categoriaId())) {
                throw new ReglaNegocioException("CATEGORIA_INEXISTENTE",
                        "La categoría " + request.categoriaId() + " no existe");
            }
            item.setCategoriaId(request.categoriaId());
        } else if (request.tieneCategoriaNueva()) {
            item.setCategoriaId(catalogoApi.obtenerOCrearCategoria(item.getTipo(), request.categoriaNueva().trim()));
        }
        if (request.cantidad() != null) {
            item.setCantidad(request.cantidad());
        }
        if (request.tieneDetalle()) {
            item.setDetalle(request.detalle().trim());
        }
        if (request.precioEstimado() != null) {
            item.setPrecioEstimado(request.precioEstimado());
        }
        if (request.monedaEstimada() != null) {
            item.setMonedaEstimada(request.monedaEstimada());
        }

        // saveAndFlush: el @Version se incrementa acá y la respuesta ya trae la versión nueva.
        Item guardado = items.saveAndFlush(item);
        eventos.publishEvent(new ItemEditado(UUID.randomUUID(), Instant.now(), guardado.getId(), solicitudId,
                guardado.getCategoriaId(), guardado.getCantidad(), guardado.getDetalle(),
                guardado.getPrecioEstimado(), guardado.getMonedaEstimada(), usuarioId));
        return detalleDe(guardado);
    }

    /**
     * Cancela un ítem PENDIENTE (no se borra la fila: pasa a CANCELADO). Mismas reglas que editarlo.
     * Si todos los ítems de la solicitud quedan cancelados, la solicitud pasa a CANCELADA.
     */
    @Transactional
    public void cancelarItem(Long solicitudId, Long itemId, Long usuarioId, Long version) {
        Item item = buscarItemEditable(solicitudId, itemId, usuarioId, version);
        item.setEstado(EstadoItem.CANCELADO);
        items.saveAndFlush(item);

        Solicitud solicitud = item.getSolicitud();
        if (solicitud.getItems().stream().allMatch(i -> i.getEstado() == EstadoItem.CANCELADO)) {
            solicitud.setEstado(EstadoSolicitud.CANCELADA);
            solicitudes.save(solicitud);
        }
        eventos.publishEvent(new ItemCancelado(UUID.randomUUID(), Instant.now(), item.getId(), solicitudId,
                usuarioId));
    }

    /**
     * Cancela la solicitud entera. Solo la puede cancelar quien la creó y solo si todos sus ítems siguen
     * PENDIENTES; si no, hay que cancelar ítem por ítem. Todos los ítems pasan a CANCELADO.
     */
    @Transactional
    public void cancelarSolicitud(Long solicitudId, Long usuarioId, Long version) {
        Solicitud solicitud = solicitudes.findConItemsById(solicitudId)
                .orElseThrow(() -> new NoEncontradoException("No existe la solicitud " + solicitudId));
        if (!solicitud.getSolicitanteId().equals(usuarioId)) {
            throw new SinPermisoException("Solo quien creó la solicitud puede cancelarla");
        }
        if (solicitud.getEstado() == EstadoSolicitud.CANCELADA) {
            throw new ReglaNegocioException("SOLICITUD_NO_CANCELABLE", "La solicitud ya está cancelada");
        }
        if (!solicitud.puedeCancelarse()) {
            throw new ReglaNegocioException("SOLICITUD_NO_CANCELABLE",
                    "La solicitud tiene ítems que ya no están pendientes. Cancelá ítem por ítem");
        }
        if (!solicitud.getVersion().equals(version)) {
            throw new ConflictoException("CONFLICTO_VERSION",
                    "Otro usuario modificó esta solicitud. Recargá la página.");
        }
        solicitud.getItems().forEach(i -> i.setEstado(EstadoItem.CANCELADO));
        solicitud.setEstado(EstadoSolicitud.CANCELADA);
        solicitudes.saveAndFlush(solicitud);
        eventos.publishEvent(new SolicitudCancelada(UUID.randomUUID(), Instant.now(), solicitudId,
                solicitud.getItems().stream().map(Item::getId).toList(), usuarioId));
    }

    /** Reglas comunes de editar y cancelar: existe, es el dueño, sigue PENDIENTE y la versión coincide. */
    private Item buscarItemEditable(Long solicitudId, Long itemId, Long usuarioId, Long version) {
        Item item = items.findByIdAndSolicitudId(itemId, solicitudId)
                .orElseThrow(() -> new NoEncontradoException(
                        "No existe el ítem " + itemId + " en la solicitud " + solicitudId));
        if (!item.getSolicitud().getSolicitanteId().equals(usuarioId)) {
            throw new SinPermisoException("Solo quien creó la solicitud puede editar sus ítems");
        }
        if (!item.puedeEditarse()) {
            throw new ReglaNegocioException("ITEM_NO_PENDIENTE", "Este ítem ya está siendo cotizado");
        }
        if (!item.getVersion().equals(version)) {
            throw new ConflictoException("CONFLICTO_VERSION",
                    "Otro usuario modificó este ítem. Recargá la página.");
        }
        return item;
    }

    private ItemDetalle detalleDe(Item i) {
        return new ItemDetalle(i.getId(), i.getTipo(), i.getCategoriaId(),
                catalogoApi.nombreCategoria(i.getCategoriaId()), i.getDetalle(), i.getCantidad(),
                i.getPrecioEstimado(), i.getMonedaEstimada(), i.getPrecioTotal(), i.getMonedaTotal(),
                i.getEstado(), i.getEstadoAprobacion(), i.getProveedorComprado(), i.getMotivoRechazo(),
                i.getRechazadoPor(), i.getVersion());
    }

    private Item armarItem(ItemRequest request) {
        Long categoriaId;
        String nombreCategoria;
        if (request.categoriaId() != null) {
            if (!catalogoApi.existeCategoria(request.categoriaId())) {
                throw new ReglaNegocioException("CATEGORIA_INEXISTENTE",
                        "La categoría " + request.categoriaId() + " no existe");
            }
            categoriaId = request.categoriaId();
            nombreCategoria = null;
        } else {
            nombreCategoria = request.categoriaNueva().trim();
            categoriaId = catalogoApi.obtenerOCrearCategoria(request.tipo(), nombreCategoria);
        }
        Item item = new Item();
        item.setTipo(request.tipo());
        item.setCategoriaId(categoriaId);
        item.setCantidad(request.cantidad());
        item.setPrecioEstimado(request.precioEstimado());
        item.setMonedaEstimada(request.monedaEstimada());
        // La columna es NOT NULL: sin detalle se usa el nombre de la categoría.
        item.setDetalle(request.detalle() != null && !request.detalle().isBlank()
                ? request.detalle().trim()
                : nombreCategoria != null ? nombreCategoria : catalogoApi.nombreCategoria(categoriaId));
        return item;
    }
}
