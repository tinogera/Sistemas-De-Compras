package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.auth.servicio.UsuarioAdminService;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Rol;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioAdminService service;
    private final UsuarioActual usuarioActual;

    public AdminUsuarioController(UsuarioAdminService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public Pagina<UsuarioAdminResponse> listar(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Rol rol,
                                               @RequestParam(defaultValue = "0") int page) {
        return Pagina.de(service.listar(q, rol, page).map(UsuarioAdminResponse::de));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioAdminResponse crear(@Valid @RequestBody CrearUsuarioRequest r) {
        return UsuarioAdminResponse.de(
                service.crear(r.email(), r.nombre(), r.sectorId(), r.rol(), r.passwordInicial()));
    }

    @PatchMapping("/{id}")
    public UsuarioAdminResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest r) {
        return UsuarioAdminResponse.de(
                service.actualizar(id, usuarioActual.id(), r.nombre(), r.sectorId(), r.rol(), r.activo()));
    }

    @PostMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(@PathVariable Long id, @Valid @RequestBody CambiarPasswordRequest r) {
        service.cambiarPassword(id, r.passwordNueva());
    }
}
