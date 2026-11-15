package com.Sistem.Solicitude_Compra.ordenes.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;

class ActualizadorDeOrdenesTest {

    OrdenPedidoRepository ordenes = mock(OrdenPedidoRepository.class);
    ActualizadorDeOrdenes actualizador = new ActualizadorDeOrdenes(ordenes);

    @Test
    void actualizaLosMontosYRecalculaElPrecioUnitario() {
        OrdenPedido orden = orden();
        when(ordenes.findByItemId(10L)).thenReturn(Optional.of(orden));

        actualizador.alActualizarseLaCompra(evento("2000.00", "5000.00"));

        assertEquals(0, new BigDecimal("2000.00").compareTo(orden.getPrecioTotal()));
        assertEquals(0, new BigDecimal("100.00").compareTo(orden.getPrecioUnitario()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(orden.getMontoEnvio()));
    }

    @Test
    void aplicarElMismoEventoDosVecesDejaLaOrdenIgual() {
        OrdenPedido orden = orden();
        when(ordenes.findByItemId(10L)).thenReturn(Optional.of(orden));

        actualizador.alActualizarseLaCompra(evento("2000.00", "5000.00"));
        actualizador.alActualizarseLaCompra(evento("2000.00", "5000.00"));

        assertEquals(0, new BigDecimal("2000.00").compareTo(orden.getPrecioTotal()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(orden.getMontoEnvio()));
    }

    @Test
    void actualizaTambienUnaOrdenYaEnviada() {
        OrdenPedido orden = orden();
        ReflectionTestUtils.setField(orden, "estado", EstadoOrden.ENVIADA);
        when(ordenes.findByItemId(10L)).thenReturn(Optional.of(orden));

        actualizador.alActualizarseLaCompra(evento("1500.00", null));

        assertEquals(0, new BigDecimal("1500.00").compareTo(orden.getPrecioTotal()));
        assertEquals(EstadoOrden.ENVIADA, orden.getEstado());
    }

    @Test
    void sinOrdenLanzaUnaExcepcionParaQueElEventoSeReintente() {
        when(ordenes.findByItemId(10L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> actualizador.alActualizarseLaCompra(evento("2000.00", null)));
    }

    private static OrdenPedido orden() {
        return new OrdenPedido(1L, 10L, 20L, 30L, "Papelera SA", null, TipoItem.PRODUCTO,
                "Lapiceras", new BigDecimal("20"), Moneda.ARS, new BigDecimal("1000.00"));
    }

    private static CompraActualizada evento(String precioTotal, String montoEnvio) {
        return new CompraActualizada(UUID.randomUUID(), Instant.now(), 10L, 20L,
                new BigDecimal("1000.00"), new BigDecimal(precioTotal),
                null, montoEnvio == null ? null : new BigDecimal(montoEnvio),
                Moneda.ARS, 99L);
    }
}
