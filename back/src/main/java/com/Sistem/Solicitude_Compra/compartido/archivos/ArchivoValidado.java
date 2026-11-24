package com.Sistem.Solicitude_Compra.compartido.archivos;

/** Resultado de validar un archivo subido: el tipo real (detectado por contenido) y su extensión. */
public record ArchivoValidado(String mimeType, String extension, long tamanoBytes) {
}
