package com.Sistem.Solicitude_Compra.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoGastoRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AlimentadorDeReportesTest {

    HechoGastoRepository gastos = mock(HechoGastoRepository.class);
    HechoResolucionRepository resoluciones = mock(HechoResolucionRepository.class);
    AlimentadorDeReportes alimentador = new AlimentadorDeReportes(gastos, resoluciones);

    private static OrdenGenerada orden(Instant ocurridoEn) {
        return new OrdenGenerada(UUID.randomUUID(), ocurridoEn, 1L, 7L, 10L, 20L, 30L, 40L, 50L, Moneda.ARS,
                new BigDecimal("1000.00"));
    }

    @Test
    void elDiaDeEmisionEsElDiaEnArgentinaYNoElDiaUtc() {
        // 01:30 UTC del 21/11 son las 22:30 del 20/11 en Buenos Aires.
        alimentador.alGenerarseLaOrden(orden(Instant.parse("2026-11-21T01:30:00Z")));

        ArgumentCaptor<HechoGasto> guardado = ArgumentCaptor.forClass(HechoGasto.class);
        verify(gastos).save(guardado.capture());
        assertThat(guardado.getValue().getFecha().toString()).isEqualTo("2026-11-20");
    }

    @Test
    void siYaExisteElGastoDeLaOrdenNoInsertaOtro() {
        when(gastos.existsById(1L)).thenReturn(true);

        alimentador.alGenerarseLaOrden(orden(Instant.now()));

        verify(gastos, never()).save(any());
    }

    @Test
    void conUnaCompraActualizadaSinHechoLanzaUnaExcepcionParaQueSeReintente() {
        when(gastos.findByItemId(10L)).thenReturn(Optional.empty());
        var compra = new CompraActualizada(UUID.randomUUID(), Instant.now(), 10L, 20L, new BigDecimal("1000.00"),
                new BigDecimal("2000.00"), null, new BigDecimal("50.00"), Moneda.ARS, 9L);

        assertThatThrownBy(() -> alimentador.alActualizarseLaCompra(compra))
                .isInstanceOf(IllegalStateException.class);
    }
}
