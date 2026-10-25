package com.Sistem.Solicitude_Compra.ordenes.web;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.PageImpl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenPdf;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenService;

@WebMvcTest(OrdenController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class, OrdenPdf.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class OrdenControllerPermisosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OrdenService service;
    @MockitoBean UsuarioActual usuarioActual;

    @Test
    void elEncargadoPuedeMarcarUnaOrdenComoEnviada() throws Exception {
        when(usuarioActual.id()).thenReturn(5L);
        when(service.marcarEnviada(any(), any())).thenReturn(orden());

        mockMvc.perform(post("/api/v1/ordenes/1/marcar-enviada")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO"))))
                .andExpect(status().isOk());
    }

    @Test
    void otroRolRecibe403ConSinPermiso() throws Exception {
        mockMvc.perform(post("/api/v1/ordenes/1/marcar-enviada")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mockMvc.perform(post("/api/v1/ordenes/1/marcar-enviada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elEncargadoPuedeListarLasOrdenes() throws Exception {
        when(service.listar(any(), any())).thenReturn(new PageImpl<>(List.of(orden())));

        mockMvc.perform(get("/api/v1/ordenes?estado=GENERADA")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void elEncargadoDescargaElPdfDeUnaOrden() throws Exception {
        when(service.buscar(1L)).thenReturn(orden());

        mockMvc.perform(get("/api/v1/ordenes/1/pdf")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ENCARGADO"))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("orden-1.pdf")));
    }

    @Test
    void otroRolNoPuedeDescargarElPdf() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes/1/pdf")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void otroRolNoPuedeListarLasOrdenes() throws Exception {
        mockMvc.perform(get("/api/v1/ordenes")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    private static OrdenPedido orden() {
        return new OrdenPedido(1L, 10L, 20L, 30L, "Papelera SA", null, TipoItem.PRODUCTO,
                "Lapiceras", new BigDecimal("20"), Moneda.ARS, new BigDecimal("1000.00"));
    }
}
