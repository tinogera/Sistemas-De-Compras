package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.auth.dominio.Usuario;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;

/** Nunca incluye el hash de la contraseña. */
public record UsuarioAdminResponse(Long id, String email, String nombre, Long sectorId, String sector,
                                   Rol rol, boolean activo) {
    static UsuarioAdminResponse de(Usuario u) {
        return new UsuarioAdminResponse(u.getId(), u.getEmail(), u.getNombre(), u.getSector().getId(),
                u.getSector().getNombre(), u.getRol(), Boolean.TRUE.equals(u.getActivo()));
    }
}
