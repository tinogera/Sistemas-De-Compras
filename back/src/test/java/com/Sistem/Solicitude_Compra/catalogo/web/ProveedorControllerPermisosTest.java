package com.Sistem.Solicitude_Compra.catalogo.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.catalogo.servicio.ProveedorService;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;

@WebMvcTest(ProveedorController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class ProveedorControllerPermisosTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ProveedorService proveedores;

    @Test
    void cualquierUsuarioLogueadoPuedeListarProveedores() throws Exception {
        when(proveedores.listar(any(), any(), any(), any()))
                .thenReturn(new Pagina<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/proveedores")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE"))))
                .andExpect(status().isOk());
    }

    @Test
    void soloElEncargadoPuedeEditarUnProveedor() throws Exception {
        mockMvc.perform(patch("/api/v1/proveedores/1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SOLICITANTE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }
}
