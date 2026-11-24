package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Las consultas agrupadas por sector, categoría y proveedor se agregan en T16.3. */
public interface HechoGastoRepository extends JpaRepository<HechoGasto, Long> {

    boolean existsByItemId(Long itemId);

    /** Para corregir la fila cuando llega CompraActualizada, que trae el ítem y no la orden. */
    Optional<HechoGasto> findByItemId(Long itemId);
}
