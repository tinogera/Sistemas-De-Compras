package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.CancelacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCancelada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Toda solicitud entra a la bandeja de compras apenas se crea, sin esperar aprobaciones:
 * la cadena de aprobación corre en paralelo (decisión 7).
 */
@Component
public class ComprasEventListener {

    private final GestionItemRepository gestiones;
    private final ApplicationEventPublisher eventos;

    public ComprasEventListener(GestionItemRepository gestiones, ApplicationEventPublisher eventos) {
        this.gestiones = gestiones;
        this.eventos = eventos;
    }

    // @ApplicationModuleListener ya es @Async + @Transactional(REQUIRES_NEW) + @TransactionalEventListener.
    @ApplicationModuleListener
    void alCrearseLaSolicitud(SolicitudCreada evento) {
        OffsetDateTime solicitadoEn = evento.ocurridoEn().atOffset(ZoneOffset.UTC);

        for (SolicitudCreada.ItemCreado item : evento.items()) {
            // Idempotente: el evento puede llegar más de una vez.
            if (gestiones.existsByItemId(item.itemId())) {
                continue;
            }
            // El constructor deja estado = PENDIENTE, estadoAprobacion = EN_CURSO
            // y deriva la prioridad de la urgencia.
            gestiones.save(new GestionItem(
                    item.itemId(), evento.solicitudId(), evento.sectorId(), item.categoriaId(),
                    item.tipo(), item.detalle(), item.cantidad(), evento.urgencia(),
                    evento.fechaNecesaria(), solicitadoEn,
                    item.precioEstimado(), item.monedaEstimada()));
        }
    }

    /** La edición solo se copia si la gestión sigue PENDIENTE; si ya la tomaron, se ignora. */
    @ApplicationModuleListener
    void alEditarseUnItem(ItemEditado evento) {
        gestiones.findById(evento.itemId()).ifPresent(gestion -> {
            if (gestion.actualizarDatos(evento.categoriaId(), evento.cantidad(), evento.detalle(),
                    evento.precioEstimado(), evento.monedaEstimada())) {
                gestiones.save(gestion);
            }
        });
    }

    @ApplicationModuleListener
    void alCancelarseUnItem(ItemCancelado evento) {
        cancelarGestion(evento.itemId(), evento.solicitudId());
    }

    @ApplicationModuleListener
    void alCancelarseLaSolicitud(SolicitudCancelada evento) {
        evento.itemIds().forEach(itemId -> cancelarGestion(itemId, evento.solicitudId()));
    }

    /**
     * PENDIENTE pasa a CANCELADO. Si un Encargado ya la había tomado (la carrera entre la cancelación y
     * este evento), la gestión no se toca y se avisa a Solicitudes con CancelacionRechazada para que
     * el ítem vuelva a EN_COTIZACION. Si ya estaba CANCELADA, el evento es un duplicado y no hace nada.
     */
    private void cancelarGestion(Long itemId, Long solicitudId) {
        gestiones.findById(itemId).ifPresent(gestion -> {
            if (gestion.getEstado() == EstadoGestion.CANCELADO) {
                return;
            }
            if (gestion.estaPendiente()) {
                gestion.cancelar(OffsetDateTime.now(ZoneOffset.UTC));
                gestiones.save(gestion);
            } else {
                eventos.publishEvent(new CancelacionRechazada(UUID.randomUUID(), Instant.now(), itemId,
                        solicitudId));
            }
        });
    }
}
