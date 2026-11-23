package com.Sistem.Solicitude_Compra.ordenes.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraActualizada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.eventos.OrdenGenerada;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import java.math.BigDecimal;
import java.time.Instant;
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

/** T12.3: el módulo Órdenes reacciona a los eventos de Compras (se prueba solo el módulo, con Scenario). */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class OrdenesEventListenerTest {

    private static final Long ITEM = 300L;

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired OrdenPedidoRepository ordenes;
    @Autowired CompletedEventPublications completadas;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    @AfterEach
    void limpiar() {
        ordenes.deleteAll();
    }

    private CompraRegistrada compra() {
        return new CompraRegistrada(UUID.randomUUID(), Instant.now(), ITEM, 10L, 2L, 5L, TipoItem.PRODUCTO,
                "Resma A4", new BigDecimal("3"), 8L, "Papelera Central SA", 9L, Moneda.ARS,
                new BigDecimal("1200.00"), 1L);
    }

    @Test
    void compraRegistradaGeneraUnaOrdenYPublicaOrdenGenerada(Scenario scenario) {
        scenario.publish(compra())
                .andWaitForEventOfType(OrdenGenerada.class)
                .toArriveAndVerify(generada -> {
                    assertThat(generada.itemId()).isEqualTo(ITEM);
                    assertThat(generada.proveedorId()).isEqualTo(8L);
                    var orden = ordenes.findByItemId(ITEM).orElseThrow();
                    assertThat(orden.getNumero()).isEqualTo(generada.numero());
                    assertThat(orden.getPrecioTotal()).isEqualByComparingTo("1200.00");
                });
    }

    @Test
    void compraRegistradaDosVecesDejaUnaSolaOrdenYUnSoloOrdenGenerada(Scenario scenario, PublishedEvents publicados) {
        var evento = compra();
        int previas = completadas.findAll().size();

        scenario.publish(evento)
                .andWaitForStateChange(() -> ordenes.existsByItemId(ITEM), existe -> existe)
                .andVerify(existe -> assertThat(existe).isTrue());

        scenario.publish(evento)
                .andWaitForStateChange(() -> completadas.findAll().size(), c -> c >= previas + 2)
                .andVerify(c -> {
                    assertThat(ordenes.findAll()).hasSize(1);
                    assertThat(publicados.ofType(OrdenGenerada.class).matching(e -> ITEM.equals(e.itemId())))
                            .hasSize(1);
                });
    }

    @Test
    void compraActualizadaCambiaElPrecioTotalYElMontoDeEnvioDeLaOrden(Scenario scenario) {
        scenario.publish(compra())
                .andWaitForStateChange(() -> ordenes.existsByItemId(ITEM), existe -> existe)
                .andVerify(existe -> assertThat(existe).isTrue());

        scenario.publish(new CompraActualizada(UUID.randomUUID(), Instant.now(), ITEM, 10L,
                        new BigDecimal("1200.00"), new BigDecimal("1250.00"), null, new BigDecimal("80.00"),
                        Moneda.ARS, 1L))
                .andWaitForStateChange(() -> ordenes.findByItemId(ITEM).orElseThrow().getMontoEnvio(),
                        envio -> envio != null && envio.signum() > 0)
                .andVerify(envio -> {
                    var orden = ordenes.findByItemId(ITEM).orElseThrow();
                    assertThat(orden.getPrecioTotal()).isEqualByComparingTo("1250.00");
                    assertThat(orden.getMontoEnvio()).isEqualByComparingTo("80.00");
                });
    }
}
