package com.Sistem.Solicitude_Compra.compras.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.servicio.BandejaComprasService;
import com.Sistem.Solicitude_Compra.compras.servicio.GestionComprasService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.util.ReflectionTestUtils;

@WebMvcTest(BandejaComprasController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class BandejaComprasAccionesControllerTest {

    private static final String RUTA = "/api/v1/bandeja/100";

    @Autowired MockMvc mockMvc;
    @MockitoBean BandejaComprasService bandeja;
    @MockitoBean GestionComprasService gestion;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    void usuario() {
        when(usuarioActual.id()).thenReturn(77L);
    }

    private static RequestPostProcessor encargado() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO"));
    }

    private static RequestPostProcessor solicitante() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"));
    }

    private ResultActions json(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder metodo,
                               String cuerpo, RequestPostProcessor usuario) throws Exception {
        return mockMvc.perform(metodo.with(usuario).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    // ---- permisos: todo es del Encargado ----

    @Test
    void unSolicitanteNoPuedeOperarLaBandeja() throws Exception {
        mockMvc.perform(post(RUTA + "/tomar").with(solicitante())).andExpect(status().isForbidden());
        mockMvc.perform(post(RUTA + "/reasignar").with(solicitante())).andExpect(status().isForbidden());
        mockMvc.perform(get(RUTA + "/proveedores-sugeridos").with(solicitante())).andExpect(status().isForbidden());
        json(post(RUTA + "/comprar"), "{\"proveedorId\":9,\"version\":1}", solicitante())
                .andExpect(status().isForbidden());
        json(post(RUTA + "/rechazar"), "{\"motivo\":\"x\",\"version\":1}", solicitante())
                .andExpect(status().isForbidden());
        verifyNoInteractions(gestion);
    }

    @Test
    void niElAdminNiElSupervisorPuedenOperarLaBandeja() throws Exception {
        for (String rol : List.of("ADMIN", "SUPERVISOR")) {
            RequestPostProcessor usuario = jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
            mockMvc.perform(post(RUTA + "/tomar").with(usuario))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
            json(post(RUTA + "/comprar"), "{\"proveedorId\":9,\"version\":1}", usuario)
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(gestion);
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(post(RUTA + "/tomar")).andExpect(status().isUnauthorized());
    }

    // ---- T09.4 ----

    @Test
    void tomarYReasignarUsanAlUsuarioLogueado() throws Exception {
        mockMvc.perform(post(RUTA + "/tomar").with(encargado())).andExpect(status().isOk());
        verify(gestion).tomar(100L, 77L);
        mockMvc.perform(post(RUTA + "/reasignar").with(encargado())).andExpect(status().isOk());
        verify(gestion).reasignar(100L, 77L);
    }

    // ---- T10.2 ----

    @Test
    void cargarCotizacionDevuelve201ConLocation() throws Exception {
        Cotizacion creada = new Cotizacion(100L, 9L, "Globos SA", Moneda.ARS, new BigDecimal("10.00"),
                new BigDecimal("30.00"), LocalDate.now().plusDays(5), null, null, 77L, OffsetDateTime.now());
        ReflectionTestUtils.setField(creada, "id", 55L);
        when(gestion.cargarCotizacion(eq(100L), any(), eq(77L))).thenReturn(creada);

        json(post(RUTA + "/cotizaciones"), cotizacionJson(9, "10.00", LocalDate.now().plusDays(5)), encargado())
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith(RUTA + "/cotizaciones/55")))
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.precioTotal").value(30.00))
                .andExpect(jsonPath("$.estado").value("CANDIDATA"))
                .andExpect(jsonPath("$.vigente").value(true));
    }

    @Test
    void cargarCotizacionConDatosInvalidosDa400() throws Exception {
        String fecha = LocalDate.now().plusDays(5).toString();
        json(post(RUTA + "/cotizaciones"), "{\"moneda\":\"ARS\",\"precioUnitario\":10,\"fechaValidez\":\"" + fecha + "\"}",
                encargado()).andExpect(status().isBadRequest());                       // sin proveedor
        json(post(RUTA + "/cotizaciones"), cotizacionJson(9, "0", LocalDate.now()), encargado())
                .andExpect(status().isBadRequest());                                   // precio 0
        json(post(RUTA + "/cotizaciones"), cotizacionJson(9, "-5", LocalDate.now()), encargado())
                .andExpect(status().isBadRequest());                                   // precio negativo
        json(post(RUTA + "/cotizaciones"), cotizacionJson(9, "10.123", LocalDate.now()), encargado())
                .andExpect(status().isBadRequest());                                   // más de 2 decimales
        verifyNoInteractions(gestion);
    }

    // ---- T10.3 ----

    @Test
    void elegirUnaCotizacionExigeLaVersion() throws Exception {
        json(post(RUTA + "/cotizaciones/5/elegir"), "{}", encargado()).andExpect(status().isBadRequest());
        json(post(RUTA + "/cotizaciones/5/elegir"), "{\"version\":4}", encargado()).andExpect(status().isOk());
        verify(gestion).elegirCotizacion(100L, 5L, 4L, 77L);
    }

    @Test
    void elPrecioTotalSeValidaYSeQuitaConDelete() throws Exception {
        json(put(RUTA + "/precio-total"), "{\"precioTotal\":0,\"moneda\":\"ARS\",\"version\":1}", encargado())
                .andExpect(status().isBadRequest());
        json(put(RUTA + "/precio-total"), "{\"precioTotal\":100,\"version\":1}", encargado())
                .andExpect(status().isBadRequest());                                   // sin moneda
        json(put(RUTA + "/precio-total"), "{\"precioTotal\":100.50,\"moneda\":\"USD\",\"version\":1}", encargado())
                .andExpect(status().isOk());

        mockMvc.perform(delete(RUTA + "/precio-total").param("version", "3").with(encargado()))
                .andExpect(status().isNoContent());
        verify(gestion).quitarPrecioTotal(100L, 3L, 77L);
        mockMvc.perform(delete(RUTA + "/precio-total").with(encargado())).andExpect(status().isBadRequest());
    }

    // ---- T10.4 ----

    @Test
    void rechazarSinMotivoDa400() throws Exception {
        json(post(RUTA + "/rechazar"), "{\"motivo\":\"   \",\"version\":1}", encargado())
                .andExpect(status().isBadRequest());
        json(post(RUTA + "/rechazar"), "{\"version\":1}", encargado()).andExpect(status().isBadRequest());
        json(post(RUTA + "/rechazar"), "{\"motivo\":\"No hace falta\",\"version\":1}", encargado())
                .andExpect(status().isOk());
        verify(gestion).rechazar(100L, "No hace falta", 1L, 77L);
    }

    @Test
    void rechazarUnItemCompradoSeTraduceA422() throws Exception {
        org.mockito.Mockito.doThrow(new ReglaNegocioException("GESTION_ESTADO_INVALIDO", "Ya está comprado"))
                .when(gestion).rechazar(any(), any(), any(), any());
        json(post(RUTA + "/rechazar"), "{\"motivo\":\"No\",\"version\":1}", encargado())
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("GESTION_ESTADO_INVALIDO"));
    }

    // ---- T10.5 ----

    @Test
    void proveedoresSugeridosDevuelveLaLista() throws Exception {
        when(bandeja.proveedoresSugeridos(100L)).thenReturn(List.of(new ProveedorSugeridoResponse(9L, "Globos SA")));
        mockMvc.perform(get(RUTA + "/proveedores-sugeridos").with(encargado()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].razonSocial").value("Globos SA"));
    }

    // ---- T10.7 ----

    @Test
    void comprarExigeLaVersionYElProveedorEsOpcional() throws Exception {
        json(post(RUTA + "/comprar"), "{\"proveedorId\":9}", encargado()).andExpect(status().isBadRequest());
        json(post(RUTA + "/comprar"), "{\"version\":2}", encargado()).andExpect(status().isOk());
        verify(gestion).comprar(eq(100L), eq(new ComprarRequest(null, 2L)), eq(77L));
    }

    // ---- T10.8 ----

    @Test
    void editarLaCompraValidaLosImportes() throws Exception {
        json(patch(RUTA + "/compra"), "{\"montoEnvio\":-1,\"version\":1}", encargado())
                .andExpect(status().isBadRequest());
        json(patch(RUTA + "/compra"), "{\"precioTotal\":0,\"version\":1}", encargado())
                .andExpect(status().isBadRequest());
        json(patch(RUTA + "/compra"), "{\"montoEnvio\":40.00}", encargado()).andExpect(status().isBadRequest());
        json(patch(RUTA + "/compra"), "{\"montoEnvio\":40.00,\"version\":1}", encargado())
                .andExpect(status().isOk());
        verify(gestion).editarCompra(eq(100L), eq(new EditarCompraRequest(null, new BigDecimal("40.00"), 1L)), eq(77L));
    }

    private static String cotizacionJson(int proveedor, String precio, LocalDate validez) {
        return "{\"proveedorId\":" + proveedor + ",\"moneda\":\"ARS\",\"precioUnitario\":" + precio
                + ",\"fechaValidez\":\"" + validez + "\"}";
    }
}
