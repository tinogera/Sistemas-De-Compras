package com.Sistem.Solicitude_Compra.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.TiempoPorUrgencia;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReporteTiemposServiceTest {

    @Mock HechoResolucionRepository repo;

    private static TiempoPorUrgencia fila(String urgencia, long cantidad, Double promedio) {
        return new TiempoPorUrgencia() {
            public String getUrgencia() { return urgencia; }
            public long getCantidad() { return cantidad; }
            public Double getPromedioHoras() { return promedio; }
        };
    }

    @Test
    void ponderaElPromedioGeneralPorCantidadYDejaNullLasUrgenciasSinItems() {
        when(repo.tiemposPorUrgencia(any(), any())).thenReturn(List.of(fila("ALTA", 2, 15.0), fila("BAJA", 1, 30.04)));

        var r = new ReporteTiemposService(repo).tiempos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(r.cantidadItems()).isEqualTo(3);
        assertThat(r.promedioHoras()).isEqualTo(20.0); // (15*2 + 30.04) / 3 = 20.01
        assertThat(r.porUrgencia().get("ALTA").promedioHoras()).isEqualTo(15.0);
        assertThat(r.porUrgencia().get("BAJA").promedioHoras()).isEqualTo(30.0);
        assertThat(r.porUrgencia().get("MEDIA").cantidad()).isZero();
        assertThat(r.porUrgencia().get("MEDIA").promedioHoras()).isNull();
    }

    @Test
    void sinResueltosElPromedioEsNullYNoCero() {
        when(repo.tiemposPorUrgencia(any(), any())).thenReturn(List.of());

        var r = new ReporteTiemposService(repo).tiempos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertThat(r.cantidadItems()).isZero();
        assertThat(r.promedioHoras()).isNull();
    }

    @Test
    void elRangoSonDiasCompletosEnHoraArgentina() {
        when(repo.tiemposPorUrgencia(any(), any())).thenReturn(List.of());

        new ReporteTiemposService(repo).tiempos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        // Argentina es UTC-3: las 00:00 del 1/11 son las 03:00Z, y las 00:00 del 1/12 son las 03:00Z del 1/12.
        verify(repo).tiemposPorUrgencia(eq(Instant.parse("2026-11-01T03:00:00Z")),
                eq(Instant.parse("2026-12-01T03:00:00Z")));
    }

    @Test
    void validaLasFechas() {
        var servicio = new ReporteTiemposService(repo);

        assertThatThrownBy(() -> servicio.tiempos(null, LocalDate.of(2026, 11, 30)))
                .isInstanceOf(ParametroInvalidoException.class);
        assertThatThrownBy(() -> servicio.tiempos(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 30)))
                .isInstanceOf(ParametroInvalidoException.class);
    }
}
