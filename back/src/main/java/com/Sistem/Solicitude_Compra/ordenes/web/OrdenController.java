package com.Sistem.Solicitude_Compra.ordenes.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO T05.1: solo ENCARGADO (@PreAuthorize). Hasta entonces, cualquier usuario logueado puede llamarlo.
@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final OrdenService service;
    private final UsuarioActual usuarioActual;

    public OrdenController(OrdenService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/{id}/marcar-enviada")
    public OrdenResponse marcarEnviada(@PathVariable Long id) {
        return OrdenResponse.de(service.marcarEnviada(id, usuarioActual.id()));
    }
}
