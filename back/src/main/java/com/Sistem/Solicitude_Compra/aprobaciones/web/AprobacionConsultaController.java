package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.servicios.ConsultaAprobacionService;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Detalle;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Fila;
import com.Sistem.Solicitude_Compra.aprobaciones.web.AprobacionResponses.Resumen;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Consultas de los aprobadores (T23.6). */
@RestController
@RequestMapping("/api/v1/aprobaciones")
public class AprobacionConsultaController {

    private final ConsultaAprobacionService service;
    private final UsuarioActual usuario;

    public AprobacionConsultaController(ConsultaAprobacionService service, UsuarioActual usuario) {
        this.service = service;
        this.usuario = usuario;
    }

    @GetMapping
    public Pagina<Fila> listar(@RequestParam(defaultValue = "para-decidir") String vista,
                               @PageableDefault(size = 20) Pageable pageable) {
        return Pagina.de(service.listar(vista, usuario.id(), usuario.rol(), usuario.sectorId(), pageable));
    }

    @GetMapping("/items/{itemId}")
    public Detalle detalle(@PathVariable Long itemId) {
        return service.detalle(itemId, usuario.id(), usuario.rol(), usuario.sectorId());
    }

    @GetMapping("/resumen")
    public Resumen resumen() {
        return service.resumen(usuario.id(), usuario.rol(), usuario.sectorId());
    }
}
