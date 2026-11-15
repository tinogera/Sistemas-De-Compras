package com.Sistem.Solicitude_Compra.catalogo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.catalogo.dominio.EstadoCategoria;
import com.Sistem.Solicitude_Compra.catalogo.servicio.CategoriaService;
import com.Sistem.Solicitude_Compra.compartido.errores.ReglaNegocioException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoriaController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class CategoriaControllerPermisosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CategoriaService categorias;
    @MockitoBean UsuarioActual usuarioActual;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void cualquierUsuarioLogueadoPuedeListarCategorias() throws Exception {
        mockMvc.perform(get("/api/v1/categorias").with(como("SOLICITANTE")))
                .andExpect(status().isOk());
    }

    @Test
    void soloElEncargadoPuedeConfirmarUnaCategoria() throws Exception {
        mockMvc.perform(patch("/api/v1/categorias/1").with(como("SOLICITANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONOCIDA\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        verifyNoInteractions(categorias);
    }

    @Test
    void elEncargadoConfirmaYRecibeLaCategoria() throws Exception {
        when(categorias.confirmar(eq(1L), any())).thenReturn(
                new CategoriaResponse(1L, "Globos", TipoItem.PRODUCTO, EstadoCategoria.CONOCIDA));
        mockMvc.perform(patch("/api/v1/categorias/1").with(como("ENCARGADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONOCIDA\",\"nombre\":\"Globos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONOCIDA"))
                .andExpect(jsonPath("$.nombre").value("Globos"));
        verify(categorias).confirmar(eq(1L), eq(new ActualizarCategoriaRequest(EstadoCategoria.CONOCIDA, "Globos")));
    }

    @Test
    void sinEstadoEnElBodyDa400() throws Exception {
        mockMvc.perform(patch("/api/v1/categorias/1").with(como("ENCARGADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(categorias);
    }

    @Test
    void transicionInvalidaSeTraduceA422() throws Exception {
        when(categorias.confirmar(eq(1L), any()))
                .thenThrow(new ReglaNegocioException("TRANSICION_INVALIDA", "ya está confirmada"));
        mockMvc.perform(patch("/api/v1/categorias/1").with(como("ENCARGADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"CONOCIDA\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TRANSICION_INVALIDA"));
    }

    @Test
    void soloElEncargadoPuedeUnificar() throws Exception {
        mockMvc.perform(post("/api/v1/categorias/1/unificar").with(como("SOLICITANTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinoId\":2}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(categorias);
    }

    @Test
    void unificarUsaElUsuarioLogueadoYDevuelve200() throws Exception {
        when(usuarioActual.id()).thenReturn(42L);
        when(categorias.unificar(1L, 2L, 42L)).thenReturn(
                new CategoriaResponse(1L, "Globo", TipoItem.PRODUCTO, EstadoCategoria.UNIFICADA));
        mockMvc.perform(post("/api/v1/categorias/1/unificar").with(como("ENCARGADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"destinoId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("UNIFICADA"));
        verify(categorias).unificar(1L, 2L, 42L);
    }

    @Test
    void unificarSinDestinoDa400() throws Exception {
        mockMvc.perform(post("/api/v1/categorias/1/unificar").with(como("ENCARGADO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
