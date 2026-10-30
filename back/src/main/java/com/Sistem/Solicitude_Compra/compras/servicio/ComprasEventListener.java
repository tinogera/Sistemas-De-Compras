package com.Sistem.Solicitude_Compra.compras.servicio;

import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Toda solicitud entra a la bandeja de compras apenas se crea, sin esperar aprobaciones:
 * la cadena de aprobación corre en paralelo (decisión 7).
 */
@Component
public class ComprasEventListener {

    private final GestionItemRepository gestiones;

    public ComprasEventListener(GestionItemRepository gestiones) {
        this.gestiones = gestiones;
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
}