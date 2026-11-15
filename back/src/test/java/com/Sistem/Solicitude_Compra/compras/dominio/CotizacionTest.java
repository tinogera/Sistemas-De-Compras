package com.Sistem.Solicitude_Compra.compras.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class CotizacionTest {

    private static Cotizacion conValidez(LocalDate fechaValidez) {
        return new Cotizacion(1L, 2L, "Globos SA", Moneda.ARS, new BigDecimal("10.00"), new BigDecimal("30.00"),
                fechaValidez, null, null, 7L, OffsetDateTime.now());
    }

    @Test
    void nuevaCotizacionEsCandidata() {
        assertThat(conValidez(LocalDate.now()).getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
    }

    @Test
    void vigenteSiLaFechaDeValidezEsHoyOPosterior() {
        LocalDate hoy = LocalDate.of(2026, 11, 7);
        assertThat(conValidez(hoy).estaVigente(hoy)).isTrue();
        assertThat(conValidez(hoy.plusDays(1)).estaVigente(hoy)).isTrue();
    }

    @Test
    void vencidaSiLaFechaDeValidezEsAnteriorAHoy() {
        LocalDate hoy = LocalDate.of(2026, 11, 7);
        assertThat(conValidez(hoy.minusDays(1)).estaVigente(hoy)).isFalse();
    }
}
