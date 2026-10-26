package com.Sistem.Solicitude_Compra.solicitudes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(SolicitudController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class SolicitudControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SolicitudService service;
    @MockitoBean UsuarioActual usuarioActual;

    private static RequestPostProcessor solicitante() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"));
    }

    private static String manana() {
        return LocalDate.now().plusDays(1).toString();
    }

    private static String item(String extra) {
        return "{\"tipo\":\"PRODUCTO\",\"categoriaId\":42,\"cantidad\":2,\"detalle\":\"Resmas\","
                + "\"precioEstimado\":1000,\"monedaEstimada\":\"ARS\"" + extra + "}";
    }

    private static String body(String fecha, String items) {
        return "{\"urgencia\":\"ALTA\",\"fechaNecesaria\":\"" + fecha + "\",\"observaciones\":\"x\",\"items\":" + items + "}";
    }

    private String crear(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/solicitudes").with(solicitante())
                .contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse().getContentAsString();
    }

    @Test
    void crearDevuelve201ConLocationYBody() throws Exception {
        Solicitud creada = new Solicitud();
        creada.setId(1587L);
        Item i = new Item();
        i.setId(4021L);
        creada.agregarItem(i);
        when(usuarioActual.id()).thenReturn(7L);
        when(service.crear(eq(7L), any())).thenReturn(creada);

        mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(manana(), "[" + item("") + "]")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/solicitudes/1587"))
                .andExpect(jsonPath("$.id").value(1587))
                .andExpect(jsonPath("$.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.items[0].id").value(4021))
                .andExpect(jsonPath("$.items[0].estado").value("PENDIENTE"));
    }

    @Test
    void sinItemsDa400() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(manana(), "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
        verify(service, never()).crear(any(), any());
    }

    @Test
    void cantidadCeroDa400() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(manana(), "[" + item("").replace("\"cantidad\":2", "\"cantidad\":0") + "]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinPrecioEstimadoODePrecioCeroDa400() throws Exception {
        String sinPrecio = item("").replace("\"precioEstimado\":1000,", "");
        String precioCero = item("").replace("\"precioEstimado\":1000", "\"precioEstimado\":0");
        for (String it : List.of(sinPrecio, precioCero)) {
            mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(manana(), "[" + it + "]")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void sinCategoriaOConLasDosDa400() throws Exception {
        String ninguna = item("").replace("\"categoriaId\":42,", "");
        String ambas = item(",\"categoriaNueva\":\"Globos\"");
        for (String it : List.of(ninguna, ambas)) {
            mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                            .content(body(manana(), "[" + it + "]")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void unaFechaPasadaDa400() throws Exception {
        mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(LocalDate.now().minusDays(1).toString(), "[" + item("") + "]")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaCategoriaInexistenteDa422() throws Exception {
        when(usuarioActual.id()).thenReturn(7L);
        when(service.crear(eq(7L), any())).thenThrow(
                new ReglaNegocioException("CATEGORIA_INEXISTENTE", "La categoría 42 no existe"));

        mockMvc.perform(post("/api/v1/solicitudes").with(solicitante()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(manana(), "[" + item("") + "]")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INEXISTENTE"));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitudes")).andExpect(status().isUnauthorized());
    }

    @Test
    void listarDevuelveLaPagina() throws Exception {
        when(usuarioActual.id()).thenReturn(7L);
        when(usuarioActual.rol()).thenReturn(Rol.SOLICITANTE);
        when(service.listar(eq(7L), eq(Rol.SOLICITANTE), eq(true), eq(null), any())).thenReturn(new PageImpl<>(
                List.of(new SolicitudResumenResponse(1L, null, Urgencia.ALTA, LocalDate.now(),
                        EstadoSolicitud.EN_REVISION, 2))));

        mockMvc.perform(get("/api/v1/solicitudes").with(solicitante()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].cantidadItems").value(2))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void verLaSolicitudDeOtroDa403ConSinPermiso() throws Exception {
        when(usuarioActual.id()).thenReturn(7L);
        when(usuarioActual.rol()).thenReturn(Rol.SOLICITANTE);
        when(service.obtener(1L, 7L, Rol.SOLICITANTE)).thenThrow(new SinPermisoException("No es tuya"));

        mockMvc.perform(get("/api/v1/solicitudes/1").with(solicitante()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }
}
