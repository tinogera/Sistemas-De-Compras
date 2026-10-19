package com.Sistem.Solicitude_Compra.auth.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.LoginService;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.errores.NoEncontradoException;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.web.ManejadorErrores;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class, ManejadorErrores.class})
@TestPropertySource(properties = {
        "app.jwt.secret=clave-de-prueba-clave-de-prueba-clave-de-prueba-123456",
        "app.jwt.expiracion-horas=8"
})
class AuthControllerIdentificateTest {

    private static final String RUTA = "/api/v1/auth/identificate";

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthApi authApi;
    @MockitoBean LoginService loginService;
    @MockitoBean UsuarioActual usuarioActual;

    @BeforeEach
    void usuarioDelToken() {
        when(usuarioActual.id()).thenReturn(1L);
    }

    @Test
    void conToken_devuelve200ConLosDatos() throws Exception {
        when(authApi.obtenerUsuario(1L))
                .thenReturn(new UsuarioResumen(1L, "Ana", "ana@test.com", Rol.ENCARGADO, 2L, true));
        when(authApi.nombreSector(2L)).thenReturn("Compras");

        mockMvc.perform(get(RUTA).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("ENCARGADO"))
                .andExpect(jsonPath("$.sectorId").value(2))
                .andExpect(jsonPath("$.sectorNombre").value("Compras"));
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get(RUTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioInactivo_devuelve401ConCodigoSesionInvalida() throws Exception {
        when(authApi.obtenerUsuario(1L))
                .thenReturn(new UsuarioResumen(1L, "Ana", "ana@test.com", Rol.ENCARGADO, 2L, false));

        mockMvc.perform(get(RUTA).with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("SESION_INVALIDA"));
    }

    @Test
    void usuarioInexistente_devuelve404() throws Exception {
        when(authApi.obtenerUsuario(1L)).thenThrow(new NoEncontradoException("No existe el usuario 1"));

        mockMvc.perform(get(RUTA).with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }
}
