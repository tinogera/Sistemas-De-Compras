package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.ProveedorResumen;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CotizacionCargada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.compras.web.CargarCotizacionRequest;
import com.Sistem.Solicitude_Compra.compras.web.ComprarRequest;
import com.Sistem.Solicitude_Compra.compras.web.EditarCompraRequest;
import com.Sistem.Solicitude_Compra.compras.web.PrecioTotalRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Operaciones del Encargado sobre una gestión: tomar, cotizar, fijar el precio, rechazar y comprar.
 * Cada operación es una transacción; los eventos se publican dentro de ella (Modulith los entrega al commit).
 */
@Service
@Transactional
public class GestionComprasService {

    static final String RECHAZADO_POR_ENCARGADO = "Encargado de compras";

    private final GestionItemRepository gestiones;
    private final CotizacionRepository cotizaciones;
    private final CatalogoApi catalogoApi;
    private final ApplicationEventPublisher eventos;

    public GestionComprasService(GestionItemRepository gestiones, CotizacionRepository cotizaciones,
                                 CatalogoApi catalogoApi, ApplicationEventPublisher eventos) {
        this.gestiones = gestiones;
        this.cotizaciones = cotizaciones;
        this.catalogoApi = catalogoApi;
        this.eventos = eventos;
    }

    // ---- T09.4 · Tomar y reasignar ----

    public void tomar(Long itemId, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        Long anterior = gestion.getEncargadoId();
        if (gestion.tomar(encargadoId)) {
            gestiones.save(gestion);
            publicarAsignacion(gestion, anterior, encargadoId);
        }
    }

    public void reasignar(Long itemId, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        Long anterior = gestion.getEncargadoId();
        gestion.reasignar(encargadoId);
        gestiones.save(gestion);
        publicarAsignacion(gestion, anterior, encargadoId);
    }

    // ---- T10.2 · Cotizaciones ----

    public Cotizacion cargarCotizacion(Long itemId, CargarCotizacionRequest pedido, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        gestion.exigirAsignadoA(encargadoId);
        if (pedido.fechaValidez().isBefore(hoy())) {
            throw new ParametroInvalidoException("La cotización no puede estar vencida: revisá la fecha de validez.");
        }
        ProveedorResumen proveedor = proveedorActivo(pedido.proveedorId());

        BigDecimal unitario = pedido.precioUnitario().setScale(2, RoundingMode.HALF_UP);
        if (unitario.signum() <= 0) {
            throw new ParametroInvalidoException("El precio unitario tiene que ser mayor a cero.");
        }
        // Siempre con BigDecimal, nunca con double: total = unitario × cantidad, a 2 decimales.
        BigDecimal total = unitario.multiply(gestion.getCantidad()).setScale(2, RoundingMode.HALF_UP);

        Cotizacion cotizacion = cotizaciones.save(new Cotizacion(itemId, proveedor.id(), proveedor.razonSocial(),
                pedido.moneda(), unitario, total, pedido.fechaValidez(), pedido.entregaEstimada(),
                pedido.observaciones(), encargadoId, OffsetDateTime.now(ZoneOffset.UTC)));
        eventos.publishEvent(new CotizacionCargada(UUID.randomUUID(), Instant.now(), cotizacion.getId(), itemId,
                proveedor.id(), cotizacion.getMoneda(), total, encargadoId));
        return cotizacion;
    }

    // ---- T10.3 · Precio total ----

    public void elegirCotizacion(Long itemId, Long cotizacionId, Long version, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        gestion.exigirAsignadoA(encargadoId);
        Cotizacion elegida = cotizaciones.findByIdAndItemId(cotizacionId, itemId)
                .orElseThrow(() -> new NoEncontradoException("La cotización " + cotizacionId + " no es de este ítem."));
        if (!elegida.estaVigente(hoy())) {
            throw new ReglaNegocioException("COTIZACION_VENCIDA",
                    "La cotización venció el " + elegida.getFechaValidez() + ". Cargá una nueva.");
        }
        exigirVersion(gestion, version);

        // El índice único ux_una_seleccionada no admite dos: la anterior vuelve a CANDIDATA antes de elegir la nueva.
        cotizaciones.findByItemIdAndEstado(itemId, EstadoCotizacion.SELECCIONADA)
                .filter(previa -> !previa.getId().equals(elegida.getId()))
                .ifPresent(previa -> {
                    previa.volverACandidata();
                    cotizaciones.saveAndFlush(previa);
                });
        elegida.seleccionar();
        cotizaciones.save(elegida);
        gestion.fijarPrecioTotal(elegida.getPrecioTotal(), elegida.getMoneda(), elegida.getId());
        gestiones.save(gestion);
        publicarPrecio(gestion, elegida.getProveedorRazonSocial(), encargadoId);
    }

