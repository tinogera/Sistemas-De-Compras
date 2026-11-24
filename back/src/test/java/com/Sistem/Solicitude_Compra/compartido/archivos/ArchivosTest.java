package com.Sistem.Solicitude_Compra.compartido.archivos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

class ArchivosTest {

    @TempDir Path carpeta;

    static final byte[] PDF = "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R',
            0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0};
    static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F', 0, 1,
            1, 0, 0, 1, 0, 1, 0, 0, (byte) 0xFF, (byte) 0xD9};

    // --- almacenamiento ---

    @Test
    void guardaConUnNombreUuidYSePuedeAbrirYBorrar() throws IOException {
        var almacenamiento = new AlmacenamientoArchivos(carpeta.toString());

        String clave = almacenamiento.guardar(new ByteArrayInputStream(PDF), "pdf");

        assertThat(clave).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(Files.readAllBytes(almacenamiento.abrir(clave).getFile().toPath())).isEqualTo(PDF);

        almacenamiento.borrar(clave);
        assertThat(almacenamiento.abrir(clave).exists()).isFalse();
    }

    @Test
    void creaLaCarpetaSiNoExiste() {
        var almacenamiento = new AlmacenamientoArchivos(carpeta.resolve("nueva/adjuntos").toString());

        String clave = almacenamiento.guardar(new ByteArrayInputStream(PDF), "pdf");

        assertThat(almacenamiento.abrir(clave).exists()).isTrue();
    }

    @Test
    void unaExtensionConRutaSeIgnoraYElArchivoQuedaDentroDeLaCarpeta() throws IOException {
        var almacenamiento = new AlmacenamientoArchivos(carpeta.toString());

        String clave = almacenamiento.guardar(new ByteArrayInputStream(PDF), "../../evil.sh");

        assertThat(clave).matches("[0-9a-f-]{36}");
        try (var archivos = Files.list(carpeta)) {
            assertThat(archivos).hasSize(1);
        }
    }

    @Test
    void unaClaveConRutaSeRechaza() {
        var almacenamiento = new AlmacenamientoArchivos(carpeta.toString());

        assertThatThrownBy(() -> almacenamiento.abrir("../secreto.txt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> almacenamiento.borrar("../../secreto.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- validación ---

    private final ValidadorArchivos validador = new ValidadorArchivos();

    private MockMultipartFile archivo(String nombre, String mime, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, mime, contenido);
    }

    @Test
    void aceptaPdfJpgYPng() {
        assertThat(validador.validar(archivo("a.pdf", "application/pdf", PDF)).mimeType())
                .isEqualTo("application/pdf");
        assertThat(validador.validar(archivo("a.png", "image/png", PNG)).extension()).isEqualTo("png");
        assertThat(validador.validar(archivo("a.jpg", "image/jpeg", JPG)).mimeType()).isEqualTo("image/jpeg");
    }

    @Test
    void unTxtRenombradoAPdfSeRechazaPorSuContenido() {
        var falso = archivo("presupuesto.pdf", "application/pdf", "hola, esto es texto".getBytes());

        assertThatThrownBy(() -> validador.validar(falso))
                .isInstanceOfSatisfying(ArchivoInvalidoException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("TIPO_NO_PERMITIDO");
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
                });
    }

    @Test
    void unPdfConExtensionYTipoDeOtraCosaSiSeAceptaPorSuContenido() {
        assertThat(validador.validar(archivo("foto.txt", "text/plain", PDF)).mimeType())
                .isEqualTo("application/pdf");
    }

    @Test
    void unArchivoVacioSeRechaza() {
        assertThatThrownBy(() -> validador.validar(archivo("a.pdf", "application/pdf", new byte[0])))
                .isInstanceOfSatisfying(ArchivoInvalidoException.class,
                        e -> assertThat(e.getCodigo()).isEqualTo("ARCHIVO_VACIO"));
    }

    @Test
    void masDe10MbDa413() {
        byte[] grande = new byte[(int) ValidadorArchivos.MAXIMO_BYTES + 1];
        System.arraycopy(PDF, 0, grande, 0, PDF.length);

        assertThatThrownBy(() -> validador.validar(archivo("a.pdf", "application/pdf", grande)))
                .isInstanceOfSatisfying(ArchivoInvalidoException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE));
    }
}
