package com.Sistem.Solicitude_Compra.auditoria.web;

import com.Sistem.Solicitude_Compra.auditoria.dominio.EntidadHistorial;
import com.Sistem.Solicitude_Compra.auditoria.servicio.HistorialService;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/historial")
public class HistorialController {

    private final HistorialService service;
    private final UsuarioActual usuarioActual;

    public HistorialController(HistorialService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public List<HistorialResponse> consultar(@RequestParam EntidadHistorial entidad, @RequestParam Long id,
                                             @RequestParam(defaultValue = "false") boolean incluirItems) {
        return service.consultar(entidad, id, incluirItems, usuarioActual.id(), usuarioActual.rol());
    }
}
