package com.Sistem.Solicitude_Compra.reportes.repositorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.dominio.EventoProcesadoReportes;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import com.Sistem.Solicitude_Compra.reportes.dominio.ResultadoResolucion;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** T16.1: con ddl-auto=validate, que este contexto arranque ya prueba que las entidades coinciden con la migración. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ReportesRepositoriesTest {

    @Autowired HechoGastoRepository gastos;
    @Autowired HechoResolucionRepository resoluciones;
    @Autowired EventoProcesadoReportesRepository procesados;
    @Autowired EntityManager em;

    private static HechoGasto gasto(Long ordenId, Long itemId, Moneda moneda) {
        return new HechoGasto(ordenId, itemId, LocalDate.of(2026, 11, 20), 2L, 3L, 4L, moneda,
                new BigDecimal("1000.00"));
    }

    @Test
    void guardaElGastoConElIdDeLaOrdenYElEnvioEnCero() {
        gastos.saveAndFlush(gasto(101L, 10L, Moneda.USD));
        em.clear();

        HechoGasto leido = gastos.findById(101L).orElseThrow();

        assertThat(leido.getItemId()).isEqualTo(10L);
        assertThat(leido.getMoneda()).isEqualTo(Moneda.USD);
        assertThat(leido.getFecha()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(leido.getImporte()).isEqualByComparingTo("1000.00");
        assertThat(leido.getMontoEnvio()).isEqualByComparingTo("0");
        assertThat(leido.isNew()).isFalse();
    }

    @Test
    void encuentraElGastoPorItem() {
        gastos.saveAndFlush(gasto(101L, 10L, Moneda.ARS));
        em.clear();

        assertThat(gastos.findByItemId(10L)).isPresent();
        assertThat(gastos.existsByItemId(10L)).isTrue();
        assertThat(gastos.existsByItemId(99L)).isFalse();
    }

    @Test
    void laCorreccionDeMontosSeGuarda() {
        gastos.saveAndFlush(gasto(101L, 10L, Moneda.ARS));
        em.clear();

        HechoGasto g = gastos.findByItemId(10L).orElseThrow();
        g.actualizarMontos(new BigDecimal("1800.00"), new BigDecimal("250.00"));
        gastos.saveAndFlush(g);
        em.clear();

        HechoGasto leido = gastos.findById(101L).orElseThrow();
        assertThat(leido.getImporte()).isEqualByComparingTo("1800.00");
        assertThat(leido.getMontoEnvio()).isEqualByComparingTo("250.00");
    }

    @Test
    void noPuedeHaberDosGastosParaElMismoItem() {
        gastos.saveAndFlush(gasto(101L, 10L, Moneda.ARS));

        assertThatThrownBy(() -> gastos.saveAndFlush(gasto(102L, 10L, Moneda.ARS)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void insertarDosVecesLaMismaOrdenFallaEnVezDeSobrescribir() {
        gastos.saveAndFlush(gasto(101L, 10L, Moneda.ARS));

        assertThatThrownBy(() -> gastos.saveAndFlush(gasto(101L, 11L, Moneda.ARS)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardaLaResolucionConSusEnums() {
        Instant creado = Instant.parse("2026-11-10T10:00:00Z");
        resoluciones.saveAndFlush(new HechoResolucion(10L, Urgencia.MEDIA, creado, creado.plusSeconds(7200),
                ResultadoResolucion.COMPRADO));
        em.clear();

        HechoResolucion leida = resoluciones.findById(10L).orElseThrow();

        assertThat(leida.getUrgencia()).isEqualTo(Urgencia.MEDIA);
        assertThat(leida.getResultado()).isEqualTo(ResultadoResolucion.COMPRADO);
        assertThat(leida.getCreadoEn()).isEqualTo(creado);
        assertThat(leida.getResueltoEn()).isEqualTo(creado.plusSeconds(7200));
    }

    @Test
    void insertarDosVecesLaMismaResolucionFalla() {
        Instant creado = Instant.parse("2026-11-10T10:00:00Z");
        resoluciones.saveAndFlush(new HechoResolucion(10L, Urgencia.ALTA, creado, creado, ResultadoResolucion.COMPRADO));

        assertThatThrownBy(() -> resoluciones.saveAndFlush(
                new HechoResolucion(10L, Urgencia.ALTA, creado, creado, ResultadoResolucion.RECHAZADO)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void elEventoProcesadoSeGuardaUnaSolaVez() {
        UUID id = UUID.randomUUID();
        procesados.saveAndFlush(new EventoProcesadoReportes(id));
        em.clear();

        assertThat(procesados.existsById(id)).isTrue();
        assertThatThrownBy(() -> procesados.saveAndFlush(new EventoProcesadoReportes(id)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
