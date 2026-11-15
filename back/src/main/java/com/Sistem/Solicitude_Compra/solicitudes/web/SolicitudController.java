package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.EstadoSolicitud;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.Solicitud;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.ResultadoCreacion;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.SolicitudService;
import com.Sistem.Solicitude_Compra.solicitudes.web.SolicitudDetalleResponse.ItemDetalle;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<SolicitudCreadaResponse> crear(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID clave,
            @Valid @RequestBody CrearSolicitudRequest request) {
        ResultadoCreacion resultado = service.crear(usuarioActual.id(), request, clave);
        Solicitud solicitud = resultado.solicitud();
        SolicitudCreadaResponse respuesta = SolicitudCreadaResponse.de(solicitud);
        // Reintento con una clave ya usada: 200 con la solicitud de antes, no 201.
        if (!resultado.creada()) {
            return ResponseEntity.ok(respuesta);
        }
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + solicitud.getId())).body(respuesta);
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

    @PatchMapping("/{id}/items/{itemId}")
    public ItemDetalle editarItem(@PathVariable Long id, @PathVariable Long itemId,
                                  @Valid @RequestBody EditarItemRequest request) {
        return service.editarItem(id, itemId, usuarioActual.id(), request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<Void> cancelarItem(@PathVariable Long id, @PathVariable Long itemId,
                                             @RequestParam Long version) {
        service.cancelarItem(id, itemId, usuarioActual.id(), version);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancelar")
    public SolicitudDetalleResponse cancelar(@PathVariable Long id,
                                             @Valid @RequestBody CancelarSolicitudRequest request) {
        service.cancelarSolicitud(id, usuarioActual.id(), request.version());
        return service.obtener(id, usuarioActual.id(), usuarioActual.rol());
    }
}