    public void fijarPrecioTotal(Long itemId, PrecioTotalRequest pedido, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        gestion.exigirAsignadoA(encargadoId);
        exigirVersion(gestion, pedido.version());

        liberarCotizacionElegida(gestion);
        gestion.fijarPrecioTotal(pedido.precioTotal().setScale(2, RoundingMode.HALF_UP), pedido.moneda(), null);
        gestiones.save(gestion);
        publicarPrecio(gestion, null, encargadoId);
    }

    public void quitarPrecioTotal(Long itemId, Long version, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        gestion.exigirAsignadoA(encargadoId);
        exigirVersion(gestion, version);

        liberarCotizacionElegida(gestion);
        gestion.quitarPrecioTotal();
        gestiones.save(gestion);
        publicarPrecio(gestion, null, encargadoId);
    }

    // ---- T10.4 · Rechazar ----

    public void rechazar(Long itemId, String motivo, Long version, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        // Sin haber cotizado (PENDIENTE) puede rechazar cualquier Encargado; en cotización, solo el dueño.
        if (gestion.getEstado() == EstadoGestion.EN_COTIZACION) {
            gestion.exigirAsignadoA(encargadoId);
        }
        exigirVersion(gestion, version);
        gestion.rechazar(motivo.trim(), OffsetDateTime.now(ZoneOffset.UTC));
        gestiones.save(gestion);
        publicarResuelto(gestion, "RECHAZADO", null, RECHAZADO_POR_ENCARGADO);
    }

    // ---- T10.7 · Registrar la compra ----

    public void comprar(Long itemId, ComprarRequest pedido, Long encargadoId) {
        GestionItem gestion = buscar(itemId);
        gestion.exigirAsignadoA(encargadoId);
        gestion.exigirListaParaComprar();

        Cotizacion elegida = gestion.getCotizacionElegidaId() == null ? null
                : cotizaciones.findById(gestion.getCotizacionElegidaId()).orElse(null);
        Long proveedorId = proveedorDeLaCompra(elegida, pedido.proveedorId());
        ProveedorResumen proveedor = proveedorActivo(proveedorId);
        // Vigente al momento de comprar, no solo cuando se eligió (caso borde 5).
        if (elegida != null && !elegida.estaVigente(hoy())) {
            throw new ReglaNegocioException("COTIZACION_VENCIDA",
                    "La cotización venció el " + elegida.getFechaValidez() + ". Cargá una nueva.");
        }
        exigirVersion(gestion, pedido.version());

        gestion.registrarCompra(proveedorId, OffsetDateTime.now(ZoneOffset.UTC));
        gestiones.save(gestion);
        cotizaciones.findByItemIdOrderByCargadaEnDesc(itemId).stream()
                .filter(c -> elegida == null || !c.getId().equals(elegida.getId()))
                .forEach(Cotizacion::descartar);

        eventos.publishEvent(new CompraRegistrada(UUID.randomUUID(), Instant.now(), itemId, gestion.getSolicitudId(),
                gestion.getSectorId(), gestion.getCategoriaId(), gestion.getTipo(), gestion.getDetalle(),
                gestion.getCantidad(), proveedorId, proveedor.razonSocial(),
                elegida == null ? null : elegida.getId(), gestion.getMonedaTotal(), gestion.getPrecioTotal(),
                gestion.getEncargadoId()));
        publicarResuelto(gestion, "COMPRADO", proveedor.razonSocial(), null);
    }

    // ---- T10.8 · Editar la compra ----

