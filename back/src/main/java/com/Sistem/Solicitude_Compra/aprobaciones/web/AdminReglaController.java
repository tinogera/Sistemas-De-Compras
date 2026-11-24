package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.servicios.ReglaAdminService;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reglas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReglaController {

    private final ReglaAdminService service;
    private final UsuarioActual usuarioActual;

    public AdminReglaController(ReglaAdminService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping("/simular")
    public SimulacionResponse simular(@Valid @RequestBody SimulacionRequest request) {
        return service.simular(request);
    }

    @GetMapping
    public List<ReglaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ReglaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReglaResponse crear(@Valid @RequestBody ReglaRequest request) {
        return service.crear(request, usuarioActual.id());
    }

    @PutMapping("/{id}")
    public ReglaResponse reemplazar(@PathVariable Long id, @Valid @RequestBody ReglaRequest request) {
        return service.reemplazar(id, request, usuarioActual.id());
    }

    @PostMapping("/{id}/activar")
    public ReglaResponse activar(@PathVariable Long id) {
        return service.activar(id, usuarioActual.id());
    }

    @PostMapping("/{id}/desactivar")
    public ReglaResponse desactivar(@PathVariable Long id) {
        return service.desactivar(id, usuarioActual.id());
    }
}
