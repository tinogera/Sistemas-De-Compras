package com.Sistem.Solicitude_Compra.compartido.archivos;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/** Acepta solo PDF, JPG y PNG de hasta 10 MB, mirando el contenido real y no el nombre ni el tipo declarado. */
@Component
public class ValidadorArchivos {

    public static final long MAXIMO_BYTES = 10L * 1024 * 1024;

    private static final Map<String, String> EXTENSIONES = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/png", "png");

    private final Tika tika = new Tika();

    public ArchivoValidado validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw ArchivoInvalidoException.vacio();
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw ArchivoInvalidoException.demasiadoGrande();
        }
        String mime;
        try (InputStream contenido = archivo.getInputStream()) {
            mime = tika.detect(contenido); // solo por contenido: sin nombre de archivo
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo", e);
        }
        String extension = EXTENSIONES.get(mime);
        if (extension == null) {
            throw ArchivoInvalidoException.tipoNoPermitido();
        }
        return new ArchivoValidado(mime, extension, archivo.getSize());
    }
}
