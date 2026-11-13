package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String nombre,
        @NotNull Long sectorId,
        @NotNull Rol rol,
        @NotNull @Size(min = 8, max = 72, message = "La contraseña tiene que tener entre 8 y 72 caracteres")
        String passwordInicial) {
}
