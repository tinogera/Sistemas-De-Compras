package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.archivos.AdjuntoResponse;
import com.Sistem.Solicitude_Compra.compartido.archivos.DescargaArchivo;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compras.dominio.AdjuntoCotizacion;
import com.Sistem.Solicitude_Compra.compras.servicio.AdjuntoCotizacionService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@PreAuthorize("hasRole('ENCARGADO')")
@RestController
public class AdjuntoCotizacionController {

    private final AdjuntoCotizacionService service;
    private final UsuarioActual usuarioActual;

    public AdjuntoCotizacionController(AdjuntoCotizacionService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping(path = "/api/v1/cotizaciones/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoResponse> subir(@PathVariable Long id,
                                                 @RequestPart("archivo") MultipartFile archivo) {
        AdjuntoResponse creado = service.subir(id, archivo, usuarioActual.id());
        return ResponseEntity.created(URI.create("/api/v1/adjuntos-cotizacion/" + creado.id())).body(creado);
    }

    @GetMapping("/api/v1/cotizaciones/{id}/adjuntos")
    public List<AdjuntoResponse> listar(@PathVariable Long id) {
        return service.listar(id);
    }

    @GetMapping("/api/v1/adjuntos-cotizacion/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        AdjuntoCotizacion adjunto = service.buscar(id);
        return DescargaArchivo.de(service.contenido(adjunto), adjunto.getNombre(), adjunto.getMimeType());
    }
}
