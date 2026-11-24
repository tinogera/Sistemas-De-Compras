package com.Sistem.Solicitude_Compra.reportes.repositorio;

import com.Sistem.Solicitude_Compra.reportes.dominio.HechoGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consultas del reporte de gasto (T16.3). Cada una agrupa por la columna elegida y por moneda: pesos y dólares
 * nunca se suman entre sí (regla 10). El rango es inclusivo en las dos puntas.
 */
public interface HechoGastoRepository extends JpaRepository<HechoGasto, Long> {

    boolean existsByItemId(Long itemId);

    /** Para corregir la fila cuando llega CompraActualizada, que trae el ítem y no la orden. */
    Optional<HechoGasto> findByItemId(Long itemId);

    /** Los reportes siguen la redirección de una categoría unificada. Devuelve cuántas filas cambió. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update HechoGasto g set g.categoriaId = :destino where g.categoriaId = :origen")
    int reasignarCategoria(@Param("origen") Long origen, @Param("destino") Long destino);

    @Query("""
            select g.sectorId as clave, g.moneda as moneda, sum(g.importe) as total,
                   sum(g.montoEnvio) as envio, count(g) as cantidad
            from HechoGasto g
            where g.fecha between :desde and :hasta
            group by g.sectorId, g.moneda
            """)
    List<FilaGasto> gastoPorSector(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select g.categoriaId as clave, g.moneda as moneda, sum(g.importe) as total,
                   sum(g.montoEnvio) as envio, count(g) as cantidad
            from HechoGasto g
            where g.fecha between :desde and :hasta
            group by g.categoriaId, g.moneda
            """)
    List<FilaGasto> gastoPorCategoria(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select g.proveedorId as clave, g.moneda as moneda, sum(g.importe) as total,
                   sum(g.montoEnvio) as envio, count(g) as cantidad
            from HechoGasto g
            where g.fecha between :desde and :hasta
            group by g.proveedorId, g.moneda
            """)
    List<FilaGasto> gastoPorProveedor(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
