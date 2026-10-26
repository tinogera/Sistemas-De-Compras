package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
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
    private final AuthApi authApi;
    private final CatalogoApi catalogoApi;
    private final ApplicationEventPublisher eventos;

    public SolicitudService(SolicitudRepository solicitudes, AuthApi authApi, CatalogoApi catalogoApi,
                            ApplicationEventPublisher eventos) {
        this.solicitudes = solicitudes;
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
