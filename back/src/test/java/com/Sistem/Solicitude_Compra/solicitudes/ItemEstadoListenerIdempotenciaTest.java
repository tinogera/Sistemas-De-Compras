package com.Sistem.Solicitude_Compra.solicitudes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.archivos.AlmacenamientoArchivos;
import com.Sistem.Solicitude_Compra.compartido.archivos.ValidadorArchivos;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCerrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudLista;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.ItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import com.Sistem.Solicitude_Compra.solicitudes.web.CrearSolicitudRequest;
import com.Sistem.Solicitude_Compra.solicitudes.web.ItemRequest;
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
import org.springframework.modulith.test.PublishedEvents;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * T12.2: cada listener de Solicitudes recibe el mismo evento (mismo eventoId) dos veces y el resultado
 * tiene que ser el de una sola entrega. Las esperas miran las publicaciones completadas, por eso estos tests
 * necesitan spring.modulith.events.completion-mode=update (ver src/test/resources/config/application.properties).
 */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class ItemEstadoListenerIdempotenciaTest {

    @Autowired SolicitudService service;
    @Autowired SolicitudRepository solicitudes;
    @Autowired ItemRepository items;
    @Autowired CompletedEventPublications completadas;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;
    @MockitoBean ValidadorArchivos validadorArchivos;
    @MockitoBean AlmacenamientoArchivos almacenamientoArchivos;

    private Long solicitudId;
    private Long itemId;

    @BeforeEach
    void preparar() {
        solicitudes.deleteAll();
        when(authApi.sectorDe(1L)).thenReturn(1L);
        when(catalogoApi.existeCategoria(5L)).thenReturn(true);
        solicitudId = service.crear(1L, new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(4), "obs",
                List.of(new ItemRequest(TipoItem.SERVICIO, 5L, null, new BigDecimal("3"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS)))).getId();
        itemId = solicitudes.findConItemsById(solicitudId).orElseThrow().getItems().get(0).getId();
    }

    @AfterEach
    void limpiar() {
        solicitudes.deleteAll();
    }

    /** Publica el mismo evento dos veces y espera a que las dos entregas estén completadas. */
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
    void itemAsignadoDosVecesDejaElItemEnCotizacion(Scenario scenario) {
        publicarDosVeces(scenario, new ItemAsignado(UUID.randomUUID(), Instant.now(), itemId, solicitudId, null, 77L));

        assertThat(items.findById(itemId).orElseThrow().getEstado()).isEqualTo(EstadoItem.EN_COTIZACION);
    }

    @Test
    void precioTotalDosVecesGuardaElMismoPrecio(Scenario scenario) {
        publicarDosVeces(scenario, new PrecioTotalActualizado(UUID.randomUUID(), Instant.now(), itemId, solicitudId,
                new BigDecimal("150000.00"), Moneda.ARS, null, null, 7L));

        var item = items.findById(itemId).orElseThrow();
        assertThat(item.getPrecioTotal()).isEqualByComparingTo("150000.00");
        assertThat(item.getMonedaTotal()).isEqualTo(Moneda.ARS);
    }

    @Test
    void aprobacionCompletadaDosVecesDejaAprobada(Scenario scenario) {
        publicarDosVeces(scenario,
                new AprobacionCompletada(UUID.randomUUID(), Instant.now(), itemId, solicitudId, false));

        assertThat(items.findById(itemId).orElseThrow().getEstadoAprobacion()).isEqualTo(EstadoAprobacion.APROBADA);
    }

    @Test
    void itemResueltoDosVecesPublicaSolicitudListaUnaSolaVez(Scenario scenario, PublishedEvents publicados) {
        publicarDosVeces(scenario, new ItemResuelto(UUID.randomUUID(), Instant.now(), itemId, solicitudId,
                "COMPRADO", "Proveedor SA", null, null, Urgencia.ALTA, Instant.now(), Instant.now(), 7L));

        assertThat(items.findById(itemId).orElseThrow().getEstado()).isEqualTo(EstadoItem.COMPRADO);
        assertThat(solicitudes.findById(solicitudId).orElseThrow().getEstado()).isEqualTo(EstadoSolicitud.LISTA);
        assertThat(publicados.ofType(SolicitudLista.class).matching(e -> solicitudId.equals(e.solicitudId())))
                .hasSize(1);
    }

    @Test
    void ordenEnviadaDosVecesPublicaSolicitudCerradaUnaSolaVez(Scenario scenario, PublishedEvents publicados) {
        publicarDosVeces(scenario, new ItemResuelto(UUID.randomUUID(), Instant.now(), itemId, solicitudId,
                "COMPRADO", "Proveedor SA", null, null, Urgencia.ALTA, Instant.now(), Instant.now(), 7L));
        publicarDosVeces(scenario, new OrdenEnviada(UUID.randomUUID(), Instant.now(), 900L, itemId, solicitudId, 7L));

        assertThat(solicitudes.findById(solicitudId).orElseThrow().getEstado()).isEqualTo(EstadoSolicitud.CERRADA);
        assertThat(publicados.ofType(SolicitudCerrada.class).matching(e -> solicitudId.equals(e.solicitudId())))
                .hasSize(1);
    }
}
