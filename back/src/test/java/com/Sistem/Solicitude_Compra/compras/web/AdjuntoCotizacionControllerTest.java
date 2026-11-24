package com.Sistem.Solicitude_Compra.compras.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.Sistem.Solicitude_Compra.TestcontainersConfiguration;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import com.Sistem.Solicitude_Compra.catalogo.repositorio.ProveedorRepository;
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.GestionItem;
import com.Sistem.Solicitude_Compra.compras.repositorio.AdjuntoCotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.CotizacionRepository;
import com.Sistem.Solicitude_Compra.compras.repositorio.GestionItemRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** T14.3 por la API completa: el presupuesto del proveedor adjunto a una cotización. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class AdjuntoCotizacionControllerTest {

    private static final Long ITEM = 8101L;
    private static final Long ENCARGADO = 1L;
    private static final Long OTRO_ENCARGADO = 2L;
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);

    private static Path carpeta;

    @DynamicPropertySource
    static void carpetaTemporal(DynamicPropertyRegistry registro) throws IOException {
        carpeta = Files.createTempDirectory("adjuntos-cotizacion-test");
        registro.add("app.adjuntos.dir", carpeta::toString);
    }

    @Autowired MockMvc mockMvc;
    @Autowired GestionItemRepository gestiones;
    @Autowired CotizacionRepository cotizaciones;
    @Autowired ProveedorRepository proveedores;
    @Autowired AdjuntoCotizacionRepository adjuntos;

    private GestionItem gestion;
    private Long cotizacionId;

    @BeforeEach
    void preparar() {
        limpiar();
        Proveedor proveedor = new Proveedor();
        proveedor.setRazonSocial("Papelera Central SA");
        Long proveedorId = proveedores.saveAndFlush(proveedor).getId();

        gestion = new GestionItem(ITEM, 10L, 2L, 5L, TipoItem.PRODUCTO, "Resma A4", new BigDecimal("3"),
                Urgencia.ALTA, LocalDate.now().plusDays(5), OffsetDateTime.now(), new BigDecimal("1500.00"),
                Moneda.ARS);
        gestion.tomar(ENCARGADO);
        gestiones.saveAndFlush(gestion);

        cotizacionId = cotizaciones.saveAndFlush(new Cotizacion(ITEM, proveedorId, "Papelera Central SA", Moneda.ARS,
                new BigDecimal("400.00"), new BigDecimal("1200.00"), LocalDate.now().plusDays(7), null, null,
                ENCARGADO, OffsetDateTime.now())).getId();
    }

    @AfterEach
    void limpiar() {
        cotizaciones.deleteAll(); // borra los adjuntos en cascada
        gestiones.deleteAll();
        proveedores.deleteAll();
    }

    private static RequestPostProcessor como(Long id, String rol) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("rol", rol).claim("sectorId", 1))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }

    private long subirComoEncargado() throws Exception {
        String respuesta = mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId)
                        .file(archivo("presupuesto.pdf", PDF)).with(como(ENCARGADO, "ENCARGADO")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void elEncargadoDelItemSubeUnPdf() throws Exception {
        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId)
                        .file(archivo("presupuesto.pdf", PDF)).with(como(ENCARGADO, "ENCARGADO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("presupuesto.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.tamanoBytes").value(PDF.length));

        var guardado = adjuntos.findByCotizacionIdOrderBySubidoEnAsc(cotizacionId).get(0);
        assertThat(guardado.getSubidoPor()).isEqualTo(ENCARGADO);
        assertThat(guardado.getClaveObjeto()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(carpeta.resolve(guardado.getClaveObjeto()))).isEqualTo(PDF);
    }

    @Test
    void unTxtRenombradoAPdfDa422() throws Exception {
        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId)
                        .file(archivo("presupuesto.pdf", "solo texto".getBytes()))
                        .with(como(ENCARGADO, "ENCARGADO")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"));
        assertThat(adjuntos.findByCotizacionIdOrderBySubidoEnAsc(cotizacionId)).isEmpty();
    }

    @Test
    void unEncargadoQueNoTieneElItemAsignadoDa403() throws Exception {
        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId).file(archivo("a.pdf", PDF))
                        .with(como(OTRO_ENCARGADO, "ENCARGADO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void unSolicitanteNoPuedeSubirNiVer() throws Exception {
        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId).file(archivo("a.pdf", PDF))
                        .with(como(9L, "SOLICITANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId).with(como(9L, "SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void unaCotizacionInexistenteDa404() throws Exception {
        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", 999999L).file(archivo("a.pdf", PDF))
                        .with(como(ENCARGADO, "ENCARGADO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void conElItemYaResueltoDa422() throws Exception {
        gestion.rechazar("No hay presupuesto", OffsetDateTime.now());
        gestiones.saveAndFlush(gestion);

        mockMvc.perform(multipart("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId).file(archivo("a.pdf", PDF))
                        .with(como(ENCARGADO, "ENCARGADO")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ITEM_RESUELTO"));
    }

    @Test
    void cualquierEncargadoPuedeListarYDescargar() throws Exception {
        long id = subirComoEncargado();

        mockMvc.perform(get("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId)
                        .with(como(OTRO_ENCARGADO, "ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].nombre").value("presupuesto.pdf"));
        mockMvc.perform(get("/api/v1/adjuntos-cotizacion/{id}", id).with(como(OTRO_ENCARGADO, "ENCARGADO")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("presupuesto.pdf")))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/v1/cotizaciones/{id}/adjuntos", cotizacionId))
                .andExpect(status().isUnauthorized());
    }
}
