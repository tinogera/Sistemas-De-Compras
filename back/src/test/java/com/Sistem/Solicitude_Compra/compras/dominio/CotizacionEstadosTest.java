package com.Sistem.Solicitude_Compra.compras.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CotizacionEstadosTest {

    @Test
    void pasaPorCandidataSeleccionadaYDescartada() {
        Cotizacion c = new Cotizacion(1L, 2L, "Globos SA", Moneda.ARS, new BigDecimal("1.00"),
                new BigDecimal("2.00"), LocalDate.now(), null, null, 7L, OffsetDateTime.now());
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        c.seleccionar();
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.SELECCIONADA);
        c.volverACandidata();
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        c.descartar();
        assertThat(c.getEstado()).isEqualTo(EstadoCotizacion.DESCARTADA);
    }
}
