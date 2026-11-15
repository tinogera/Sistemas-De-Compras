package com.Sistem.Solicitude_Compra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.CategoriaRepository;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.compartido.eventos.AprobacionCompletada;
import com.Sistem.Solicitude_Compra.compartido.tipos.EstadoAprobacion;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoGestion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.repositorio.OrdenPedidoRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.when;

/**
 * T10 + T11 de punta a punta, por la API y con todos los módulos reales: el Encargado toma un ítem, cotiza,
 * elige, la cadena se aprueba (evento de Aprobaciones), compra, y Órdenes genera la orden; después edita la
 * compra y la orden se actualiza. Los eventos entre módulos son asíncronos: se espera con Awaitility.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class CompraAOrdenTest {

    private static final Long ITEM = 7001L;
    private static final Long ENCARGADO = 1L;

    @Autowired MockMvc mockMvc;
    @Autowired GestionItemRepository gestiones;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired OrdenPedidoRepository ordenes;
    @Autowired ProveedorRepository proveedores;
    @Autowired CategoriaRepository categorias;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired TransactionTemplate transacciones;
    @MockitoBean AuthApi authApi;

    private Long proveedorId;
    private Long categoriaId;

    @BeforeEach
    void preparar() {
        limpiar();
        when(authApi.obtenerUsuario(ENCARGADO)).thenReturn(
                new UsuarioResumen(ENCARGADO, "Marta Compras", "marta@test.com", Rol.ENCARGADO, 1L, true));

        Categoria categoria = new Categoria();
        categoria.setNombre("Papelería e2e");
        categoria.setTipo(TipoItem.PRODUCTO);
        categoriaId = categorias.saveAndFlush(categoria).getId();
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial("Papelera Central SA");
        proveedorId = proveedores.saveAndFlush(proveedor).getId();

        gestiones.saveAndFlush(new GestionItem(ITEM, 10L, 2L, categoriaId, TipoItem.PRODUCTO, "Resma A4",
                new BigDecimal("3"), Urgencia.ALTA, LocalDate.now().plusDays(5), OffsetDateTime.now(),
                new BigDecimal("1500.00"), Moneda.ARS));
    }

    @AfterEach
    void limpiar() {
        ordenes.deleteAll();
        cotizaciones.deleteAll();
        gestiones.deleteAll();
        proveedores.deleteAll();
        categorias.deleteAll();
    }

    private static RequestPostProcessor encargado() {
        return jwt().jwt(j -> j.subject(ENCARGADO.toString()).claim("rol", "ENCARGADO").claim("sectorId", 1))
                .authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO"));
    }

    private long version() {
        return gestiones.findById(ITEM).orElseThrow().getVersion();
    }

    @Test
    void laCompraGeneraLaOrdenYEditarLaCompraLaActualiza() throws Exception {
        // 1. tomar y cotizar
        mockMvc.perform(post("/api/v1/bandeja/{id}/tomar", ITEM).with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_COTIZACION"))
                .andExpect(jsonPath("$.encargadoNombre").value("Marta Compras"));

        String cotizacion = mockMvc.perform(post("/api/v1/bandeja/{id}/cotizaciones", ITEM).with(encargado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proveedorId\":" + proveedorId + ",\"moneda\":\"ARS\",\"precioUnitario\":400.00,"
                                + "\"fechaValidez\":\"" + LocalDate.now().plusDays(7) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.precioTotal").value(1200.00))
                .andReturn().getResponse().getContentAsString();
        long cotizacionId = Long.parseLong(cotizacion.replaceAll(".*\"id\":(\\d+).*", "$1"));

        // 2. elegirla: queda el precio total del ítem
        mockMvc.perform(post("/api/v1/bandeja/{id}/cotizaciones/{c}/elegir", ITEM, cotizacionId).with(encargado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioTotal").value(1200.00))
                .andExpect(jsonPath("$.cotizacionElegidaId").value(cotizacionId));

        // 3. todavía no se puede comprar: la cadena de aprobación no terminó
        mockMvc.perform(post("/api/v1/bandeja/{id}/comprar", ITEM).with(encargado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version() + "}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("APROBACION_PENDIENTE"));

        // 4. Aprobaciones avisa que se completó: el ítem pasa a "Para comprar"
        transacciones.executeWithoutResult(t -> eventos.publishEvent(
                new AprobacionCompletada(UUID.randomUUID(), Instant.now(), ITEM, 10L, false)));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(gestiones.findById(ITEM).orElseThrow().getEstadoAprobacion())
                        .isEqualTo(EstadoAprobacion.APROBADA));
        mockMvc.perform(get("/api/v1/bandeja").param("paraComprar", "true").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.itemId == " + ITEM + ")]").isNotEmpty());

        // 5. comprar: T10.7 → CompraRegistrada → T11.2 genera la orden
        mockMvc.perform(post("/api/v1/bandeja/{id}/comprar", ITEM).with(encargado())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPRADO"));
        assertThat(gestiones.findById(ITEM).orElseThrow().getEstado()).isEqualTo(EstadoGestion.COMPRADO);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(ordenes.existsByItemId(ITEM)).isTrue());
        OrdenPedido orden = ordenes.findByItemId(ITEM).orElseThrow();
        assertThat(orden.getNumero()).isPositive();
        assertThat(orden.getProveedorId()).isEqualTo(proveedorId);
        assertThat(orden.getProveedorRazonSocial()).isEqualTo("Papelera Central SA");
        assertThat(orden.getPrecioTotal()).isEqualByComparingTo("1200.00");

        // 6. la orden se ve en la API y se descarga en PDF
        mockMvc.perform(get("/api/v1/ordenes").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.numero == " + orden.getNumero() + ")]").isNotEmpty());
        mockMvc.perform(get("/api/v1/ordenes/{id}/pdf", orden.getId()).with(encargado()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 7. T10.8 → CompraActualizada → T11.6: la orden refleja el precio corregido y el envío
        mockMvc.perform(patch("/api/v1/bandeja/{id}/compra", ITEM).with(encargado())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"precioTotal\":1250.00,\"montoEnvio\":80.00,\"version\":" + version() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioTotal").value(1250.00))
                .andExpect(jsonPath("$.montoEnvio").value(80.00));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            OrdenPedido actualizada = ordenes.findByItemId(ITEM).orElseThrow();
            assertThat(actualizada.getPrecioTotal()).isEqualByComparingTo("1250.00");
            assertThat(actualizada.getMontoEnvio()).isEqualByComparingTo("80.00");
        });

        // La cotización elegida se conserva y no queda ninguna otra SELECCIONADA.
        assertThat(cotizaciones.findById(cotizacionId).orElseThrow().getEstado().name()).isEqualTo("SELECCIONADA");
    }
}
