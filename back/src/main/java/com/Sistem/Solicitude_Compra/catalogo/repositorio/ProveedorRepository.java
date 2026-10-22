package com.Sistem.Solicitude_Compra.catalogo.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.Sistem.Solicitude_Compra.catalogo.dominio.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    // Proveedores sugeridos para una categoría
    List<Proveedor> findByActivoTrueAndCategorias_Id(Long categoriaId);

    // Buscador
    Page<Proveedor> findByRazonSocialContainingIgnoreCase(String texto, Pageable pageable);

    boolean existsByCuit(String cuit);

    boolean existsByCuitAndIdNot(String cuit, Long id);

    @Query("""
        select p from Proveedor p
        where (:q is null or lower(p.razonSocial) like lower(concat('%', :q, '%')))
          and (:activo is null or p.activo = :activo)
          and (:categoriaId is null or exists (
                select 1 from p.categorias c where c.id = :categoriaId))
        """)
    Page<Proveedor> buscar(@Param("q") String q,
                           @Param("categoriaId") Long categoriaId,
                           @Param("activo") Boolean activo,
                           Pageable pageable);
}
