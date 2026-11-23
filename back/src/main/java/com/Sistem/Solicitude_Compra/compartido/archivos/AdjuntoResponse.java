package com.Sistem.Solicitude_Compra.compartido.archivos;

import java.time.Instant;

/** Lo que se devuelve de un adjunto (de un ítem o de una cotización). */
public record AdjuntoResponse(Long id, String nombre, String mimeType, long tamanoBytes, Instant subidoEn) {
}
