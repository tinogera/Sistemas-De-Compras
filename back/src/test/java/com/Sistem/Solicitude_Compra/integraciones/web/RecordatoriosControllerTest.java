package com.Sistem.Solicitude_Compra.integraciones.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.ComprasApi;
import com.Sistem.Solicitude_Compra.compras.RecordatorioPendiente;
import com.Sistem.Solicitude_Compra.integraciones.servicio.SeguridadIntegracion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecordatoriosController.class)
@Import({SeguridadConfig.class, SeguridadIntegracion.class, EntradaNoAutenticado.class,
        ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8",
        "app.n8n.api-key=clave-de-n8n"})
class RecordatoriosControllerTest {

    private static final String URL = "/api/v1/integraciones/recordatorios-pendientes";

    @Autowired MockMvc mockMvc;
    @Autowired JwtEncoder encoder;
    @MockitoBean ComprasApi compras;
    @MockitoBean AuthApi auth;

    @Test
    void sinApiKeyEs401() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void conApiKeyIncorrectaEs401() throws Exception {
        mockMvc.perform(get(URL).header("X-API-Key", "otra")).andExpect(status().isUnauthorized());
    }

    @Test
    void unJwtDeEncargadoSinApiKeyEs401() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + jwtDeEncargado()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conLaApiKeyCorrectaEs200ConLosDestinatarios() throws Exception {
        when(auth.emailsEncargadosActivos()).thenReturn(List.of("a@x.com", "b@x.com"));
        when(auth.obtenerUsuario(7L)).thenReturn(new UsuarioResumen(7L, "Ana", "ana@x.com", Rol.ENCARGADO, 1L, true));
        when(compras.recordatoriosPendientes(any(Instant.class))).thenReturn(List.of(
                new RecordatorioPendiente(1L, 10L, "Sin tomar", Urgencia.ALTA, LocalDate.of(2026, 12, 1), 4, null),
                new RecordatorioPendiente(2L, 10L, "Tomado", Urgencia.MEDIA, null, 30, 7L)));

        mockMvc.perform(get(URL).header("X-API-Key", "clave-de-n8n"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].destinatarios.length()").value(2))
                .andExpect(jsonPath("$[0].urgencia").value("ALTA"))
                .andExpect(jsonPath("$[1].destinatarios[0]").value("ana@x.com"));
    }

    @Test
    void marcarEnviadoRegistraElRecordatorioYDevuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/integraciones/recordatorios/5/enviado").header("X-API-Key", "clave-de-n8n"))
                .andExpect(status().isNoContent());

        verify(compras).registrarRecordatorio(eq(5L), any(Instant.class));
    }

    @Test
    void laApiKeyNoSirveEnElRestoDeLaApi() throws Exception {
        mockMvc.perform(get("/api/v1/historial").header("X-API-Key", "clave-de-n8n"))
                .andExpect(status().isUnauthorized());
    }

    private String jwtDeEncargado() {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("1").claim("rol", "ENCARGADO")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS384).build(), claims))
                .getTokenValue();
    }
}
