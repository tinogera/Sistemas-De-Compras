package com.Sistem.Solicitude_Compra.ordenes.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.ordenes.dominio.EstadoOrden;
import com.Sistem.Solicitude_Compra.ordenes.dominio.OrdenPedido;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenPdf;
import com.Sistem.Solicitude_Compra.ordenes.servicio.OrdenService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ENCARGADO')")
@RestController
@RequestMapping("/api/v1/ordenes")
public class OrdenController {

    private final OrdenService service;
    private final UsuarioActual usuarioActual;
    private final OrdenPdf ordenPdf;

    public OrdenController(OrdenService service, UsuarioActual usuarioActual, OrdenPdf ordenPdf) {
        this.service = service;
        this.usuarioActual = usuarioActual;
        this.ordenPdf = ordenPdf;
    }

    @GetMapping
    public Pagina<OrdenResponse> listar(@RequestParam(required = false) EstadoOrden estado, Pageable pageable) {
        return Pagina.de(service.listar(estado, pageable).map(OrdenResponse::de));
    }

    @GetMapping("/{id}")
    public OrdenDetalleResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        OrdenPedido orden = service.buscar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orden-" + orden.getNumero() + ".pdf").build().toString())
                .body(ordenPdf.generar(orden));
    }

    @PostMapping("/{id}/marcar-enviada")
    public OrdenResponse marcarEnviada(@PathVariable Long id) {
        return OrdenResponse.de(service.marcarEnviada(id, usuarioActual.id()));
    }
}
