package com.Sistem.Solicitude_Compra.solicitudes.servicio;

import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;

/** `creada` es false cuando la clave de idempotencia ya existía y se devolvió la solicitud de antes. */
public record ResultadoCreacion(Solicitud solicitud, boolean creada) {
}
