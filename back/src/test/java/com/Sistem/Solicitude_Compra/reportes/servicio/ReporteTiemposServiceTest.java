package com.Sistem.Solicitude_Compra.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.TiempoPorUrgencia;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReporteTiemposServiceTest {

    static final LocalDate DESDE = LocalDate.of(2026, 11, 1);
    static final LocalDate HASTA = LocalDate.of(2026, 11, 30);
    // Hora de Argentina (UTC-3): desde las 00:00 del 1/11 hasta las 00:00 del 1/12.
    static final Instant INICIO = Instant.parse("2026-11-01T03:00:00Z");
    static final Instant FIN = Instant.parse("2026-12-01T03:00:00Z");

    HechoResolucionRepository resoluciones = mock(HechoResolucionRepository.class);
    ReporteTiemposService service = new ReporteTiemposService(resoluciones);

    /** Fila hecha a mano: crear mocks adentro de un when(...) anidado rompe el stubbing de Mockito. */
    private record Fila(String urgencia, long cantidad, Double promedioHoras) implements TiempoPorUrgencia {
        @Override public String getUrgencia() { return urgencia; }
        @Override public long getCantidad() { return cantidad; }
        @Override public Double getPromedioHoras() { return promedioHoras; }
    }

    @Test
    void altaConDosItemsDe10Y20HorasPromedia15YMediaSinItemsQuedaEnCeroConPromedioNull() {
        when(resoluciones.tiempoPorUrgencia(INICIO, FIN)).thenReturn(List.of(new Fila("ALTA", 2, 15.0)));

        TiemposResolucion t = service.tiempos(DESDE, HASTA);

        assertThat(t.porUrgencia().get(Urgencia.ALTA).cantidad()).isEqualTo(2);
        assertThat(t.porUrgencia().get(Urgencia.ALTA).promedioHoras()).isEqualTo(15.0);
        assertThat(t.porUrgencia().get(Urgencia.MEDIA).cantidad()).isZero();
        assertThat(t.porUrgencia().get(Urgencia.MEDIA).promedioHoras()).isNull();
        assertThat(t.porUrgencia().get(Urgencia.BAJA).promedioHoras()).isNull();
    }

    @Test
    void elPromedioGeneralPonderaPorCantidad() {
        // (2 * 15 + 1 * 60) / 3 = 30
        when(resoluciones.tiempoPorUrgencia(INICIO, FIN)).thenReturn(List.of(
                new Fila("ALTA", 2, 15.0), new Fila("BAJA", 1, 60.0)));

        TiemposResolucion t = service.tiempos(DESDE, HASTA);

        assertThat(t.cantidadItems()).isEqualTo(3);
        assertThat(t.promedioHoras()).isEqualTo(30.0);
    }

    @Test
    void redondeaAUnDecimal() {
        when(resoluciones.tiempoPorUrgencia(INICIO, FIN)).thenReturn(List.of(new Fila("MEDIA", 3, 10.0 / 3)));

        TiemposResolucion t = service.tiempos(DESDE, HASTA);

        assertThat(t.porUrgencia().get(Urgencia.MEDIA).promedioHoras()).isEqualTo(3.3);
        assertThat(t.promedioHoras()).isEqualTo(3.3);
    }

    @Test
    void sinItemsElGeneralTambienEsNullYLasTresUrgenciasSalenEnOrden() {
        when(resoluciones.tiempoPorUrgencia(INICIO, FIN)).thenReturn(List.of());

        TiemposResolucion t = service.tiempos(DESDE, HASTA);

        assertThat(t.cantidadItems()).isZero();
        assertThat(t.promedioHoras()).isNull();
        assertThat(t.porUrgencia().keySet()).containsExactly(Urgencia.ALTA, Urgencia.MEDIA, Urgencia.BAJA);
    }

    @Test
    void elRangoSonDiasCompletosEnHoraDeArgentina() {
        when(resoluciones.tiempoPorUrgencia(Instant.parse("2026-11-10T03:00:00Z"),
                Instant.parse("2026-11-11T03:00:00Z"))).thenReturn(List.of(new Fila("ALTA", 1, 5.0)));

        // desde == hasta es un solo día.
        TiemposResolucion t = service.tiempos(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 10));

        assertThat(t.cantidadItems()).isEqualTo(1);
    }

    @Test
    void desdePosteriorAHastaOFaltarFechasEsUnParametroInvalido() {
        assertThatThrownBy(() -> service.tiempos(HASTA, DESDE)).isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.tiempos(null, HASTA)).isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> service.tiempos(DESDE, null)).isInstanceOf(ParametroInvalidoException.class);
        verifyNoInteractions(resoluciones);
    }
}
