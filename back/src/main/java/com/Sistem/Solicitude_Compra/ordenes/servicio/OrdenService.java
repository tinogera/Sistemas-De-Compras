package com.Sistem.Solicitude_Compra.ordenes.servicio;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import com.Sistem.Solicitude_Compra.ordenes.web.OrdenDetalleResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class OrdenService {

    private static final Sort MAS_NUEVAS_PRIMERO = Sort.by(Sort.Direction.DESC, "emitidaEn");

    private final OrdenPedidoRepository ordenes;
    private final ApplicationEventPublisher eventos;
    private final AuthApi authApi;

    public OrdenService(OrdenPedidoRepository ordenes, ApplicationEventPublisher eventos, AuthApi authApi) {
        this.ordenes = ordenes;
        this.eventos = eventos;
        this.authApi = authApi;
    }

    /** Sin estado trae todas. El orden es siempre el más nuevo primero, el que pida el cliente se ignora. */
    @Transactional(readOnly = true)
    public Page<OrdenPedido> listar(EstadoOrden estado, Pageable pageable) {
        Pageable pagina = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), MAS_NUEVAS_PRIMERO);
        return estado == null ? ordenes.findAll(pagina) : ordenes.findByEstado(estado, pagina);
    }

    @Transactional(readOnly = true)
    public OrdenPedido buscar(Long id) {
        return ordenes.findById(id)
                .orElseThrow(() -> new NoEncontradoException("No existe la orden " + id));
    }

    @Transactional(readOnly = true)
    public OrdenDetalleResponse obtener(Long id) {
        OrdenPedido orden = buscar(id);
        String nombre = orden.getEnviadaPor() == null
                ? null
                : authApi.obtenerUsuario(orden.getEnviadaPor()).nombre();
        return OrdenDetalleResponse.de(orden, nombre);
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
