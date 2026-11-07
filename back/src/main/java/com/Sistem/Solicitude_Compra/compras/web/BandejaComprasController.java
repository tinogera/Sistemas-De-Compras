package com.Sistem.Solicitude_Compra.compras.web;

import com.Sistem.Solicitude_Compra.compartido.seguridad.UsuarioActual;
import com.Sistem.Solicitude_Compra.compartido.tipos.Urgencia;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.servicio.BandejaComprasService;
import com.Sistem.Solicitude_Compra.compras.servicio.GestionComprasService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@PreAuthorize("hasRole('ENCARGADO')")
@RestController
@RequestMapping("/api/v1/bandeja")
public class BandejaComprasController {

    private final BandejaComprasService bandeja;
    private final GestionComprasService gestion;
    private final UsuarioActual usuarioActual;

    public BandejaComprasController(BandejaComprasService bandeja, GestionComprasService gestion,
                                    UsuarioActual usuarioActual) {
        this.bandeja = bandeja;
        this.gestion = gestion;
        this.usuarioActual = usuarioActual;
    }

    @GetMapping
    public Pagina<BandejaItemResponse> listar(
            @RequestParam(required = false) Boolean asignadosAMi,
            @RequestParam(required = false) Urgencia urgencia,
            @RequestParam(required = false) Boolean paraComprar,
            Pageable pageable) {
        return Pagina.de(bandeja.listar(asignadosAMi, urgencia, paraComprar, usuarioActual.id(), pageable));
    }

    @GetMapping("/{itemId}")
    public GestionItemDetalleResponse obtener(@PathVariable Long itemId) {
        return bandeja.obtener(itemId);
    }

    @GetMapping("/{itemId}/proveedores-sugeridos")
    public List<ProveedorSugeridoResponse> proveedoresSugeridos(@PathVariable Long itemId) {
        return bandeja.proveedoresSugeridos(itemId);
    }

    // ---- T09.4 ----

    @PostMapping("/{itemId}/tomar")
    public GestionItemDetalleResponse tomar(@PathVariable Long itemId) {
        gestion.tomar(itemId, usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    @PostMapping("/{itemId}/reasignar")
    public GestionItemDetalleResponse reasignar(@PathVariable Long itemId) {
        gestion.reasignar(itemId, usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    // ---- T10.2 ----

    @PostMapping("/{itemId}/cotizaciones")
    public ResponseEntity<CotizacionResponse> cargarCotizacion(@PathVariable Long itemId,
                                                               @Valid @RequestBody CargarCotizacionRequest pedido) {
        Cotizacion creada = gestion.cargarCotizacion(itemId, pedido, usuarioActual.id());
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(creada.getId()).toUri();
        return ResponseEntity.created(ubicacion)
                .body(CotizacionResponse.de(creada, LocalDate.now(Cotizacion.ZONA)));
    }

    // ---- T10.3 ----

    @PostMapping("/{itemId}/cotizaciones/{cotizacionId}/elegir")
    public GestionItemDetalleResponse elegirCotizacion(@PathVariable Long itemId, @PathVariable Long cotizacionId,
                                                       @Valid @RequestBody ElegirCotizacionRequest pedido) {
        gestion.elegirCotizacion(itemId, cotizacionId, pedido.version(), usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    @PutMapping("/{itemId}/precio-total")
    public GestionItemDetalleResponse fijarPrecioTotal(@PathVariable Long itemId,
                                                       @Valid @RequestBody PrecioTotalRequest pedido) {
        gestion.fijarPrecioTotal(itemId, pedido, usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    @DeleteMapping("/{itemId}/precio-total")
    public ResponseEntity<Void> quitarPrecioTotal(@PathVariable Long itemId, @RequestParam Long version) {
        gestion.quitarPrecioTotal(itemId, version, usuarioActual.id());
        return ResponseEntity.noContent().build();
    }

    // ---- T10.4 ----

    @PostMapping("/{itemId}/rechazar")
    public GestionItemDetalleResponse rechazar(@PathVariable Long itemId,
                                               @Valid @RequestBody RechazarItemRequest pedido) {
        gestion.rechazar(itemId, pedido.motivo(), pedido.version(), usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    // ---- T10.7 ----

    @PostMapping("/{itemId}/comprar")
    public GestionItemDetalleResponse comprar(@PathVariable Long itemId, @Valid @RequestBody ComprarRequest pedido) {
        gestion.comprar(itemId, pedido, usuarioActual.id());
        return bandeja.obtener(itemId);
    }

    // ---- T10.8 ----

    @PatchMapping("/{itemId}/compra")
    public GestionItemDetalleResponse editarCompra(@PathVariable Long itemId,
                                                   @Valid @RequestBody EditarCompraRequest pedido) {
        gestion.editarCompra(itemId, pedido, usuarioActual.id());
        return bandeja.obtener(itemId);
    }
}
