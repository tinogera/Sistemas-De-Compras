package com.Sistem.Solicitude_Compra.catalogo.web;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Categoria;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;
import java.util.Comparator;
import java.util.List;

public record ProveedorResponse(
        Long id,
        String razonSocial,
        String cuit,
        String telefono,
        String email,
        String contacto,
        boolean activo,
        List<CategoriaResumenResponse> categorias) {

    public static ProveedorResponse de(Proveedor proveedor) {
        List<CategoriaResumenResponse> categorias = proveedor.getCategorias().stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(CategoriaResumenResponse::de)
                .toList();
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getRazonSocial(),
                proveedor.getCuit(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.getContacto(),
                proveedor.isActivo(),
                categorias);
    }
}
