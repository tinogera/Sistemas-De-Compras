package com.Sistem.Solicitude_Compra.aprobaciones;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.OperadorCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoAprobador;
import com.Sistem.Solicitude_Compra.aprobaciones.dominio.Enums.TipoCondicion;
import com.Sistem.Solicitude_Compra.aprobaciones.servicios.CadenaAprobacionService;
import com.Sistem.Solicitude_Compra.aprobaciones.servicios.ReglaAdminService;
import com.Sistem.Solicitude_Compra.aprobaciones.web.ReglaRequest;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.auth.UsuarioResumen;
import com.Sistem.Solicitude_Compra.catalogo.CatalogoApi;
import com.Sistem.Solicitude_Compra.compartido.eventos.SolicitudCreada;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** T23.6 y T23.7: lo que ve cada aprobador y el simulador de reglas, con la base real. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class ConsultaAprobacionIntegracionTest {

    private static final Long SECTOR_GG = 5L;
    private static final Long SOLICITANTE = 100L;
    private static final long ITEM = 9101L;

    @Autowired MockMvc mockMvc;
    @Autowired CadenaAprobacionService cadena;
    @Autowired ReglaAdminService reglas;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AuthApi auth;
    @MockitoBean CatalogoApi catalogo;

    @BeforeEach
    void preparar() {
        limpiar();
        when(auth.nombreSector(any())).thenReturn("Gerencia General");
        when(auth.existeSector(SECTOR_GG)).thenReturn(true);
        when(auth.hayUsuariosActivosConRol(Rol.SUPERVISOR)).thenReturn(true);
        when(auth.hayUsuariosActivosEnSector(SECTOR_GG)).thenReturn(true);
        when(auth.obtenerUsuario(any()))
                .thenReturn(new UsuarioResumen(1L, "Persona", "p@x.com", Rol.SUPERVISOR, 1L, true));
        when(catalogo.nombreCategoria(any())).thenReturn("Informática");
        reglas.crear(new ReglaRequest("Compras grandes", 10, true,
                List.of(new ReglaRequest.CondicionRequest(TipoCondicion.MONTO, OperadorCondicion.MAYOR,
                        new BigDecimal("500000"), Moneda.ARS)),
                List.of(new ReglaRequest.PasoRequest(1, TipoAprobador.ROL, Rol.SUPERVISOR, null),
                        new ReglaRequest.PasoRequest(2, TipoAprobador.SECTOR, null, SECTOR_GG)),
                null), 1L);
        var item = new SolicitudCreada.ItemCreado(ITEM, TipoItem.PRODUCTO, 3L, BigDecimal.ONE, "Equipo",
                new BigDecimal("600000"), Moneda.ARS);
        cadena.iniciar(new SolicitudCreada(UUID.randomUUID(), Instant.now(), 8101L, SOLICITANTE, 1L, Urgencia.ALTA,
                LocalDate.now().plusDays(5), List.of(item)));
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM aprobaciones.paso_item WHERE item_id >= 9000");
        jdbc.update("DELETE FROM aprobaciones.aprobacion_item WHERE item_id >= 9000");
        jdbc.update("DELETE FROM aprobaciones.regla_condicion WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla_paso WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla WHERE NOT por_defecto");
    }

    private static RequestPostProcessor como(Long id, String rol, long sector) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("rol", rol).claim("sectorId", sector))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void elSupervisorVeElPendienteYGerenciaSoloLoVeEnCurso() throws Exception {
        mockMvc.perform(get("/api/v1/aprobaciones?vista=para-decidir").with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.itemId == 9101)].puedoDecidir").value(true))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 9101)].aprobadorNombre").value("Supervisor"));
        mockMvc.perform(get("/api/v1/aprobaciones?vista=para-decidir").with(como(300L, "SOLICITANTE", SECTOR_GG)))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 9101)]").isEmpty());
        mockMvc.perform(get("/api/v1/aprobaciones?vista=en-curso").with(como(300L, "SOLICITANTE", SECTOR_GG)))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 9101)].puedoDecidir").value(false));
    }

    @Test
    void elSolicitanteNuncaTieneSuPropioPedidoParaDecidir() throws Exception {
        mockMvc.perform(get("/api/v1/aprobaciones?vista=para-decidir").with(como(SOLICITANTE, "SUPERVISOR", 1)))
                .andExpect(jsonPath("$.contenido[?(@.itemId == 9101)]").isEmpty());
    }

    @Test
    void elResumenCuentaLosPendientesYMarcaSiEsAprobador() throws Exception {
        mockMvc.perform(get("/api/v1/aprobaciones/resumen").with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.esAprobador").value(true))
                .andExpect(jsonPath("$.paraDecidir").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
        mockMvc.perform(get("/api/v1/aprobaciones/resumen").with(como(400L, "ENCARGADO", 2)))
                .andExpect(jsonPath("$.esAprobador").value(false)).andExpect(jsonPath("$.paraDecidir").value(0));
    }

    @Test
    void elDetalleMuestraLosPasosYRespetaLosPermisos() throws Exception {
        mockMvc.perform(get("/api/v1/aprobaciones/items/{id}", ITEM).with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pasos.length()").value(2))
                .andExpect(jsonPath("$.pasos[0].activo").value(true))
                .andExpect(jsonPath("$.pasos[0].puedoDecidir").value(true))
                .andExpect(jsonPath("$.pasos[1].activo").value(false));
        mockMvc.perform(get("/api/v1/aprobaciones/items/{id}", ITEM).with(como(SOLICITANTE, "SOLICITANTE", 1)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/aprobaciones/items/{id}", ITEM).with(como(500L, "SOLICITANTE", 7)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/aprobaciones/items/{id}", 99999).with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void laVistaTodasEsSoloDelAdminYUnaVistaRaraEs400() throws Exception {
        mockMvc.perform(get("/api/v1/aprobaciones?vista=todas").with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/aprobaciones?vista=todas").with(como(1L, "ADMIN", 1)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/aprobaciones?vista=rara").with(como(200L, "SUPERVISOR", 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simularDevuelveLaReglaYLosPasosSinGuardarNada() throws Exception {
        mockMvc.perform(post("/api/v1/admin/reglas/simular").with(como(1L, "ADMIN", 1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":600000,\"moneda\":\"ARS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.regla.nombre").value("Compras grandes"))
                .andExpect(jsonPath("$.automatica").value(false)).andExpect(jsonPath("$.pasos.length()").value(2))
                .andExpect(jsonPath("$.pasos[1].aprobadorNombre").value("Gerencia General"));
        mockMvc.perform(post("/api/v1/admin/reglas/simular").with(como(1L, "ADMIN", 1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":30000,\"moneda\":\"ARS\"}"))
                .andExpect(jsonPath("$.regla.nombre").value("Por defecto"))
                .andExpect(jsonPath("$.pasos.length()").value(1));
        mockMvc.perform(post("/api/v1/admin/reglas/simular").with(como(200L, "SUPERVISOR", 1))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1,\"moneda\":\"ARS\"}"))
                .andExpect(status().isForbidden());
    }
}
