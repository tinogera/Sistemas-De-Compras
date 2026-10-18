package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;

public record UsuarioLogin(Long id, String nombre, String email, Rol rol, Long sectorId) {
}
