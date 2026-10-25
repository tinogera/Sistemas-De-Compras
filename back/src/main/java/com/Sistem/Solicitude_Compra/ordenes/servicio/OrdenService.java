package com.Sistem.Solicitude_Compra.ordenes.servicio;

import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrdenService {

    private final OrdenPedidoRepository ordenes;
    private final ApplicationEventPublisher eventos;

    public OrdenService(OrdenPedidoRepository ordenes, ApplicationEventPublisher eventos) {
        this.ordenes = ordenes;
        this.eventos = eventos;
    }

    @Transactional
    public OrdenPedido marcarEnviada(Long id, Long usuarioId) {
        OrdenPedido orden = ordenes.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la orden " + id));

        // Update condicional: si dos requests llegan a la vez, solo uno cambia la fila y publica el evento.
        Instant ahora = Instant.now();
        if (ordenes.marcarEnviada(id, usuarioId, ahora) == 0) {
            throw new ReglaNegocioException("ORDEN_YA_ENVIADA", "La orden ya fue enviada");
        }
        eventos.publishEvent(new OrdenEnviada(UUID.randomUUID(), ahora,
                orden.getId(), orden.getItemId(), orden.getSolicitudId(), usuarioId));
        return ordenes.findById(id).orElseThrow();
    }
}
