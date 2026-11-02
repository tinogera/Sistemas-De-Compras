package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.compras.servicio.BandejaComprasService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ENCARGADO')")
@RestController
@RequestMapping("/api/v1/bandeja")
public class BandejaComprasController {

    private final BandejaComprasService service;
    private final UsuarioActual usuarioActual;

    public BandejaComprasController(BandejaComprasService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public Pagina<BandejaItemResponse> listar(
            @RequestParam(required = false) Boolean asignadosAMi,
            @RequestParam(required = false) Urgencia urgencia,
            @RequestParam(required = false) Boolean paraComprar,
            Pageable pageable) {
        return Pagina.de(service.listar(asignadosAMi, urgencia, paraComprar, usuarioActual.id(), pageable));
    }

    @GetMapping("/{itemId}")
    public GestionItemDetalleResponse obtener(@PathVariable Long itemId) {
        return service.obtener(itemId);
    }
}