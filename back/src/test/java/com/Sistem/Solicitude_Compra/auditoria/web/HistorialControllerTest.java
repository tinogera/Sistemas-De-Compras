package com.Sistem.Solicitude_Compra.auditoria.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auditoria.dominio.AccionHistorial;
import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.servicio.HistorialService;
import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.errores.SinPermisoException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HistorialController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class HistorialControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean HistorialService service;
    @MockitoBean UsuarioActual usuarioActual;

    @Test
    void devuelveLasEntradasConElFormatoPedido() throws Exception {
        when(usuarioActual.id()).thenReturn(5L);
        when(usuarioActual.rol()).thenReturn(Rol.SOLICITANTE);
        when(service.consultar(eq(EntidadHistorial.SOLICITUD), eq(123L), eq(true), eq(5L), eq(Rol.SOLICITANTE)))
                .thenReturn(List.of(new HistorialResponse(Instant.parse("2026-11-01T10:00:00Z"),
                        EntidadHistorial.SOLICITUD, 123L, AccionHistorial.CREACION, null, "EN_REVISION", "Ana",
                        null)));

        mockMvc.perform(get("/api/v1/historial").param("entidad", "SOLICITUD").param("id", "123")
                        .param("incluirItems", "true").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entidad").value("SOLICITUD"))
                .andExpect(jsonPath("$[0].accion").value("CREACION"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("EN_REVISION"))
                .andExpect(jsonPath("$[0].usuarioNombre").value("Ana"));
    }

    @Test
    void unaEntidadInvalidaEs400() throws Exception {
        mockMvc.perform(get("/api/v1/historial").param("entidad", "PLANETA").param("id", "1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinEntidadEs400() throws Exception {
        mockMvc.perform(get("/api/v1/historial").param("id", "1").with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinPermisoEs403() throws Exception {
        when(usuarioActual.id()).thenReturn(5L);
        when(usuarioActual.rol()).thenReturn(Rol.SOLICITANTE);
        when(service.consultar(eq(EntidadHistorial.SOLICITUD), eq(9L), eq(false), eq(5L), eq(Rol.SOLICITANTE)))
                .thenThrow(new SinPermisoException("No tenés permiso para ver este historial"));

        mockMvc.perform(get("/api/v1/historial").param("entidad", "SOLICITUD").param("id", "9").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void sinTokenEs401() throws Exception {
        mockMvc.perform(get("/api/v1/historial").param("entidad", "SOLICITUD").param("id", "1"))
                .andExpect(status().isUnauthorized());
    }
}
