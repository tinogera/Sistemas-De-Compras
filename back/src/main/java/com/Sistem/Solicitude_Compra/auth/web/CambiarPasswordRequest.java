package com.Sistem.Solicitude_Compra.auth.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CambiarPasswordRequest(
        @NotNull @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
        String passwordNueva) {
}
