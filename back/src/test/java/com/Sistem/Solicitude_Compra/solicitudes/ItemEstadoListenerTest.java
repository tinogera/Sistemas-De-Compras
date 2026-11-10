package com.Sistem.Solicitude_Compra.solicitudes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionReabierta;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemAsignado;
import com.Sistem.Solicitude_Compra.compartido.eventos.ItemResuelto;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenEnviada;
import com.Sistem.Solicitude_Compra.compartido.eventos.PrecioTotalActualizado;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
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
class ItemEstadoListenerTest {

    @Autowired SolicitudService service;
    @Autowired SolicitudRepository solicitudes;
    @Autowired ItemRepository items;
    @Autowired CompletedEventPublications completadas;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    private Long solicitudId;
    private Long itemId;

    // Los listeners corren en su propia transacción (REQUIRES_NEW): el test no hace rollback, hay que limpiar.
    @BeforeEach
    void preparar() {
        solicitudes.deleteAll();
        when(authApi.sectorDe(1L)).thenReturn(1L);
        when(catalogoApi.existeCategoria(5L)).thenReturn(true);
        Solicitud creada = service.crear(1L, new CrearSolicitudRequest(Urgencia.ALTA,
                LocalDate.now().plusDays(4), "obs",
                List.of(new ItemRequest(TipoItem.SERVICIO, 5L, null, new BigDecimal("3"), "Pelotero",
                        new BigDecimal("180000"), Moneda.ARS))));
        solicitudId = creada.getId();
        itemId = solicitudes.findConItemsById(solicitudId).orElseThrow().getItems().get(0).getId();
    }

    @AfterEach
    void limpiar() {
        solicitudes.deleteAll();
    }

    @Test
    void asignarUnItemPendienteLoPasaAEnCotizacion(Scenario scenario) {
        scenario.publish(new ItemAsignado(UUID.randomUUID(), Instant.now(), itemId, solicitudId, null, 77L))
                .andWaitForStateChange(this::estado, e -> e == EstadoItem.EN_COTIZACION)
                .andVerify(e -> assertThat(e).isEqualTo(EstadoItem.EN_COTIZACION));
    }

    @Test
    void asignarUnItemCanceladoNoLoCambia(Scenario scenario) {
        forzarEstado(EstadoItem.CANCELADO);
        int previas = completadas.findAll().size();

        scenario.publish(new ItemAsignado(UUID.randomUUID(), Instant.now(), itemId, solicitudId, null, 77L))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> assertThat(estado()).isEqualTo(EstadoItem.CANCELADO));
    }

    @Test
    void elPrecioTotalSeGuardaYSeQuitaCuandoLlegaNull(Scenario scenario) {
        scenario.publish(precioTotal(new BigDecimal("150000.00"), Moneda.ARS))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getPrecioTotal() != null,
                        guardado -> guardado)
                .andVerify(guardado -> {
                    Item item = items.findById(itemId).orElseThrow();
                    assertThat(item.getPrecioTotal()).isEqualByComparingTo("150000.00");
                    assertThat(item.getMonedaTotal()).isEqualTo(Moneda.ARS);
                });

        scenario.publish(precioTotal(null, null))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getPrecioTotal() == null,
                        quitado -> quitado)
                .andVerify(quitado -> assertThat(items.findById(itemId).orElseThrow().getMonedaTotal()).isNull());
    }

    @Test
    void unaCompraActualizadaGuardaElPrecioTotalCorregido(Scenario scenario) {
        scenario.publish(new CompraActualizada(UUID.randomUUID(), Instant.now(), itemId, solicitudId,
                        new BigDecimal("150000.00"), new BigDecimal("170000.00"), null, null, Moneda.USD, 7L))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getPrecioTotal() != null,
                        guardado -> guardado)
                .andVerify(guardado -> {
                    Item item = items.findById(itemId).orElseThrow();
                    assertThat(item.getPrecioTotal()).isEqualByComparingTo("170000.00");
                    assertThat(item.getMonedaTotal()).isEqualTo(Moneda.USD);
                });
    }

    @Test
    void laAprobacionCambiaSoloElEstadoDeAprobacionYSeReabre(Scenario scenario) {
        scenario.publish(new AprobacionCompletada(UUID.randomUUID(), Instant.now(), itemId, solicitudId, false))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getEstadoAprobacion(),
                        a -> a == EstadoAprobacion.APROBADA)
                .andVerify(a -> assertThat(estado()).isEqualTo(EstadoItem.PENDIENTE));

        scenario.publish(new AprobacionReabierta(UUID.randomUUID(), Instant.now(), itemId, solicitudId, "cambió el monto"))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getEstadoAprobacion(),
                        a -> a == EstadoAprobacion.EN_CURSO)
                .andVerify(a -> assertThat(estado()).isEqualTo(EstadoItem.PENDIENTE));
    }

    @Test
    void resolverComoCompradoGuardaElProveedor(Scenario scenario) {
        scenario.publish(itemResuelto("COMPRADO", "Proveedor SA", null, null))
                .andWaitForStateChange(this::estado, e -> e == EstadoItem.COMPRADO)
                .andVerify(e -> assertThat(items.findById(itemId).orElseThrow().getProveedorComprado())
                        .isEqualTo("Proveedor SA"));
    }

    @Test
    void resolverComoRechazadoGuardaMotivoYQuien(Scenario scenario) {
        scenario.publish(itemResuelto("RECHAZADO", null, "Sin presupuesto", "Gerencia General"))
                .andWaitForStateChange(this::estado, e -> e == EstadoItem.RECHAZADO)
                .andVerify(e -> {
                    Item item = items.findById(itemId).orElseThrow();
                    assertThat(item.getMotivoRechazo()).isEqualTo("Sin presupuesto");
                    assertThat(item.getRechazadoPor()).isEqualTo("Gerencia General");
                });
    }

    @Test
    void resolverUnItemCanceladoNoLoCambia(Scenario scenario) {
        forzarEstado(EstadoItem.CANCELADO);
        int previas = completadas.findAll().size();

        scenario.publish(itemResuelto("COMPRADO", "Proveedor SA", null, null))
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 1)
                .andVerify(c -> {
                    assertThat(estado()).isEqualTo(EstadoItem.CANCELADO);
                    assertThat(items.findById(itemId).orElseThrow().getProveedorComprado()).isNull();
                });
    }

    @Test
    void elEnvioDeLaOrdenMarcaElItem(Scenario scenario) {
        scenario.publish(new OrdenEnviada(UUID.randomUUID(), Instant.now(), 900L, itemId, solicitudId, 7L))
                .andWaitForStateChange(() -> items.findById(itemId).orElseThrow().getOrdenEnviada(),
                        enviada -> enviada)
                .andVerify(enviada -> assertThat(enviada).isTrue());
    }

    private EstadoItem estado() {
        return items.findById(itemId).orElseThrow().getEstado();
    }

    private void forzarEstado(EstadoItem estado) {
        Item item = items.findById(itemId).orElseThrow();
        item.setEstado(estado);
        items.save(item);
    }

    private PrecioTotalActualizado precioTotal(BigDecimal precio, Moneda moneda) {
        return new PrecioTotalActualizado(UUID.randomUUID(), Instant.now(), itemId, solicitudId, precio, moneda,
                null, null, 7L);
    }

    private ItemResuelto itemResuelto(String resultado, String proveedor, String motivo, String rechazadoPor) {
        return new ItemResuelto(UUID.randomUUID(), Instant.now(), itemId, solicitudId, resultado, proveedor,
                motivo, rechazadoPor, Urgencia.ALTA, Instant.now(), Instant.now(), 7L);
    }
}
