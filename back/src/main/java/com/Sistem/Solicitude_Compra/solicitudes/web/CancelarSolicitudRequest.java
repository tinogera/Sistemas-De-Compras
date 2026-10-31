package com.Sistem.Solicitude_Compra.solicitudes.web;

import jakarta.validation.constraints.NotNull;

public record CancelarSolicitudRequest(@NotNull(message = "La versión es obligatoria") Long version) {
}
