package com.Sistem.Solicitude_Compra.aprobaciones.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.auth.AuthApi;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** T23.3 por la API completa, con la base real. Los usuarios y sectores los simula AuthApi. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class AdminReglaControllerTest {

    private static final String URL = "/api/v1/admin/reglas";
    private static final String COMPRAS_GRANDES = """
            { "nombre": "Compras grandes", "prioridad": 10, "activa": true,
              "condiciones": [ { "tipo": "MONTO", "operador": "MAYOR", "valor": 500000, "moneda": "ARS" } ],
              "pasos": [ { "orden": 1, "tipoAprobador": "ROL", "rol": "SUPERVISOR" },
                         { "orden": 2, "tipoAprobador": "SECTOR", "sectorId": 5 } ] }
            """;

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AuthApi auth;

    @BeforeEach
    void preparar() {
        when(auth.existeSector(5L)).thenReturn(true);
        when(auth.nombreSector(5L)).thenReturn("Gerencia General");
        when(auth.hayUsuariosActivosConRol(Rol.SUPERVISOR)).thenReturn(true);
        when(auth.hayUsuariosActivosConRol(Rol.ADMIN)).thenReturn(true);
        when(auth.hayUsuariosActivosEnSector(5L)).thenReturn(true);
        restaurar();
    }

    @AfterEach
    void restaurar() {
        // Deja solo la regla por defecto, con su único paso de Supervisor.
        jdbc.update("DELETE FROM aprobaciones.regla_condicion WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla_paso WHERE regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE NOT por_defecto)");
        jdbc.update("DELETE FROM aprobaciones.regla WHERE NOT por_defecto");
        jdbc.update("DELETE FROM aprobaciones.regla_paso WHERE orden > 1 AND regla_id IN "
                + "(SELECT id FROM aprobaciones.regla WHERE por_defecto)");
    }

    private static RequestPostProcessor como(String rol) {
        return jwt().jwt(j -> j.subject("1").claim("rol", rol).claim("sectorId", 1))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private String crear(String cuerpo) throws Exception {
        return mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void creaLaReglaDelEjemploYLaListaPorPrioridad() throws Exception {
        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(COMPRAS_GRANDES))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Compras grandes"))
                .andExpect(jsonPath("$.pasos.length()").value(2))
                .andExpect(jsonPath("$.pasos[1].aprobadorNombre").value("Gerencia General"))
                .andExpect(jsonPath("$.pasos[0].sinAprobadores").value(false))
                .andExpect(jsonPath("$.condiciones[0].valor").value(500000));

        mockMvc.perform(get(URL).with(como("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Compras grandes"))
                .andExpect(jsonPath("$[1].porDefecto").value(true));
    }

    @Test
    void unPasoDeSectorInexistenteEs422() throws Exception {
        when(auth.existeSector(99L)).thenReturn(false);
        String cuerpo = COMPRAS_GRANDES.replace("\"sectorId\": 5", "\"sectorId\": 99");

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SECTOR_INEXISTENTE"));
    }

    @Test
    void unPasoSinUsuariosActivosEs422SinAprobadores() throws Exception {
        when(auth.hayUsuariosActivosEnSector(5L)).thenReturn(false);

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(COMPRAS_GRANDES))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("SIN_APROBADORES"));
    }

    @Test
    void sinAprobadoresSePuedeGuardarUnaReglaInactiva() throws Exception {
        when(auth.hayUsuariosActivosEnSector(5L)).thenReturn(false);

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(COMPRAS_GRANDES.replace("\"activa\": true", "\"activa\": false")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activa").value(false))
                .andExpect(jsonPath("$.pasos[1].sinAprobadores").value(true));
    }

    @Test
    void unRolSolicitanteNoPuedeAprobar() throws Exception {
        String cuerpo = COMPRAS_GRANDES.replace("\"rol\": \"SUPERVISOR\"", "\"rol\": \"SOLICITANTE\"");

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ROL_NO_APROBADOR"));
    }

    @Test
    void elOrdenDeLosPasosNoPuedeTenerSaltos() throws Exception {
        String cuerpo = COMPRAS_GRANDES.replace("\"orden\": 2", "\"orden\": 3");

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ORDEN_INVALIDO"));
    }

    @Test
    void unTipoDeCondicionDesconocidoEs400() throws Exception {
        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(COMPRAS_GRANDES.replace("\"MONTO\"", "\"COLOR\"")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unNombreRepetidoSinDistinguirMayusculasEs409() throws Exception {
        crear(COMPRAS_GRANDES);

        mockMvc.perform(post(URL).with(como("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(COMPRAS_GRANDES.replace("Compras grandes", "COMPRAS GRANDES")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_DUPLICADO"));
    }

    @Test
    void laReglaPorDefectoNoSePuedeDesactivar() throws Exception {
        JsonNode porDefecto = reglaPorDefecto();

        mockMvc.perform(post(URL + "/{id}/desactivar", porDefecto.get("id").asLong()).with(como("ADMIN")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("REGLA_POR_DEFECTO_NO_DESACTIVABLE"));
    }

    @Test
    void laReglaPorDefectoNoPuedeQuedarSinPasosNiTenerCondiciones() throws Exception {
        JsonNode porDefecto = reglaPorDefecto();
        String base = "{ \"nombre\": \"Por defecto\", \"prioridad\": 1000, \"version\": " + porDefecto.get("version");

        mockMvc.perform(put(URL + "/{id}", porDefecto.get("id").asLong()).with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(base + ", \"pasos\": [] }"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("REGLA_POR_DEFECTO_SIN_PASOS"));
    }

    @Test
    void sePuedenEditarLosPasosDeLaReglaPorDefectoASupervisorYGerenciaGeneral() throws Exception {
        JsonNode porDefecto = reglaPorDefecto();
        String cuerpo = "{ \"nombre\": \"Por defecto\", \"prioridad\": 1000, \"version\": " + porDefecto.get("version")
                + ", \"pasos\": [ { \"orden\": 1, \"tipoAprobador\": \"ROL\", \"rol\": \"SUPERVISOR\" },"
                + " { \"orden\": 2, \"tipoAprobador\": \"SECTOR\", \"sectorId\": 5 } ] }";

        mockMvc.perform(put(URL + "/{id}", porDefecto.get("id").asLong()).with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.porDefecto").value(true))
                .andExpect(jsonPath("$.pasos.length()").value(2))
                .andExpect(jsonPath("$.pasos[1].aprobadorNombre").value("Gerencia General"));
    }

    @Test
    void editarConUnaVersionVencidaEs409() throws Exception {
        JsonNode creada = json.readTree(crear(COMPRAS_GRANDES));
        String cuerpo = "{ \"nombre\": \"Compras grandes\", \"prioridad\": 10, \"version\": 99, "
                + "\"pasos\": [ { \"orden\": 1, \"tipoAprobador\": \"ROL\", \"rol\": \"SUPERVISOR\" } ] }";

        mockMvc.perform(put(URL + "/{id}", creada.get("id").asLong()).with(como("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLICTO_VERSION"));
    }

    @Test
    void sePuedeDesactivarYVolverAActivarUnaRegla() throws Exception {
        long id = json.readTree(crear(COMPRAS_GRANDES)).get("id").asLong();

        mockMvc.perform(post(URL + "/{id}/desactivar", id).with(como("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(false));
        mockMvc.perform(post(URL + "/{id}/activar", id).with(como("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activa").value(true));
    }

    @Test
    void unEncargadoRecibe403() throws Exception {
        mockMvc.perform(get(URL).with(como("ENCARGADO"))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(como("ENCARGADO")).contentType(MediaType.APPLICATION_JSON)
                .content(COMPRAS_GRANDES)).andExpect(status().isForbidden());
    }

    private JsonNode reglaPorDefecto() throws Exception {
        String lista = mockMvc.perform(get(URL).with(como("ADMIN"))).andReturn().getResponse().getContentAsString();
        for (JsonNode regla : json.readTree(lista)) {
            if (regla.get("porDefecto").asBoolean()) {
                return regla;
            }
        }
        throw new IllegalStateException("No hay regla por defecto");
    }
}
