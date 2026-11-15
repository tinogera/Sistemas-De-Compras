package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Lo que Aprobaciones le avisa a Compras sobre la cadena de un ítem. Todos son idempotentes: el evento puede
 * llegar más de una vez, o después de que el ítem ya se resolvió.
 */
@Component
public class AprobacionesEventListener {

    private final GestionItemRepository gestiones;
    private final ApplicationEventPublisher eventos;

    public AprobacionesEventListener(GestionItemRepository gestiones, ApplicationEventPublisher eventos) {
        this.gestiones = gestiones;
        this.eventos = eventos;
    }

    /** Habilita la compra: el ítem aparece en "Para comprar". */
    @ApplicationModuleListener
    void alCompletarseLaAprobacion(AprobacionCompletada evento) {
        gestiones.findById(evento.itemId()).ifPresent(gestion -> {
            if (gestion.marcarAprobada()) {
                gestiones.save(gestion);
            }
        });
    }

    /** El precio cambió y la cadena vuelve a correr: se bloquea la compra hasta que se complete otra vez. */
    @ApplicationModuleListener
    void alReabrirseLaAprobacion(AprobacionReabierta evento) {
        gestiones.findById(evento.itemId()).ifPresent(gestion -> {
            if (gestion.reabrirAprobacion()) {
                gestiones.save(gestion);
            }
        });
    }

    /**
     * Un aprobador rechazó el ítem: queda RECHAZADO con su motivo, aunque lo tenga asignado un Encargado.
     * Si ya estaba resuelto (rechazado, comprado o cancelado) gana lo primero que pasó y no se hace nada.
     */
    @ApplicationModuleListener
    void alRechazarseLaAprobacion(AprobacionRechazada evento) {
        gestiones.findById(evento.itemId()).ifPresent(gestion -> {
            if (gestion.estaResuelta()) {
                return;
            }
            gestion.rechazar(evento.motivo(), OffsetDateTime.now(ZoneOffset.UTC));
            gestiones.save(gestion);
            eventos.publishEvent(new ItemResuelto(UUID.randomUUID(), Instant.now(), gestion.getItemId(),
                    gestion.getSolicitudId(), "RECHAZADO", null, gestion.getMotivoRechazo(),
                    evento.aprobadorNombre(), gestion.getUrgencia(), gestion.getSolicitadoEn().toInstant(),
                    gestion.getResueltoEn().toInstant(), gestion.getEncargadoId()));
        });
    }
}
