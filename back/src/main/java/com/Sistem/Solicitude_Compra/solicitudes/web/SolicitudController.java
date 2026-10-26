package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {

    private final SolicitudService service;
    private final UsuarioActual usuarioActual;

    public SolicitudController(SolicitudService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping
    public ResponseEntity<SolicitudCreadaResponse> crear(@Valid @RequestBody CrearSolicitudRequest request) {
        Solicitud solicitud = service.crear(usuarioActual.id(), request);
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + solicitud.getId()))
                .body(SolicitudCreadaResponse.de(solicitud));
    }

    @GetMapping
    public Pagina<SolicitudResumenResponse> listar(
            @RequestParam(defaultValue = "true") boolean mias,
            @RequestParam(required = false) EstadoSolicitud estado,
            @PageableDefault(sort = "creadaEn", direction = Sort.Direction.DESC) Pageable pageable) {
        return Pagina.de(service.listar(usuarioActual.id(), usuarioActual.rol(), mias, estado, pageable));
    }

    @GetMapping("/{id}")
    public SolicitudDetalleResponse obtener(@PathVariable Long id) {
        return service.obtener(id, usuarioActual.id(), usuarioActual.rol());
    }
}
