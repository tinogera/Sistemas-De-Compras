package com.Sistem.Solicitude_Compra.compartido.seguridad;

import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class UsuarioActual {

    public Long id() {
        return Long.valueOf(jwt().getSubject());
    }

    public Rol rol() {
        return Rol.valueOf(jwt().getClaimAsString("rol"));
    }

    public Long sectorId() {
        return ((Number) jwt().getClaim("sectorId")).longValue();
    }

    public boolean esEncargado() {
        return rol() == Rol.ENCARGADO;
    }

    private Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No hay un usuario autenticado");
        }
        return jwt;
    }
}
