package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.archivos.AlmacenamientoArchivos;
import com.Sistem.Solicitude_Compra.compartido.archivos.ValidadorArchivos;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
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
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class AprobacionesEventListenerIdempotenciaTest {

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
    @MockitoBean ValidadorArchivos validadorArchivos;
    @MockitoBean AlmacenamientoArchivos almacenamientoArchivos;

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

    private void publicarDosVeces(Scenario scenario, Object evento) {
        int previas = completadas.findAll().size();
        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> { });
        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> { });
    }

    @Test
    void aprobacionCompletadaDosVecesDejaLaCompraAprobada(Scenario scenario) {
        guardar(true);

        publicarDosVeces(scenario, new AprobacionCompletada(UUID.randomUUID(), Instant.now(), ITEM, 10L, false));

        assertThat(gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion()).isEqualTo(EstadoAprobacion.APROBADA);
    }

    @Test
    void aprobacionReabiertaDosVecesDejaLaCompraEnCurso(Scenario scenario) {
        GestionItem g = guardar(true);
        g.marcarAprobada();
        gestiones.saveAndFlush(g);

        publicarDosVeces(scenario,
                new AprobacionReabierta(UUID.randomUUID(), Instant.now(), ITEM, 10L, "Cambió el precio"));

        assertThat(gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion()).isEqualTo(EstadoAprobacion.EN_CURSO);
    }
}
