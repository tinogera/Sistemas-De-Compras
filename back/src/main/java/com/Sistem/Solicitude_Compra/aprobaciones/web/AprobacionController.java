package com.Sistem.Solicitude_Compra.aprobaciones.web;

import com.Sistem.Solicitude_Compra.aprobaciones.servicios.CadenaAprobacionService;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Aprobar o rechazar un paso de la cadena. Quién puede decidir lo valida el servicio, paso por paso. */
@RestController
@RequestMapping("/api/v1/aprobaciones/pasos/{pasoId}")
public class AprobacionController {

    public record AprobarRequest(@NotNull(message = "La versión es obligatoria") Integer version) {
    }

    public record RechazarRequest(
            @NotBlank(message = "El motivo es obligatorio") @Size(max = 1000) String motivo,
            @NotNull(message = "La versión es obligatoria") Integer version) {
    }

    private final CadenaAprobacionService cadena;
    private final UsuarioActual usuario;

    public AprobacionController(CadenaAprobacionService cadena, UsuarioActual usuario) {
        this.cadena = cadena;
        this.usuario = usuario;
    }

    @PostMapping("/aprobar")
    @ResponseStatus(HttpStatus.OK)
    public void aprobar(@PathVariable Long pasoId, @Valid @RequestBody AprobarRequest request) {
        cadena.aprobar(pasoId, request.version(), usuario.id(), usuario.rol(), usuario.sectorId());
    }

    @PostMapping("/rechazar")
    @ResponseStatus(HttpStatus.OK)
    public void rechazar(@PathVariable Long pasoId, @Valid @RequestBody RechazarRequest request) {
        cadena.rechazar(pasoId, request.motivo(), request.version(), usuario.id(), usuario.rol(), usuario.sectorId());
    }
}
