package com.Sistem.Solicitude_Compra.compras.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.catalogo.ProveedorResumen;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.eventos.CompraRegistrada;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.compras.web.CargarCotizacionRequest;
import com.Sistem.Solicitude_Compra.compras.web.ComprarRequest;
import com.Sistem.Solicitude_Compra.compras.web.EditarCompraRequest;
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
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * El camino del ejemplo de Laura dentro de Compras, contra Postgres real: tomar, cotizar, elegir, esperar la
 * aprobación (evento), comprar y editar la compra. Sin @Transactional: cada operación es su propia transacción.
 */
@ApplicationModuleTest
@Import(TestcontainersConfiguration.class)
class GestionComprasFlujoTest {

    private static final Long ITEM = 100L;
    private static final Long YO = 1L;
    private static final LocalDate HOY = LocalDate.now(Cotizacion.ZONA);

    @BeforeAll
    static void configurarZonaHoraria() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired GestionComprasService service;
    @Autowired GestionItemRepository gestiones;
    @Autowired CotizacionRepository cotizaciones;
    @MockitoBean AuthApi authApi;
    @MockitoBean CatalogoApi catalogoApi;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    @AfterEach
    void limpiar() {
        cotizaciones.deleteAll();
        gestiones.deleteAll();
    }

    private void guardarPendiente() {
        gestiones.saveAndFlush(new GestionItem(ITEM, 10L, 2L, 5L, TipoItem.PRODUCTO, "Resma A4",
                new BigDecimal("3"), Urgencia.ALTA, HOY.plusDays(5), OffsetDateTime.now(),
                new BigDecimal("1500.00"), Moneda.ARS));
        when(catalogoApi.obtenerProveedor(9L)).thenReturn(new ProveedorResumen(9L, "Papelera SA", true));
    }

    private Cotizacion cotizar(String unitario) {
        return service.cargarCotizacion(ITEM, new CargarCotizacionRequest(9L, Moneda.ARS, new BigDecimal(unitario),
                HOY.plusDays(7), null, null), YO);
    }

    @Test
    void elIndiceUnicoYElOrdenDeEleccionDejanUnaSolaSeleccionada() {
        guardarPendiente();
        service.tomar(ITEM, YO);
        Cotizacion a = cotizar("400.00");
        Cotizacion b = cotizar("450.00");
        Long version = gestiones.findById(ITEM).orElseThrow().getVersion();

        service.elegirCotizacion(ITEM, a.getId(), version, YO);
        version = gestiones.findById(ITEM).orElseThrow().getVersion();
        service.elegirCotizacion(ITEM, b.getId(), version, YO);

        assertThat(cotizaciones.findById(a.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCotizacion.CANDIDATA);
        assertThat(cotizaciones.findById(b.getId()).orElseThrow().getEstado()).isEqualTo(EstadoCotizacion.SELECCIONADA);
        GestionItem g = gestiones.findById(ITEM).orElseThrow();
        assertThat(g.getCotizacionElegidaId()).isEqualTo(b.getId());
        assertThat(g.getPrecioTotal()).isEqualByComparingTo("1350.00"); // 450.00 × cantidad 3
    }

    @Test
    void laBaseNoDejaComprarSinLaCadenaAprobada() {
        guardarPendiente();
        service.tomar(ITEM, YO);
        Cotizacion a = cotizar("400.00");
        service.elegirCotizacion(ITEM, a.getId(), gestiones.findById(ITEM).orElseThrow().getVersion(), YO);

        Long version = gestiones.findById(ITEM).orElseThrow().getVersion();
        assertThatThrownBy(() -> service.comprar(ITEM, new ComprarRequest(null, version), YO))
                .hasMessageContaining("aprobaciones");
        assertThat(gestiones.findById(ITEM).orElseThrow().getEstado()).isEqualTo(EstadoGestion.EN_COTIZACION);
    }

    @Test
    void flujoCompletoDeLaCotizacionALaCompraYSuEdicion(Scenario scenario) {
        guardarPendiente();
        service.tomar(ITEM, YO);
        Cotizacion elegida = cotizar("400.00");
        Cotizacion otra = cotizar("500.00");
        service.elegirCotizacion(ITEM, elegida.getId(), gestiones.findById(ITEM).orElseThrow().getVersion(), YO);

        // Aprobaciones avisa que la cadena se completó: recién ahí aparece en "Para comprar".
        scenario.publish(new AprobacionCompletada(UUID.randomUUID(), Instant.now(), ITEM, 10L, false))
                .andWaitForStateChange(() -> gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion(),
                        estado -> estado == EstadoAprobacion.APROBADA)
                .andVerify(estado -> assertThat(estado).isEqualTo(EstadoAprobacion.APROBADA));

        scenario.stimulate(() -> service.comprar(ITEM,
                        new ComprarRequest(null, gestiones.findById(ITEM).orElseThrow().getVersion()), YO))
                .andWaitForEventOfType(CompraRegistrada.class)
                .toArriveAndVerify(compra -> {
                    assertThat(compra.itemId()).isEqualTo(ITEM);
                    assertThat(compra.proveedorRazonSocial()).isEqualTo("Papelera SA");
                    assertThat(compra.precioTotal()).isEqualByComparingTo("1200.00");
                    assertThat(compra.cotizacionId()).isEqualTo(elegida.getId());
                    GestionItem g = gestiones.findById(ITEM).orElseThrow();
                    assertThat(g.getEstado()).isEqualTo(EstadoGestion.COMPRADO);
                    assertThat(g.getProveedorCompraId()).isEqualTo(9L);
                    assertThat(cotizaciones.findById(otra.getId()).orElseThrow().getEstado())
                            .isEqualTo(EstadoCotizacion.DESCARTADA);
                    assertThat(cotizaciones.findById(elegida.getId()).orElseThrow().getEstado())
                            .isEqualTo(EstadoCotizacion.SELECCIONADA);
                });

        service.editarCompra(ITEM, new EditarCompraRequest(new BigDecimal("1250.00"), new BigDecimal("80.00"),
                gestiones.findById(ITEM).orElseThrow().getVersion()), YO);
        GestionItem editada = gestiones.findById(ITEM).orElseThrow();
        assertThat(editada.getPrecioTotal()).isEqualByComparingTo("1250.00");
        assertThat(editada.getMontoEnvio()).isEqualByComparingTo("80.00");
    }

    @Test
    void dosEncargadosQueTomanElMismoItemUnoSoloLoConsigue() {
        guardarPendiente();
        service.tomar(ITEM, YO);
        assertThatThrownBy(() -> service.tomar(ITEM, 2L)).hasMessageContaining("otro Encargado");
        assertThat(gestiones.findById(ITEM).orElseThrow().getEncargadoId()).isEqualTo(YO);

        service.reasignar(ITEM, 2L);
        assertThat(gestiones.findById(ITEM).orElseThrow().getEncargadoId()).isEqualTo(2L);
    }
}
