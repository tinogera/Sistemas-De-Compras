package com.Sistem.Solicitude_Compra.auth.web;

import com.Sistem.Solicitude_Compra.auth.servicio.SectorAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sectores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSectorController {

    private final SectorAdminService service;

    public AdminSectorController(SectorAdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<SectorResponse> listar() {
        return service.listar().stream().map(SectorResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SectorResponse crear(@Valid @RequestBody SectorRequest request) {
        return SectorResponse.de(service.crear(request.nombre()));
    }

    @PatchMapping("/{id}")
    public SectorResponse renombrar(@PathVariable Long id, @Valid @RequestBody SectorRequest request) {
        return SectorResponse.de(service.renombrar(id, request.nombre()));
    }
}
