package com.Sistem.Solicitude_Compra.ordenes.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/** Genera una orden de pedido por cada ítem comprado. */
@Service
public class GeneradorDeOrdenes {

    private final OrdenPedidoRepository ordenes;
    private final ApplicationEventPublisher eventos;

    public GeneradorDeOrdenes(OrdenPedidoRepository ordenes, ApplicationEventPublisher eventos) {
        this.ordenes = ordenes;
        this.eventos = eventos;
    }

    @ApplicationModuleListener
    void alRegistrarseLaCompra(CompraRegistrada compra) {
        // Idempotente: el evento puede llegar más de una vez.
        if (ordenes.existsByItemId(compra.itemId())) {
            return;
        }
        OrdenPedido orden = ordenes.save(new OrdenPedido(
                ordenes.siguienteNumero(), compra.itemId(), compra.solicitudId(),
                compra.proveedorId(), compra.proveedorRazonSocial(), compra.cotizacionId(),
                compra.tipo(), compra.detalle(), compra.cantidad(), compra.moneda(),
                compra.precioTotal()));
        eventos.publishEvent(new OrdenGenerada(UUID.randomUUID(), Instant.now(),
                orden.getId(), orden.getNumero(), compra.itemId(), compra.solicitudId(),
                compra.sectorId(), compra.categoriaId(), compra.proveedorId(),
                compra.moneda(), compra.precioTotal()));
    }
}
