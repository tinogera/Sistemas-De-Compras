package com.Sistem.Solicitude_Compra.ordenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;

class OrdenServiceTest {

    OrdenPedidoRepository ordenes = mock(OrdenPedidoRepository.class);
    ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);
    OrdenService service = new OrdenService(ordenes, eventos);

    @Test
    void ordenInexistenteDa404() {
        when(ordenes.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NoEncontradoException.class, () -> service.marcarEnviada(1L, 5L));
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void siCambiaLaFilaPublicaOrdenEnviada() {
        OrdenPedido orden = orden();
        when(ordenes.findById(1L)).thenReturn(Optional.of(orden));
        when(ordenes.marcarEnviada(eq(1L), eq(5L), any())).thenReturn(1);

        service.marcarEnviada(1L, 5L);

        ArgumentCaptor<OrdenEnviada> publicado = ArgumentCaptor.forClass(OrdenEnviada.class);
        verify(eventos).publishEvent(publicado.capture());
        assertEquals(10L, publicado.getValue().itemId());
        assertEquals(20L, publicado.getValue().solicitudId());
        assertEquals(5L, publicado.getValue().usuarioId());
    }

    @Test
    void siYaEstabaEnviadaDa422YNoPublica() {
        when(ordenes.findById(1L)).thenReturn(Optional.of(orden()));
        when(ordenes.marcarEnviada(eq(1L), eq(5L), any())).thenReturn(0);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> service.marcarEnviada(1L, 5L));

        assertEquals("ORDEN_YA_ENVIADA", error.getCodigo());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    private static OrdenPedido orden() {
        return new OrdenPedido(1L, 10L, 20L, 30L, "Papelera SA", null, TipoItem.PRODUCTO,
                "Lapiceras", new BigDecimal("20"), Moneda.ARS, new BigDecimal("1000.00"));
    }
}
