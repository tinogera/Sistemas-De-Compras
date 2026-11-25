package com.Sistem.Solicitude_Compra.reportes.web;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.auth.servicio.EntradaNoAutenticado;
import com.Sistem.Solicitude_Compra.auth.servicio.ManejadorAccesoDenegado;
import com.Sistem.Solicitude_Compra.auth.servicio.SeguridadConfig;
import com.Sistem.Solicitude_Compra.compartido.errores.ParametroInvalidoException;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteTiemposService;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion.TiempoUrgencia;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(TiemposResolucionController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class TiemposResolucionControllerTest {

    static final String URL = "/api/v1/reportes/tiempos-resolucion";

    @Autowired MockMvc mockMvc;
    @MockitoBean ReporteTiemposService service;

    private static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static TiemposResolucion resultado() {
        Map<Urgencia, TiempoUrgencia> porUrgencia = new LinkedHashMap<>();
        porUrgencia.put(Urgencia.ALTA, new TiempoUrgencia(2, 15.0));
        porUrgencia.put(Urgencia.MEDIA, new TiempoUrgencia(0, null));
        porUrgencia.put(Urgencia.BAJA, new TiempoUrgencia(1, 48.5));
        return new TiemposResolucion(3, 26.2, porUrgencia);
    }

    @Test
    void elEncargadoRecibeElPromedioGeneralYPorUrgencia() throws Exception {
        when(service.tiempos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(resultado());

        mockMvc.perform(get(URL + "?desde=2026-11-01&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadItems").value(3))
                .andExpect(jsonPath("$.promedioHoras").value(26.2))
                .andExpect(jsonPath("$.porUrgencia.ALTA.cantidad").value(2))
                .andExpect(jsonPath("$.porUrgencia.ALTA.promedioHoras").value(15.0))
                .andExpect(jsonPath("$.porUrgencia.MEDIA.cantidad").value(0))
                .andExpect(jsonPath("$.porUrgencia.MEDIA.promedioHoras").value(nullValue()))
                .andExpect(jsonPath("$.porUrgencia.BAJA.promedioHoras").value(48.5));
    }

    @Test
    void elAdminTambienPuedeVerElReporte() throws Exception {
        when(service.tiempos(any(), any())).thenReturn(resultado());

        mockMvc.perform(get(URL + "?desde=2026-11-01&hasta=2026-11-30").with(como("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void elSolicitanteRecibe403() throws Exception {
        mockMvc.perform(get(URL + "?desde=2026-11-01&hasta=2026-11-30").with(como("SOLICITANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        verifyNoInteractions(service);
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mockMvc.perform(get(URL + "?desde=2026-11-01&hasta=2026-11-30")).andExpect(status().isUnauthorized());
    }

    @Test
    void faltarUnaFechaOCargarUnaInvalidaEsUn400() throws Exception {
        mockMvc.perform(get(URL + "?desde=2026-11-01").with(como("ENCARGADO"))).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?hasta=2026-11-30").with(como("ENCARGADO"))).andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?desde=ayer&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desdePosteriorAHastaEsUn400() throws Exception {
        when(service.tiempos(any(), any()))
                .thenThrow(new ParametroInvalidoException("La fecha desde no puede ser posterior a la fecha hasta."));

        mockMvc.perform(get(URL + "?desde=2026-12-01&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }
}
