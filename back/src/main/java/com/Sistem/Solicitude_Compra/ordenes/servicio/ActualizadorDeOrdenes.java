package com.Sistem.Solicitude_Compra.ordenes.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

/** Mantiene los montos de la orden al día cuando el Encargado edita la compra. */
@Service
public class ActualizadorDeOrdenes {

    private final OrdenPedidoRepository ordenes;

    public ActualizadorDeOrdenes(OrdenPedidoRepository ordenes) {
        this.ordenes = ordenes;
    }

    @ApplicationModuleListener
    void alActualizarseLaCompra(CompraActualizada compra) {
        // Si el evento llegó antes que CompraRegistrada, la excepción lo deja pendiente para reintentarlo.
        OrdenPedido orden = ordenes.findByItemId(compra.itemId())
                .orElseThrow(() -> new IllegalStateException(
                        "Todavía no existe la orden del ítem " + compra.itemId()));
        // Se actualiza aunque ya esté ENVIADA: el Encargado corrige lo que se pagó de verdad.
        orden.actualizarMontos(compra.precioTotal(), compra.montoEnvio());
    }
}
