package com.Sistem.Solicitude_Compra.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectorRequest(@NotBlank @Size(max = 100) String nombre) {
}
