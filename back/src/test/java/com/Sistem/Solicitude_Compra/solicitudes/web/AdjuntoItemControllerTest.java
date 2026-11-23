package com.Sistem.Solicitude_Compra.solicitudes.web;

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
import com.Sistem.Solicitude_Compra.compartido.tipos.Moneda;
import com.Sistem.Solicitude_Compra.compartido.tipos.TipoItem;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoItem;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Item;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.AdjuntoItemRepository;
import com.Sistem.Solicitude_Compra.solicitudes.repositorio.SolicitudRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

/** T14.2 por la API completa: validación del tipo real, permisos y descarga. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789012345678901234567890123456789012345678901",
        "app.jwt.expiracion-horas=8"})
class AdjuntoItemControllerTest {

    private static final Long DUENO = 51L;
    private static final Long OTRO = 52L;
    private static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);

    private static Path carpeta;

    @DynamicPropertySource
    static void carpetaTemporal(DynamicPropertyRegistry registro) throws IOException {
        carpeta = Files.createTempDirectory("adjuntos-test");
        registro.add("app.adjuntos.dir", carpeta::toString);
    }

    @Autowired MockMvc mockMvc;
    @Autowired SolicitudRepository solicitudes;
    @Autowired AdjuntoItemRepository adjuntos;

    private Long itemId;

    @BeforeEach
    void preparar() {
        limpiar();
        Solicitud s = new Solicitud();
        s.setSolicitanteId(DUENO);
        s.setSectorId(1L);
        s.setUrgencia(Urgencia.ALTA);
        s.setFechaNecesaria(LocalDate.now().plusDays(3));
        Item item = new Item();
        item.setTipo(TipoItem.PRODUCTO);
        item.setCategoriaId(1L);
        item.setDetalle("Resma");
        item.setCantidad(new BigDecimal("2"));
        item.setPrecioEstimado(new BigDecimal("100"));
        item.setMonedaEstimada(Moneda.ARS);
        s.agregarItem(item);
        itemId = solicitudes.saveAndFlush(s).getItems().get(0).getId();
    }

    @AfterEach
    void limpiar() {
        solicitudes.deleteAll(); // borra los ítems y sus adjuntos en cascada
    }

    private static RequestPostProcessor como(Long id, String rol) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("rol", rol).claim("sectorId", 1))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static MockMultipartFile archivo(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "application/pdf", contenido);
    }

    private String subirComoDueno() throws Exception {
        return mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId).file(archivo("../spec.pdf", PDF))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void elDuenoSubeUnPdfYQuedaEnDiscoConNombreUuid() throws Exception {
        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId).file(archivo("especificacion.pdf", PDF))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("especificacion.pdf"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.tamanoBytes").value(PDF.length))
                .andExpect(jsonPath("$.subidoEn").exists());

        var guardado = adjuntos.findByItemIdOrderBySubidoEnAsc(itemId).get(0);
        assertThat(guardado.getClaveObjeto()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(carpeta.resolve(guardado.getClaveObjeto()))).isEqualTo(PDF);
    }

    @Test
    void unTxtRenombradoAPdfDa422() throws Exception {
        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId)
                        .file(archivo("presupuesto.pdf", "solo texto".getBytes()))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("TIPO_NO_PERMITIDO"));
        assertThat(adjuntos.findByItemIdOrderBySubidoEnAsc(itemId)).isEmpty();
    }

    @Test
    void otroSolicitanteNoPuedeSubir() throws Exception {
        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId).file(archivo("a.pdf", PDF))
                        .with(como(OTRO, "SOLICITANTE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void conElItemYaTerminadoNoSeAceptanAdjuntos() throws Exception {
        var s = solicitudes.findAll().get(0);
        Item item = solicitudes.findConItemsById(s.getId()).orElseThrow().getItems().get(0);
        item.setEstado(EstadoItem.CANCELADO);
        solicitudes.saveAndFlush(item.getSolicitud());

        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId).file(archivo("a.pdf", PDF))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("ITEM_FINALIZADO"));
    }

    @Test
    void unItemInexistenteDa404() throws Exception {
        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", 999999L).file(archivo("a.pdf", PDF))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void elDuenoYElEncargadoListanYDescarganConElNombreOriginal() throws Exception {
        String respuesta = subirComoDueno();
        long id = Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        for (var usuario : new RequestPostProcessor[] {como(DUENO, "SOLICITANTE"), como(1L, "ENCARGADO")}) {
            mockMvc.perform(get("/api/v1/items/{id}/adjuntos", itemId).with(usuario))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(id))
                    .andExpect(jsonPath("$[0].nombre").value("spec.pdf"));
            mockMvc.perform(get("/api/v1/adjuntos-item/{id}", id).with(usuario))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", containsString("spec.pdf")))
                    .andExpect(content().bytes(PDF));
        }
    }

    @Test
    void otroSolicitanteNoPuedeListarNiDescargar() throws Exception {
        String respuesta = subirComoDueno();
        long id = Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/v1/items/{id}/adjuntos", itemId).with(como(OTRO, "SOLICITANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/adjuntos-item/{id}", id).with(como(OTRO, "SOLICITANTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/v1/items/{id}/adjuntos", itemId)).andExpect(status().isUnauthorized());
    }

    @Test
    void masDe10MbDa413() throws Exception {
        byte[] grande = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        mockMvc.perform(multipart("/api/v1/items/{id}/adjuntos", itemId).file(archivo("grande.pdf", grande))
                        .with(como(DUENO, "SOLICITANTE")))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.codigo").value("ARCHIVO_DEMASIADO_GRANDE"));
    }
}
