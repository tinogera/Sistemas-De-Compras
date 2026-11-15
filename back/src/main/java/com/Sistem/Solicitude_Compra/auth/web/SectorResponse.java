package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.auth.dominio.Sector;

public record SectorResponse(Long id, String nombre) {
    static SectorResponse de(Sector s) {
        return new SectorResponse(s.getId(), s.getNombre());
    }
}
