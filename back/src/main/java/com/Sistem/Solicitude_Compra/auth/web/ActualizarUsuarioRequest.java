package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Todos los campos son opcionales: el que viene en null no se toca. */
public record ActualizarUsuarioRequest(
        @Size(max = 150) @Pattern(regexp = ".*\\S.*", message = "El nombre no puede estar vacío") String nombre,
        Long sectorId,
        Rol rol,
        Boolean activo) {
}
