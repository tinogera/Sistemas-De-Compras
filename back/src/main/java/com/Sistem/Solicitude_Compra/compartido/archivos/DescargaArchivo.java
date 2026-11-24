package com.Sistem.Solicitude_Compra.compartido.archivos;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public final class DescargaArchivo {

    private DescargaArchivo() {
    }

    /** Respuesta de descarga con el nombre original, sin rutas (solo el último tramo del nombre). */
    public static ResponseEntity<Resource> de(Resource archivo, String nombre, String mimeType) {
        String limpio = Paths.get(nombre.replace('\\', '/')).getFileName().toString();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(limpio, StandardCharsets.UTF_8).build().toString())
                .body(archivo);
    }
}
