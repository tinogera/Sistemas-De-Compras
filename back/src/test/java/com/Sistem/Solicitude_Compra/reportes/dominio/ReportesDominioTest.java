package com.Sistem.Solicitude_Compra.reportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReportesDominioTest {

    private static HechoGasto gasto() {
        return new HechoGasto(1L, 10L, LocalDate.of(2026, 11, 20), 2L, 3L, 4L, Moneda.ARS, new BigDecimal("1000.00"));
    }

    @Test
    void elGastoNuevoTieneEnvioCero() {
        assertThat(gasto().getMontoEnvio()).isEqualByComparingTo("0");
        assertThat(gasto().getId()).isEqualTo(1L);
    }

    @Test
    void actualizarMontosCambiaImporteYEnvio() {
        HechoGasto g = gasto();

        g.actualizarMontos(new BigDecimal("1500.00"), new BigDecimal("200.00"));

        assertThat(g.getImporte()).isEqualByComparingTo("1500.00");
        assertThat(g.getMontoEnvio()).isEqualByComparingTo("200.00");
    }

    @Test
    void unEnvioNuloCuentaComoCero() {
        HechoGasto g = gasto();
        g.actualizarMontos(new BigDecimal("1000.00"), new BigDecimal("200.00"));

        g.actualizarMontos(new BigDecimal("1000.00"), null);

        assertThat(g.getMontoEnvio()).isEqualByComparingTo("0");
    }

    @Test
    void rechazaImportesYEnviosNegativos() {
        HechoGasto g = gasto();

        assertThatThrownBy(() -> g.actualizarMontos(new BigDecimal("-1"), null))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> g.actualizarMontos(new BigDecimal("1"), new BigDecimal("-1")))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void elGastoExigeTodosLosDatos() {
        assertThatThrownBy(() -> new HechoGasto(1L, 10L, null, 2L, 3L, 4L, Moneda.ARS, BigDecimal.ONE))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void laResolucionNoPuedeSerAnteriorALaCreacion() {
        Instant creado = Instant.parse("2026-11-10T10:00:00Z");

        assertThatThrownBy(() -> new HechoResolucion(1L, Urgencia.ALTA, creado, creado.minusSeconds(1),
                ResultadoResolucion.COMPRADO)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    void laResolucionGuardaSusDatos() {
        Instant creado = Instant.parse("2026-11-10T10:00:00Z");

        HechoResolucion r = new HechoResolucion(5L, Urgencia.BAJA, creado, creado.plusSeconds(3600),
                ResultadoResolucion.RECHAZADO);

        assertThat(r.getId()).isEqualTo(5L);
        assertThat(r.getResultado()).isEqualTo(ResultadoResolucion.RECHAZADO);
        assertThat(r.isNew()).isTrue();
    }
}
