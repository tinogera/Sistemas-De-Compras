package com.Sistem.Solicitude_Compra.solicitudes.web;

import com.Sistem.Solicitude_Compra.compartido.archivos.AdjuntoResponse;
import com.Sistem.Solicitude_Compra.compartido.archivos.DescargaArchivo;
import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.solicitudes.dominio.AdjuntoItem;
import com.Sistem.Solicitude_Compra.solicitudes.servicio.AdjuntoItemService;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
public class AdjuntoItemController {

    private final AdjuntoItemService service;
    private final UsuarioActual usuarioActual;

    public AdjuntoItemController(AdjuntoItemService service, UsuarioActual usuarioActual) {
        this.service = service;
        this.usuarioActual = usuarioActual;
    }

    @PostMapping(path = "/api/v1/items/{itemId}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoResponse> subir(@PathVariable Long itemId,
                                                 @RequestPart("archivo") MultipartFile archivo) {
        AdjuntoResponse creado = service.subir(itemId, archivo, usuarioActual.id());
        return ResponseEntity.created(URI.create("/api/v1/adjuntos-item/" + creado.id())).body(creado);
    }

    @GetMapping("/api/v1/items/{itemId}/adjuntos")
    public List<AdjuntoResponse> listar(@PathVariable Long itemId) {
        return service.listar(itemId, usuarioActual.id(), usuarioActual.rol());
    }

    @GetMapping("/api/v1/adjuntos-item/{id}")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        AdjuntoItem adjunto = service.buscar(id, usuarioActual.id(), usuarioActual.rol());
        return DescargaArchivo.de(service.contenido(adjunto), adjunto.getNombre(), adjunto.getMimeType());
    }
}
