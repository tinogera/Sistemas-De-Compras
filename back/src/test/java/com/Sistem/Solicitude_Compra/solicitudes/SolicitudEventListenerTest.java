package com.Sistem.Solicitude_Compra.solicitudes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.CancelacionRechazada;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
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
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class SolicitudEventListenerTest {

    @Autowired SolicitudService service;
    @Autowired SolicitudRepository solicitudes;
    @Autowired ItemRepository items;
    @Autowired CompletedEventPublications completadas;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    // El listener corre en su propia transacción (REQUIRES_NEW): el test no hace rollback, hay que limpiar.
    @BeforeEach
    void preparar() {
        solicitudes.deleteAll();
        when(authApi.sectorDe(1L)).thenReturn(1L);
        when(catalogoApi.existeCategoria(5L)).thenReturn(true);
    }

    @AfterEach
    void limpiar() {
        solicitudes.deleteAll();
    }

    @Test
    void siComprasRechazaLaCancelacionElItemVuelveAEnCotizacionYLaSolicitudAEnRevision(Scenario scenario) {
        Solicitud solicitud = crearSolicitudDeUnItem();
        Long itemId = solicitud.getItems().get(0).getId();
        Item item = items.findById(itemId).orElseThrow();
        item.setEstado(EstadoItem.CANCELADO);
        items.save(item);
        Solicitud cancelada = solicitudes.findById(solicitud.getId()).orElseThrow();
        cancelada.setEstado(EstadoSolicitud.CANCELADA);
        solicitudes.save(cancelada);
        assertThat(estadoItem(itemId)).isEqualTo(EstadoItem.CANCELADO);
        assertThat(estadoSolicitud(solicitud.getId())).isEqualTo(EstadoSolicitud.CANCELADA);

        scenario.publish(new CancelacionRechazada(UUID.randomUUID(), Instant.now(), itemId, solicitud.getId()))
                .andWaitForStateChange(() -> estadoItem(itemId), estado -> estado == EstadoItem.EN_COTIZACION)
                .andVerify(estado -> assertThat(estadoSolicitud(solicitud.getId()))
                        .isEqualTo(EstadoSolicitud.EN_REVISION));
    }

    @Test
    void unaCancelacionRechazadaSobreUnItemNoCanceladoSeIgnora(Scenario scenario) {
        Solicitud solicitud = crearSolicitudDeUnItem();
        Long itemId = solicitud.getItems().get(0).getId();
        int previas = completadas.findAll().size();

        scenario.publish(new CancelacionRechazada(UUID.randomUUID(), Instant.now(), itemId, solicitud.getId()))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> {
                    assertThat(estadoItem(itemId)).isEqualTo(EstadoItem.PENDIENTE);
                    assertThat(estadoSolicitud(solicitud.getId())).isEqualTo(EstadoSolicitud.EN_REVISION);
                });
    }

    private Solicitud crearSolicitudDeUnItem() {
        var request = new CrearSolicitudRequest(Urgencia.ALTA, LocalDate.now().plusDays(4), "obs",
                List.of(new ItemRequest(TipoItem.SERVICIO, 5L, null, new BigDecimal("3"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS)));
        Solicitud creada = service.crear(1L, request);
        // Se vuelve a leer: hace falta la versión y los ítems ya persistidos.
        return solicitudes.findConItemsById(creada.getId()).orElseThrow();
    }

    private EstadoItem estadoItem(Long itemId) {
        return items.findById(itemId).orElseThrow().getEstado();
    }

    private EstadoSolicitud estadoSolicitud(Long solicitudId) {
        return solicitudes.findById(solicitudId).orElseThrow().getEstado();
    }
}
