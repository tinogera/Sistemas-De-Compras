package com.Sistem.Solicitude_Compra.auth;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;

public record UsuarioResumen(Long id, String nombre, String email, Rol rol, Long sectorId, boolean activo) {
}
