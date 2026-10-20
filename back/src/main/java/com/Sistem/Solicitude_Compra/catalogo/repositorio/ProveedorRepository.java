package com.Sistem.Solicitude_Compra.catalogo.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Proveedores sugeridos para una categoría
    List<Proveedor> findByActivoTrueAndCategorias_Id(Long categoriaId);

    // Buscador
    Page<Proveedor> findByRazonSocialContainingIgnoreCase(String texto, Pageable pageable);

    boolean existsByCuit(String cuit);
}
