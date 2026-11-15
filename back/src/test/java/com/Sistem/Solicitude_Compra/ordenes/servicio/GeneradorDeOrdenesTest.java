package com.Sistem.Solicitude_Compra.ordenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;

class GeneradorDeOrdenesTest {

    OrdenPedidoRepository ordenes = mock(OrdenPedidoRepository.class);
    ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    GeneradorDeOrdenes generador = new GeneradorDeOrdenes(ordenes, eventos);

    @Test
    void conUnaCompraNuevaGuardaLaOrdenYPublicaOrdenGenerada() {
        when(ordenes.existsByItemId(10L)).thenReturn(false);
        when(ordenes.siguienteNumero()).thenReturn(7L);
        when(ordenes.save(any(OrdenPedido.class))).thenAnswer(i -> i.getArgument(0));

        generador.alRegistrarseLaCompra(compra());

        ArgumentCaptor<OrdenPedido> guardada = ArgumentCaptor.forClass(OrdenPedido.class);
        verify(ordenes).save(guardada.capture());
        assertEquals(7L, guardada.getValue().getNumero());
        assertEquals(10L, guardada.getValue().getItemId());
        assertEquals(0, new BigDecimal("50.00").compareTo(guardada.getValue().getPrecioUnitario()));

        ArgumentCaptor<OrdenGenerada> publicado = ArgumentCaptor.forClass(OrdenGenerada.class);
        verify(eventos).publishEvent(publicado.capture());
        assertEquals(7L, publicado.getValue().numero());
        assertEquals(40L, publicado.getValue().sectorId());
        assertEquals(50L, publicado.getValue().categoriaId());
    }

    @Test
    void siYaExisteLaOrdenDelItemNoHaceNada() {
        when(ordenes.existsByItemId(10L)).thenReturn(true);

        generador.alRegistrarseLaCompra(compra());

        verify(ordenes, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    private static CompraRegistrada compra() {
        return new CompraRegistrada(UUID.randomUUID(), Instant.now(), 10L, 20L, 40L, 50L,
                TipoItem.PRODUCTO, "Lapiceras", new BigDecimal("20"), 30L, "Papelera SA", null,
                Moneda.ARS, new BigDecimal("1000.00"), 99L);
    }
}
