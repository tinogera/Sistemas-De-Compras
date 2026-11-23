package com.Sistem.Solicitude_Compra.compartido.archivos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Guarda los archivos en una carpeta del servidor. En disco el nombre es siempre un UUID generado acá:
 * el nombre que manda el usuario no se usa nunca (evita escribir fuera de la carpeta con "../").
 */
@Component
public class AlmacenamientoArchivos {

    private final Path carpeta;

    public AlmacenamientoArchivos(@Value("${app.adjuntos.dir}") String carpeta) {
        this.carpeta = Path.of(carpeta).toAbsolutePath().normalize();
    }

    /** Guarda el contenido y devuelve la clave (UUID.extension) para volver a encontrarlo. */
    public String guardar(InputStream contenido, String extension) {
        String clave = UUID.randomUUID() + extensionSegura(extension);
        try {
            Files.createDirectories(carpeta);
            Files.copy(contenido, ruta(clave), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar el archivo", e);
        }
        return clave;
    }

    public Resource abrir(String clave) {
        return new FileSystemResource(ruta(clave));
    }

    public void borrar(String clave) {
        try {
            Files.deleteIfExists(ruta(clave));
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar el archivo", e);
        }
    }

    /** Solo letras y números, para que la extensión no pueda traer rutas ni caracteres raros. */
    private static String extensionSegura(String extension) {
        if (extension == null) {
            return "";
        }
        String limpia = extension.startsWith(".") ? extension.substring(1) : extension;
        return limpia.matches("[A-Za-z0-9]{1,10}") ? "." + limpia.toLowerCase() : "";
    }

    private Path ruta(String clave) {
        Path ruta = carpeta.resolve(clave).normalize();
        if (!carpeta.equals(ruta.getParent())) {
            throw new IllegalArgumentException("Clave de archivo inválida");
        }
        return ruta;
    }
}
