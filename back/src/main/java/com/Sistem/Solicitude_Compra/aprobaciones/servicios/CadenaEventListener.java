package com.Sistem.Solicitude_Compra.aprobaciones.servicios;

import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCancelada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import java.util.List;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Lo que el resto del sistema le avisa a Aprobaciones. Todos los listeners son idempotentes. */
@Component
public class CadenaEventListener {

    private final CadenaAprobacionService cadena;

    public CadenaEventListener(CadenaAprobacionService cadena) {
        this.cadena = cadena;
    }

    @ApplicationModuleListener
    void alCrearseLaSolicitud(SolicitudCreada evento) {
        cadena.iniciar(evento);
    }

    @ApplicationModuleListener
    void alActualizarseElPrecioTotal(PrecioTotalActualizado evento) {
        cadena.precioActualizado(evento);
    }

    @ApplicationModuleListener
    void alEditarseUnItem(ItemEditado evento) {
        cadena.itemEditado(evento);
    }

    @ApplicationModuleListener
    void alCancelarseUnItem(ItemCancelado evento) {
        cadena.cancelar(List.of(evento.itemId()));
    }

    @ApplicationModuleListener
    void alCancelarseLaSolicitud(SolicitudCancelada evento) {
        cadena.cancelar(evento.itemIds());
    }

    @ApplicationModuleListener
    void alResolverseUnItem(ItemResuelto evento) {
        cadena.resuelto(evento);
    }
}