    public void editarCompra(Long itemId, EditarCompraRequest pedido, Long encargadoId) {
        if (pedido.precioTotal() == null && pedido.montoEnvio() == null) {
            throw new ParametroInvalidoException("Indicá el precio total, el monto de envío o los dos.");
        }
        GestionItem gestion = buscar(itemId);
        gestion.exigirCompraPropia(encargadoId);
        exigirVersion(gestion, pedido.version());

        BigDecimal precioAnterior = gestion.getPrecioTotal();
        BigDecimal envioAnterior = gestion.getMontoEnvio();
        gestion.actualizarCompra(
                pedido.precioTotal() == null ? null : pedido.precioTotal().setScale(2, RoundingMode.HALF_UP),
                pedido.montoEnvio() == null ? null : pedido.montoEnvio().setScale(2, RoundingMode.HALF_UP));
        gestiones.save(gestion);

        // Se publica siempre con anterior y nuevo: es lo que queda en el historial para controlar subas de precio.
        eventos.publishEvent(new CompraActualizada(UUID.randomUUID(), Instant.now(), itemId, gestion.getSolicitudId(),
                precioAnterior, gestion.getPrecioTotal(), envioAnterior, gestion.getMontoEnvio(),
                gestion.getMonedaTotal(), encargadoId));
    }

    // ---- internos ----

    private GestionItem buscar(Long itemId) {
        return gestiones.findById(itemId).orElseThrow(() -> new NoEncontradoException("GestionItem no encontrado"));
    }

    /** La versión que vio el front tiene que ser la actual; si no, alguien tocó el ítem (409 CONFLICTO_VERSION). */
    private static void exigirVersion(GestionItem gestion, Long version) {
        if (version == null || !version.equals(gestion.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(GestionItem.class, gestion.getItemId());
        }
    }

    private static LocalDate hoy() {
        return LocalDate.now(Cotizacion.ZONA);
    }

    /** Si había una cotización elegida, vuelve a CANDIDATA: el precio ya no sale de ella. */
    private void liberarCotizacionElegida(GestionItem gestion) {
        if (gestion.getCotizacionElegidaId() != null) {
            cotizaciones.findById(gestion.getCotizacionElegidaId()).ifPresent(c -> {
                c.volverACandidata();
                cotizaciones.save(c);
            });
        }
    }

    /** Con cotización elegida el proveedor es el de esa cotización; si no, tiene que venir en el pedido. */
    private static Long proveedorDeLaCompra(Cotizacion elegida, Long proveedorPedido) {
        if (elegida != null) {
            if (proveedorPedido != null && !proveedorPedido.equals(elegida.getProveedorId())) {
                throw new ReglaNegocioException("PROVEEDOR_NO_COINCIDE",
                        "El proveedor no coincide con el de la cotización elegida.");
            }
            return elegida.getProveedorId();
        }
        if (proveedorPedido == null) {
            throw new ReglaNegocioException("PROVEEDOR_REQUERIDO",
                    "Indicá el proveedor: no hay una cotización elegida.");
        }
        return proveedorPedido;
    }

    private ProveedorResumen proveedorActivo(Long proveedorId) {
        ProveedorResumen proveedor;
        try {
            proveedor = catalogoApi.obtenerProveedor(proveedorId);
        } catch (NoEncontradoException e) {
            throw new ReglaNegocioException("PROVEEDOR_INACTIVO", "El proveedor no existe.");
        }
        if (!proveedor.activo()) {
            throw new ReglaNegocioException("PROVEEDOR_INACTIVO", "El proveedor está dado de baja.");
        }
        return proveedor;
    }

    private void publicarAsignacion(GestionItem g, Long anterior, Long nuevo) {
        eventos.publishEvent(new ItemAsignado(UUID.randomUUID(), Instant.now(), g.getItemId(), g.getSolicitudId(),
                anterior, nuevo));
    }

    private void publicarPrecio(GestionItem g, String proveedorRazonSocial, Long usuarioId) {
        eventos.publishEvent(new PrecioTotalActualizado(UUID.randomUUID(), Instant.now(), g.getItemId(),
                g.getSolicitudId(), g.getPrecioTotal(), g.getMonedaTotal(), g.getCotizacionElegidaId(),
                proveedorRazonSocial, usuarioId));
    }

    void publicarResuelto(GestionItem g, String resultado, String proveedorRazonSocial, String rechazadoPor) {
        eventos.publishEvent(new ItemResuelto(UUID.randomUUID(), Instant.now(), g.getItemId(), g.getSolicitudId(),
                resultado, proveedorRazonSocial, g.getMotivoRechazo(), rechazadoPor, g.getUrgencia(),
                g.getSolicitadoEn().toInstant(), g.getResueltoEn().toInstant(), g.getEncargadoId()));
    }
}
