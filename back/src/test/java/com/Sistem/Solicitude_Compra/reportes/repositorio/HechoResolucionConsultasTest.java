package com.Sistem.Solicitude_Compra.reportes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import com.Sistem.Solicitude_Compra.reportes.dominio.ResultadoResolucion;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class HechoResolucionConsultasTest {

    private static final Instant DESDE = Instant.parse("2026-11-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-01T03:00:00Z");

    @Autowired HechoResolucionRepository resoluciones;

    private long siguienteItem = 1;

    @BeforeEach
    void limpiar() {
        resoluciones.deleteAll();
    }

    /** Un ítem que tardó {@code horas} en resolverse, resuelto en {@code resueltoEn}. */
    private void resuelto(Urgencia urgencia, Instant resueltoEn, long horas, ResultadoResolucion resultado) {
        resoluciones.saveAndFlush(new HechoResolucion(siguienteItem++, urgencia, resueltoEn.minus(Duration.ofHours(horas)),
                resueltoEn, resultado));
    }

    private static TiempoPorUrgencia de(List<TiempoPorUrgencia> filas, Urgencia urgencia) {
        return filas.stream().filter(f -> f.getUrgencia().equals(urgencia.name())).findFirst().orElseThrow();
    }

    @Test
    void promediaLasHorasDeResolucionPorUrgencia() {
        Instant dia = Instant.parse("2026-11-10T15:00:00Z");
        resuelto(Urgencia.ALTA, dia, 10, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.ALTA, dia, 20, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.BAJA, dia, 48, ResultadoResolucion.COMPRADO);

        List<TiempoPorUrgencia> filas = resoluciones.tiempoPorUrgencia(DESDE, HASTA);

        assertThat(filas).hasSize(2);
        assertThat(de(filas, Urgencia.ALTA).getCantidad()).isEqualTo(2);
        assertThat(de(filas, Urgencia.ALTA).getPromedioHoras()).isEqualTo(15.0);
        assertThat(de(filas, Urgencia.BAJA).getCantidad()).isEqualTo(1);
        assertThat(de(filas, Urgencia.BAJA).getPromedioHoras()).isEqualTo(48.0);
    }

    @Test
    void cuentaCompradosYRechazados() {
        Instant dia = Instant.parse("2026-11-10T15:00:00Z");
        resuelto(Urgencia.MEDIA, dia, 4, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.MEDIA, dia, 8, ResultadoResolucion.RECHAZADO);

        TiempoPorUrgencia media = de(resoluciones.tiempoPorUrgencia(DESDE, HASTA), Urgencia.MEDIA);

        assertThat(media.getCantidad()).isEqualTo(2);
        assertThat(media.getPromedioHoras()).isEqualTo(6.0);
    }

    @Test
    void filtraPorMomentoDeResolucionConDesdeIncluidoYHastaExcluido() {
        resuelto(Urgencia.ALTA, DESDE.minusSeconds(1), 1, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.ALTA, DESDE, 2, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.ALTA, HASTA.minusSeconds(1), 4, ResultadoResolucion.COMPRADO);
        resuelto(Urgencia.ALTA, HASTA, 8, ResultadoResolucion.COMPRADO);

        TiempoPorUrgencia alta = de(resoluciones.tiempoPorUrgencia(DESDE, HASTA), Urgencia.ALTA);

        assertThat(alta.getCantidad()).isEqualTo(2);
        assertThat(alta.getPromedioHoras()).isEqualTo(3.0);
    }

    @Test
    void sinItemsResueltosEnElRangoNoDevuelveFilas() {
        resuelto(Urgencia.ALTA, Instant.parse("2026-10-10T15:00:00Z"), 5, ResultadoResolucion.COMPRADO);

        assertThat(resoluciones.tiempoPorUrgencia(DESDE, HASTA)).isEmpty();
    }
}
