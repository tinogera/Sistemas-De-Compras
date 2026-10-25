package com.Sistem.Solicitude_Compra.ordenes.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ENCARGADO')")
@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final OrdenService service;
    private final UsuarioActual usuarioActual;

    public OrdenController(OrdenService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public Pagina<OrdenResponse> listar(@RequestParam(required = false) EstadoOrden estado, Pageable pageable) {
        return Pagina.de(service.listar(estado, pageable).map(OrdenResponse::de));
    }

    @GetMapping("/{id}")
    public OrdenDetalleResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping("/{id}/marcar-enviada")
    public OrdenResponse marcarEnviada(@PathVariable Long id) {
        return OrdenResponse.de(service.marcarEnviada(id, usuarioActual.id()));
    }
}
