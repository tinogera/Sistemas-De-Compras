package com.Sistem.Solicitude_Compra.reportes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import com.Sistem.Solicitude_Compra.reportes.dominio.ResultadoResolucion;
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
class HechoResolucionTiemposTest {

    private static final Instant CREADO = Instant.parse("2026-11-10T12:00:00Z");
    private static final Instant DESDE = Instant.parse("2026-11-01T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2026-12-01T03:00:00Z");

    @Autowired HechoResolucionRepository repo;

    @BeforeEach
    void limpiar() {
        repo.deleteAll();
    }

    private void resuelto(long id, Urgencia urgencia, Instant creado, long horas, ResultadoResolucion resultado) {
        repo.saveAndFlush(new HechoResolucion(id, urgencia, creado, creado.plusSeconds(horas * 3600), resultado));
    }

    private static TiempoPorUrgencia de(List<TiempoPorUrgencia> filas, String urgencia) {
        return filas.stream().filter(f -> f.getUrgencia().equals(urgencia)).findFirst().orElse(null);
    }

    @Test
    void dosItemsAltaResueltosEn10Y20HorasDanPromedio15() {
        resuelto(1, Urgencia.ALTA, CREADO, 10, ResultadoResolucion.COMPRADO);
        resuelto(2, Urgencia.ALTA, CREADO, 20, ResultadoResolucion.RECHAZADO);

        List<TiempoPorUrgencia> filas = repo.tiemposPorUrgencia(DESDE, HASTA);

        assertThat(filas).hasSize(1);
        assertThat(de(filas, "ALTA").getCantidad()).isEqualTo(2);
        assertThat(de(filas, "ALTA").getPromedioHoras()).isEqualTo(15.0);
        assertThat(de(filas, "MEDIA")).isNull();
    }

    @Test
    void filtraPorCuandoSeResolvioYNoPorCuandoSeCreo() {
        // Creado en octubre pero resuelto en noviembre: entra.
        resuelto(1, Urgencia.MEDIA, Instant.parse("2026-10-30T12:00:00Z"), 48, ResultadoResolucion.COMPRADO);
        // Resuelto justo en el límite superior (exclusivo): no entra.
        repo.saveAndFlush(new HechoResolucion(2L, Urgencia.MEDIA, Instant.parse("2026-11-30T12:00:00Z"),
                HASTA, ResultadoResolucion.COMPRADO));
        // Resuelto en el límite inferior (inclusivo): entra.
        repo.saveAndFlush(new HechoResolucion(3L, Urgencia.BAJA, Instant.parse("2026-10-31T12:00:00Z"),
                DESDE, ResultadoResolucion.RECHAZADO));

        List<TiempoPorUrgencia> filas = repo.tiemposPorUrgencia(DESDE, HASTA);

        assertThat(de(filas, "MEDIA").getCantidad()).isEqualTo(1);
        assertThat(de(filas, "BAJA").getCantidad()).isEqualTo(1);
    }
}
