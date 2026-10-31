package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.CancelacionRechazada;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Compras avisa que no pudo cancelar un ítem porque un Encargado ya lo había tomado: se deshace la
 * cancelación que el solicitante vio como exitosa.
 */
@Component
public class SolicitudEventListener {

    private final ItemRepository items;
    private final SolicitudRepository solicitudes;

    public SolicitudEventListener(ItemRepository items, SolicitudRepository solicitudes) {
        this.items = items;
        this.solicitudes = solicitudes;
    }

    // @ApplicationModuleListener ya es @Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener.
    @ApplicationModuleListener
    void alRechazarseLaCancelacion(CancelacionRechazada evento) {
        items.findById(evento.itemId()).ifPresent(item -> {
            // Idempotente: solo se revierte un ítem que sigue CANCELADO.
            if (item.getEstado() != EstadoItem.CANCELADO) {
                return;
            }
            item.setEstado(EstadoItem.EN_COTIZACION);
            items.save(item);

            Solicitud solicitud = item.getSolicitud();
            if (solicitud.getEstado() == EstadoSolicitud.CANCELADA) {
                solicitud.setEstado(EstadoSolicitud.EN_REVISION);
                solicitudes.save(solicitud);
            }
        });
    }
}
