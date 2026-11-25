package com.Sistem.Solicitude_Compra.reportes.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.reportes.dominio.Agrupacion;
import com.Sistem.Solicitude_Compra.reportes.servicio.GastoAgrupado;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteGastoService;
import com.Sistem.Solicitude_Compra.reportes.servicio.ReporteTiemposService;
import com.Sistem.Solicitude_Compra.reportes.servicio.TiemposResolucion;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ReporteController.class)
@Import({SeguridadConfig.class, EntradaNoAutenticado.class, ManejadorAccesoDenegado.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class ReporteControllerTest {

    static final String URL = "/api/v1/reportes/gasto";

    @Autowired MockMvc mockMvc;
    @MockitoBean ReporteGastoService service;
    @MockitoBean ReporteTiemposService tiempos;

    private static RequestPostProcessor como(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void elEncargadoRecibeElGastoConTotalYEnvioSeparados() throws Exception {
        when(service.gasto(Agrupacion.SECTOR, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
                .thenReturn(List.of(new GastoAgrupado(1L, "Sistemas", Moneda.ARS, new BigDecimal("1500.00"),
                        new BigDecimal("50.00"), 2)));

        mockMvc.perform(get(URL + "?agrupar=sector&desde=2026-11-01&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clave").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Sistemas"))
                .andExpect(jsonPath("$[0].moneda").value("ARS"))
                .andExpect(jsonPath("$[0].total").value(1500.00))
                .andExpect(jsonPath("$[0].envio").value(50.00))
                .andExpect(jsonPath("$[0].cantidadOrdenes").value(2));
    }

    @Test
    void elAdminTambienPuedeVerElReporte() throws Exception {
        when(service.gasto(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get(URL + "?agrupar=proveedor&desde=2026-11-01&hasta=2026-11-30").with(como("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void elSolicitanteRecibe403() throws Exception {
        mockMvc.perform(get(URL + "?agrupar=sector&desde=2026-11-01&hasta=2026-11-30").with(como("SOLICITANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        verifyNoInteractions(service);
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mockMvc.perform(get(URL + "?agrupar=sector&desde=2026-11-01&hasta=2026-11-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void faltarUnaFechaEsUn400() throws Exception {
        mockMvc.perform(get(URL + "?agrupar=sector&desde=2026-11-01").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "?agrupar=sector&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaFechaConFormatoInvalidoEsUn400() throws Exception {
        mockMvc.perform(get(URL + "?agrupar=sector&desde=ayer&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agruparPorAlgoInvalidoEsUn400() throws Exception {
        mockMvc.perform(get(URL + "?agrupar=usuario&desde=2026-11-01&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
    }

    @Test
    void desdePosteriorAHastaEsUn400() throws Exception {
        when(service.gasto(any(), any(), any()))
                .thenThrow(new ParametroInvalidoException("La fecha desde no puede ser posterior a la fecha hasta."));

        mockMvc.perform(get(URL + "?agrupar=sector&desde=2026-12-01&hasta=2026-11-30").with(como("ENCARGADO")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PARAMETRO_INVALIDO"));
        verify(service).gasto(Agrupacion.SECTOR, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 30));
    }

    @Test
    void tiemposDeResolucionDevuelveElTotalYElDesglosePorUrgencia() throws Exception {
        when(tiempos.tiempos(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30))).thenReturn(
                new TiemposResolucion(2, 15.0, Map.of("ALTA", new TiemposResolucion.PorUrgencia(2, 15.0),
                        "MEDIA", new TiemposResolucion.PorUrgencia(0, null))));

        mockMvc.perform(get("/api/v1/reportes/tiempos-resolucion?desde=2026-11-01&hasta=2026-11-30")
                        .with(como("ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadItems").value(2))
                .andExpect(jsonPath("$.promedioHoras").value(15.0))
                .andExpect(jsonPath("$.porUrgencia.ALTA.promedioHoras").value(15.0))
                .andExpect(jsonPath("$.porUrgencia.MEDIA.cantidad").value(0))
                .andExpect(jsonPath("$.porUrgencia.MEDIA.promedioHoras").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void tiemposDeResolucionNoEstaAbiertoAlSolicitante() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/tiempos-resolucion?desde=2026-11-01&hasta=2026-11-30")
                        .with(como("SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tiemposDeResolucionSinFechasEs400() throws Exception {
        mockMvc.perform(get("/api/v1/reportes/tiempos-resolucion").with(como("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
