package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import java.math.BigDecimal;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Mantiene el ítem de Solicitudes al día con lo que pasa en Compras, Aprobaciones y Órdenes, para
 * mostrarlo sin consultar a esos módulos. Todos los listeners son idempotentes: el mismo evento
 * puede llegar más de una vez.
 */
@Component
public class ItemEstadoListener {

    private static final Logger log = LoggerFactory.getLogger(ItemEstadoListener.class);

    private final ItemRepository items;
    private final SolicitudService solicitudes;

    public ItemEstadoListener(ItemRepository items, SolicitudService solicitudes) {
        this.items = items;
        this.solicitudes = solicitudes;
    }

    // @ApplicationModuleListener ya es @Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener.

    /** Solo un ítem PENDIENTE pasa a EN_COTIZACION: no se pisa un ítem ya cancelado o resuelto. */
    @ApplicationModuleListener
    void alAsignarseElItem(ItemAsignado evento) {
        actualizar(evento.itemId(), item -> {
            if (item.getEstado() == EstadoItem.PENDIENTE) {
                item.setEstado(EstadoItem.EN_COTIZACION);
            }
        });
    }

    /** El precio total y su moneda pueden venir en null: el Encargado quitó la cotización elegida. */
    @ApplicationModuleListener
    void alActualizarseElPrecioTotal(PrecioTotalActualizado evento) {
        guardarPrecioTotal(evento.itemId(), evento.precioTotal(), evento.moneda());
    }

    @ApplicationModuleListener
    void alActualizarseLaCompra(CompraActualizada evento) {
        guardarPrecioTotal(evento.itemId(), evento.precioTotal(), evento.moneda());
    }

    /** Solo cambia estadoAprobacion, nunca el estado: "listo para comprar" es EN_COTIZACION + APROBADA. */
    @ApplicationModuleListener
    void alCompletarseLaAprobacion(AprobacionCompletada evento) {
        actualizar(evento.itemId(), item -> item.setEstadoAprobacion(EstadoAprobacion.APROBADA));
    }

    @ApplicationModuleListener
    void alReabrirseLaAprobacion(AprobacionReabierta evento) {
        actualizar(evento.itemId(), item -> item.setEstadoAprobacion(EstadoAprobacion.EN_CURSO));
    }

    @ApplicationModuleListener
    void alResolverseElItem(ItemResuelto evento) {
        actualizar(evento.itemId(), item -> {
            if (item.getEstado().esFinal()) {
                return; // ya resuelto o cancelado: evento duplicado o tardío
            }
            switch (String.valueOf(evento.resultado())) {
                case "COMPRADO" -> {
                    item.setEstado(EstadoItem.COMPRADO);
                    item.setProveedorComprado(evento.proveedorRazonSocial());
                }
                case "RECHAZADO" -> {
                    item.setEstado(EstadoItem.RECHAZADO);
                    item.setMotivoRechazo(evento.motivoRechazo());
                    item.setRechazadoPor(evento.rechazadoPor());
                }
                default -> {
                    log.warn("ItemResuelto {} con resultado desconocido: {}", evento.eventoId(),
                            evento.resultado());
                    return;
                }
            }
            recalcularEstadoDeLaSolicitud(item.getSolicitud());
        });
    }

    @ApplicationModuleListener
    void alEnviarseLaOrden(OrdenEnviada evento) {
        actualizar(evento.itemId(), item -> {
            item.setOrdenEnviada(true);
            recalcularEstadoDeLaSolicitud(item.getSolicitud());
        });
    }

    private void guardarPrecioTotal(Long itemId, BigDecimal precioTotal, Moneda moneda) {
        actualizar(itemId, item -> {
            item.setPrecioTotal(precioTotal);
            item.setMonedaTotal(precioTotal == null ? null : moneda);
        });
    }

    private void actualizar(Long itemId, Consumer<Item> cambio) {
        items.findById(itemId).ifPresentOrElse(item -> {
            cambio.accept(item);
            items.save(item);
        }, () -> log.warn("Llegó un evento para el ítem {}, que no existe en Solicitudes", itemId));
    }

    private void recalcularEstadoDeLaSolicitud(Solicitud solicitud) {
        solicitudes.recalcularEstado(solicitud.getId());
    }
}
