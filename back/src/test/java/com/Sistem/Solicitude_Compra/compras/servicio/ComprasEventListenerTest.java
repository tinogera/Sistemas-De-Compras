package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.compartido.eventos.CancelacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemCancelado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemEditado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCancelada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.CompletedEventPublications;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.junit.jupiter.api.BeforeAll;
import java.util.TimeZone;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class ComprasEventListenerTest {

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired GestionItemRepository gestiones;
    @Autowired CompletedEventPublications completadas;

    // El listener corre en su propia transacción (REQUIRES_NEW): el test no hace rollback, hay que limpiar.
    @BeforeEach
    @AfterEach
    void limpiar() {
        gestiones.deleteAll();
    }

    @Test
    void solicitudCreadaConDosItemsCreaDosGestionesPendientes(Scenario scenario) {
        var evento = solicitudCreada();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gestiones.count(), cantidad -> cantidad == 2)
                .andVerify(cantidad -> {
                    assertThat(cantidad).isEqualTo(2);

                    List<GestionItem> guardadas = gestiones.findAll();
                    assertThat(guardadas)
                            .extracting(GestionItem::getItemId)
                            .containsExactlyInAnyOrder(100L, 101L);
                    assertThat(guardadas).allSatisfy(g -> {
                        assertThat(g.getEstado()).isEqualTo(EstadoGestion.PENDIENTE);
                        assertThat(g.getEstadoAprobacion()).isEqualTo(EstadoAprobacion.EN_CURSO);
                        assertThat(g.getSolicitudId()).isEqualTo(evento.solicitudId());
                        assertThat(g.getSectorId()).isEqualTo(evento.sectorId());
                        assertThat(g.getUrgencia()).isEqualTo(Urgencia.ALTA);
                        assertThat(g.getPrioridad()).isEqualTo((short) Urgencia.ALTA.prioridad());
                        assertThat(g.getFechaNecesaria()).isEqualTo(evento.fechaNecesaria());
                        assertThat(g.getSolicitadoEn().toInstant())
                                .isCloseTo(evento.ocurridoEn(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MILLIS));
                    });
                });
    }

    @Test
    void publicarElMismoEventoDosVecesNoDuplicaLasGestiones(Scenario scenario) {
        var evento = solicitudCreada();
        int publicacionesPrevias = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gestiones.count(), cantidad -> cantidad == 2)
                .andVerify(cantidad -> assertThat(cantidad).isEqualTo(2));

        // Segunda entrega del mismo evento: esperamos a que ambas publicaciones estén completadas
        // (no alcanza con mirar el count, porque justamente no debe cambiar).
        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(),
                        completadas -> completadas >= publicacionesPrevias + 2)
                .andVerify(completadas -> assertThat(gestiones.count()).isEqualTo(2));
    }

    // ---- T08.4: ediciones y cancelaciones que vienen de Solicitudes ----

    @Test
    void editarUnItemPendienteActualizaLaCopia(Scenario scenario) {
        guardarGestion(100L, false);

        scenario.publish(itemEditado(100L, "Resma A4 x 10", new BigDecimal("9")))
                .andWaitForStateChange(() -> gestiones.findById(100L).orElseThrow().getDetalle(),
                        detalle -> detalle.equals("Resma A4 x 10"))
                .andVerify(detalle -> {
                    GestionItem g = gestiones.findById(100L).orElseThrow();
                    assertThat(g.getCategoriaId()).isEqualTo(9L);
                    assertThat(g.getCantidad()).isEqualByComparingTo("9");
                    assertThat(g.getPrecioEstimado()).isEqualByComparingTo("2000.00");
                    assertThat(g.getEstado()).isEqualTo(EstadoGestion.PENDIENTE);
                });
    }

    @Test
    void editarUnItemYaTomadoNoCambiaLaCopia(Scenario scenario) {
        guardarGestion(100L, true);
        int previas = completadas.findAll().size();

        scenario.publish(itemEditado(100L, "Resma A4 x 10", new BigDecimal("9")))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> {
                    GestionItem g = gestiones.findById(100L).orElseThrow();
                    assertThat(g.getDetalle()).isEqualTo("Resma A4");
                    assertThat(g.getCantidad()).isEqualByComparingTo("3");
                    assertThat(g.getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
                });
    }

    @Test
    void cancelarUnItemPendientePasaLaGestionACancelado(Scenario scenario) {
        guardarGestion(100L, false);

        scenario.publish(new ItemCancelado(UUID.randomUUID(), Instant.now(), 100L, 10L, 1L))
                .andWaitForStateChange(() -> gestiones.findById(100L).orElseThrow().getEstado(),
                        estado -> estado == EstadoGestion.CANCELADO)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoGestion.CANCELADO));
    }

    @Test
    void cancelarUnItemYaTomadoPublicaCancelacionRechazadaYNoTocaLaGestion(Scenario scenario) {
        guardarGestion(100L, true);

        scenario.publish(new ItemCancelado(UUID.randomUUID(), Instant.now(), 100L, 10L, 1L))
                .andWaitForEventOfType(CancelacionRechazada.class)
                .toArriveAndVerify(evento -> {
                    assertThat(evento.itemId()).isEqualTo(100L);
                    assertThat(evento.solicitudId()).isEqualTo(10L);
                    assertThat(gestiones.findById(100L).orElseThrow().getEstado())
                            .isEqualTo(EstadoGestion.EN_COTIZACION);
                });
    }

    @Test
    void cancelarLaSolicitudCancelaLasPendientesYRechazaLasTomadas(Scenario scenario) {
        guardarGestion(100L, false);
        guardarGestion(101L, true);

        scenario.publish(new SolicitudCancelada(UUID.randomUUID(), Instant.now(), 10L,
                        List.of(100L, 101L), 1L))
                .andWaitForEventOfType(CancelacionRechazada.class)
                .toArriveAndVerify(evento -> {
                    assertThat(evento.itemId()).isEqualTo(101L);
                    assertThat(gestiones.findById(100L).orElseThrow().getEstado())
                            .isEqualTo(EstadoGestion.CANCELADO);
                    assertThat(gestiones.findById(101L).orElseThrow().getEstado())
                            .isEqualTo(EstadoGestion.EN_COTIZACION);
                });
    }

    @Test
    void cancelarDosVecesElMismoItemNoFalla(Scenario scenario) {
        guardarGestion(100L, false);
        var evento = new ItemCancelado(UUID.randomUUID(), Instant.now(), 100L, 10L, 1L);
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gestiones.findById(100L).orElseThrow().getEstado(),
                        estado -> estado == EstadoGestion.CANCELADO)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoGestion.CANCELADO));

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> assertThat(gestiones.findById(100L).orElseThrow().getEstado())
                        .isEqualTo(EstadoGestion.CANCELADO));
    }

    private void guardarGestion(Long itemId, boolean tomada) {
        GestionItem gestion = new GestionItem(itemId, 10L, 2L, 5L, TipoItem.PRODUCTO, "Resma A4",
                new BigDecimal("3"), Urgencia.ALTA, LocalDate.now().plusDays(5), OffsetDateTime.now(),
                new BigDecimal("1500.00"), Moneda.ARS);
        if (tomada) {
            gestion.tomar(77L);
        }
        gestiones.save(gestion);
    }

    private static ItemEditado itemEditado(Long itemId, String detalle, BigDecimal cantidad) {
        return new ItemEditado(UUID.randomUUID(), Instant.now(), itemId, 10L, 9L, cantidad, detalle,
                new BigDecimal("2000.00"), Moneda.ARS, 1L);
    }

    private static SolicitudCreada solicitudCreada() {
        return new SolicitudCreada(
                UUID.randomUUID(),
                Instant.now(),
                10L,
                1L,
                2L,
                Urgencia.ALTA,
                LocalDate.now().plusDays(5),
                List.of(
                        new SolicitudCreada.ItemCreado(100L, TipoItem.PRODUCTO, 5L,
                                new BigDecimal("3"), "Resma A4", new BigDecimal("1500.00"), Moneda.ARS),
                        new SolicitudCreada.ItemCreado(101L, TipoItem.SERVICIO, 6L,
                                new BigDecimal("1"), "Pelotero", new BigDecimal("180000.00"), Moneda.ARS)));
    }
}