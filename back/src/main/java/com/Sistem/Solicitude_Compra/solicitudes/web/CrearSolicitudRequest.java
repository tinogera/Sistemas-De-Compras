package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record CrearSolicitudRequest(
        @NotNull(message = "La urgencia es obligatoria") Urgencia urgencia,
        @NotNull(message = "La fecha necesaria es obligatoria")
        @FutureOrPresent(message = "La fecha necesaria no puede ser anterior a hoy") LocalDate fechaNecesaria,
        String observaciones,
        @NotEmpty(message = "Tenés que cargar al menos un ítem") List<@Valid ItemRequest> items) {
}
