package com.Sistem.Solicitude_Compra.reportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.eventos.CategoriaUnificada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import com.Sistem.Solicitude_Compra.reportes.dominio.HechoResolucion;
import com.Sistem.Solicitude_Compra.reportes.dominio.ResultadoResolucion;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoGastoRepository;
import com.Sistem.Solicitude_Compra.reportes.repositorio.HechoResolucionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

/** T16.2: eventos de entrada del módulo Reportes, con Scenario. */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class AlimentadorDeReportesEventosTest {

    private static final Long ORDEN = 8101L;
    private static final Long ITEM = 8102L;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired HechoGastoRepository gastos;
    @Autowired HechoResolucionRepository resoluciones;
    @Autowired CompletedEventPublications completadas;

    // Los listeners corren en su propia transacción: no hay rollback del test, hay que limpiar.
    @BeforeEach
    @AfterEach
    void limpiar() {
        gastos.deleteAll();
        resoluciones.deleteAll();
    }

    private static OrdenGenerada ordenGenerada() {
        return new OrdenGenerada(UUID.randomUUID(), Instant.parse("2026-11-20T15:00:00Z"), ORDEN, 7L, ITEM, 20L,
                30L, 40L, 50L, Moneda.USD, new BigDecimal("1000.00"));
    }

    private static CompraActualizada compraActualizada() {
        return new CompraActualizada(UUID.randomUUID(), Instant.now(), ITEM, 20L, new BigDecimal("1000.00"),
                new BigDecimal("1800.00"), null, new BigDecimal("250.00"), Moneda.USD, 9L);
    }

    private static ItemResuelto itemResuelto(Long itemId, String resultado) {
        return new ItemResuelto(UUID.randomUUID(), Instant.now(), itemId, 20L, resultado, "Proveedor SA", null,
                null, Urgencia.ALTA, Instant.parse("2026-11-10T10:00:00Z"), Instant.parse("2026-11-12T10:00:00Z"), 9L);
    }

    private void generarGasto(Scenario scenario) {
        scenario.publish(ordenGenerada())
                .andWaitForStateChange(() -> gastos.existsByItemId(ITEM), existe -> existe)
                .andVerify(existe -> assertThat(existe).isTrue());
    }

    @Test
    void ordenGeneradaCreaUnHechoDeGastoConEnvioCero(Scenario scenario) {
        scenario.publish(ordenGenerada())
                .andWaitForStateChange(() -> gastos.findById(ORDEN), g -> g.isPresent())
                .andVerify(g -> {
                    HechoGasto gasto = g.orElseThrow();
                    assertThat(gasto.getItemId()).isEqualTo(ITEM);
                    assertThat(gasto.getFecha()).isEqualTo(LocalDate.of(2026, 11, 20));
                    assertThat(gasto.getSectorId()).isEqualTo(30L);
                    assertThat(gasto.getCategoriaId()).isEqualTo(40L);
                    assertThat(gasto.getProveedorId()).isEqualTo(50L);
                    assertThat(gasto.getMoneda()).isEqualTo(Moneda.USD);
                    assertThat(gasto.getImporte()).isEqualByComparingTo("1000.00");
                    assertThat(gasto.getMontoEnvio()).isEqualByComparingTo("0");
                });
    }

    @Test
    void ordenGeneradaDosVecesCreaUnSoloHecho(Scenario scenario) {
        var evento = ordenGenerada();
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gastos.count(), c -> c == 1)
                .andVerify(c -> assertThat(c).isEqualTo(1));

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> assertThat(gastos.count()).isEqualTo(1));
    }

    @Test
    void compraActualizadaCorrigeImporteYEnvioYRepetirlaNoCambiaNada(Scenario scenario) {
        generarGasto(scenario);
        var evento = compraActualizada();
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gastos.findById(ORDEN).orElseThrow().getMontoEnvio(),
                        envio -> envio.signum() > 0)
                .andVerify(envio -> {
                    HechoGasto g = gastos.findById(ORDEN).orElseThrow();
                    assertThat(g.getImporte()).isEqualByComparingTo("1800.00");
                    assertThat(g.getMontoEnvio()).isEqualByComparingTo("250.00");
                });

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> {
                    HechoGasto g = gastos.findById(ORDEN).orElseThrow();
                    assertThat(g.getImporte()).isEqualByComparingTo("1800.00");
                    assertThat(g.getMontoEnvio()).isEqualByComparingTo("250.00");
                });
    }

    @Test
    void itemResueltoCreaUnHechoDeResolucion(Scenario scenario) {
        scenario.publish(itemResuelto(ITEM, "COMPRADO"))
                .andWaitForStateChange(() -> resoluciones.findById(ITEM), r -> r.isPresent())
                .andVerify(r -> {
                    HechoResolucion h = r.orElseThrow();
                    assertThat(h.getUrgencia()).isEqualTo(Urgencia.ALTA);
                    assertThat(h.getResultado()).isEqualTo(ResultadoResolucion.COMPRADO);
                    assertThat(h.getCreadoEn()).isEqualTo(Instant.parse("2026-11-10T10:00:00Z"));
                    assertThat(h.getResueltoEn()).isEqualTo(Instant.parse("2026-11-12T10:00:00Z"));
                });
    }

    @Test
    void itemResueltoRechazadoYRepetidoCreaUnSoloHecho(Scenario scenario) {
        var evento = itemResuelto(ITEM, "RECHAZADO");
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> resoluciones.count(), c -> c == 1)
                .andVerify(c -> assertThat(resoluciones.findById(ITEM).orElseThrow().getResultado())
                        .isEqualTo(ResultadoResolucion.RECHAZADO));

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> assertThat(resoluciones.count()).isEqualTo(1));
    }

    @Test
    void categoriaUnificadaReasignaSoloLosGastosDeLaCategoriaOrigen(Scenario scenario) {
        generarGasto(scenario);
        gastos.saveAndFlush(new HechoGasto(8201L, 8202L, LocalDate.of(2026, 11, 20), 30L, 41L, 50L, Moneda.ARS,
                new BigDecimal("10.00")));

        scenario.publish(new CategoriaUnificada(UUID.randomUUID(), Instant.now(), 40L, 99L, 1L))
                .andWaitForStateChange(() -> gastos.findById(ORDEN).orElseThrow().getCategoriaId(),
                        categoria -> categoria == 99L)
                .andVerify(categoria -> {
                    assertThat(gastos.findById(ORDEN).orElseThrow().getCategoriaId()).isEqualTo(99L);
                    assertThat(gastos.findById(8201L).orElseThrow().getCategoriaId()).isEqualTo(41L);
                });
    }
}
