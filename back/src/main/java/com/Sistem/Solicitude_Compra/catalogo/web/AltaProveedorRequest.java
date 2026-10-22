package com.Sistem.Solicitude_Compra.catalogo.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AltaProveedorRequest(
        @NotBlank @Size(max = 200) String razonSocial,
        @Pattern(regexp = "|[0-9]{2}-[0-9]{8}-[0-9]") String cuit,
        @Size(max = 50) String telefono,
        @Email @Size(max = 255) String email,
        @Size(max = 150) String contacto,
        @NotEmpty List<Long> categoriaIds) {
}
