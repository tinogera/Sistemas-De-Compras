package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.servicio.ProveedorService;
import com.Sistem.Solicitude_Compra.compartido.web.Pagina;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {

    private final ProveedorService proveedores;

    public ProveedorController(ProveedorService proveedores) {
        this.proveedores = proveedores;
    }

    @GetMapping
    public Pagina<ProveedorResponse> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable) {
        return proveedores.listar(q, categoriaId, activo, pageable);
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return proveedores.obtener(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody AltaProveedorRequest pedido) {
        ProveedorResponse creado = proveedores.alta(pedido);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(creado.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PatchMapping("/{id}")
    public ProveedorResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarProveedorRequest pedido) {
        return proveedores.actualizar(id, pedido);
    }
}
