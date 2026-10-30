package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
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
                                .isEqualTo(evento.ocurridoEn().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
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