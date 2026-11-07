package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
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
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class AprobacionesEventListenerTest {

    private static final Long ITEM = 100L;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired GestionItemRepository gestiones;
    @Autowired CompletedEventPublications completadas;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    @AfterEach
    void limpiar() {
        gestiones.deleteAll();
    }

    private GestionItem guardar(boolean tomada) {
        GestionItem g = new GestionItem(ITEM, 10L, 2L, 5L, TipoItem.PRODUCTO, "Resma A4", new BigDecimal("3"),
                Urgencia.ALTA, LocalDate.now().plusDays(5), OffsetDateTime.now(), new BigDecimal("1500.00"),
                Moneda.ARS);
        if (tomada) {
            g.tomar(1L);
        }
        return gestiones.saveAndFlush(g);
    }

    @Test
    void aprobacionCompletadaHabilitaLaCompra(Scenario scenario) {
        guardar(true);

        scenario.publish(new AprobacionCompletada(UUID.randomUUID(), Instant.now(), ITEM, 10L, false))
                .andWaitForStateChange(() -> gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion(),
                        estado -> estado == EstadoAprobacion.APROBADA)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoAprobacion.APROBADA));
    }

    @Test
    void aprobacionReabiertaBloqueaLaCompra(Scenario scenario) {
        GestionItem g = guardar(true);
        g.marcarAprobada();
        gestiones.saveAndFlush(g);

        scenario.publish(new AprobacionReabierta(UUID.randomUUID(), Instant.now(), ITEM, 10L, "Cambió el precio"))
                .andWaitForStateChange(() -> gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion(),
                        estado -> estado == EstadoAprobacion.EN_CURSO)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoAprobacion.EN_CURSO));
    }

    @Test
    void unAprobadorRechazaElItemYSePublicaItemResuelto(Scenario scenario) {
        guardar(true);

        scenario.publish(rechazo("No hay presupuesto"))
                .andWaitForEventOfType(ItemResuelto.class)
                .toArriveAndVerify(resuelto -> {
                    assertThat(resuelto.itemId()).isEqualTo(ITEM);
                    assertThat(resuelto.resultado()).isEqualTo("RECHAZADO");
                    assertThat(resuelto.rechazadoPor()).isEqualTo("Gerencia General");
                    assertThat(resuelto.motivoRechazo()).isEqualTo("No hay presupuesto");
                    GestionItem g = gestiones.findById(ITEM).orElseThrow();
                    assertThat(g.getEstado()).isEqualTo(EstadoGestion.RECHAZADO);
                    assertThat(g.getMotivoRechazo()).isEqualTo("No hay presupuesto");
                    assertThat(g.getResueltoEn()).isNotNull();
                });
    }

    @Test
    void elRechazoAplicaTambienAUnItemSinTomar(Scenario scenario) {
        guardar(false);

        scenario.publish(rechazo("No"))
                .andWaitForStateChange(() -> gestiones.findById(ITEM).orElseThrow().getEstado(),
                        estado -> estado == EstadoGestion.RECHAZADO)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoGestion.RECHAZADO));
    }

    @Test
    void publicarElRechazoDosVecesNoDuplicaItemResuelto(Scenario scenario, PublishedEvents publicados) {
        guardar(true);
        var evento = rechazo("No hay presupuesto");
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> gestiones.findById(ITEM).orElseThrow().getEstado(),
                        estado -> estado == EstadoGestion.RECHAZADO)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoGestion.RECHAZADO));

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> assertThat(publicados.ofType(ItemResuelto.class).matching(e -> ITEM.equals(e.itemId())))
                        .hasSize(1));
    }

    @Test
    void siElItemYaEstabaCompradoElRechazoNoHaceNada(Scenario scenario) {
        GestionItem g = guardar(true);
        g.marcarAprobada();
        g.fijarPrecioTotal(new BigDecimal("900.00"), Moneda.ARS, null);
        g.registrarCompra(9L, OffsetDateTime.now());
        gestiones.saveAndFlush(g);
        int previas = completadas.findAll().size();

        scenario.publish(rechazo("Tarde"))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> assertThat(gestiones.findById(ITEM).orElseThrow().getEstado())
                        .isEqualTo(EstadoGestion.COMPRADO));
    }

    private static AprobacionRechazada rechazo(String motivo) {
        return new AprobacionRechazada(UUID.randomUUID(), Instant.now(), ITEM, 10L, 1L, 4L, "Gerencia General", motivo);
    }
}
