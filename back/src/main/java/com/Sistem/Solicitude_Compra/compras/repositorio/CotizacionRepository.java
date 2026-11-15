package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import com.Sistem.Solicitude_Compra.compras.dominio.EstadoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findByItemIdOrderByCargadaEnDesc(Long itemId);

    Optional<Cotizacion> findByIdAndItemId(Long id, Long itemId);

    Optional<Cotizacion> findByItemIdAndEstado(Long itemId, EstadoCotizacion estado);

    /** Una sola consulta para toda la página de la bandeja (evita el N+1 de T10.6). */
    @Query("select distinct c.itemId from Cotizacion c where c.itemId in :ids")
    List<Long> itemIdsConCotizacion(@Param("ids") Collection<Long> ids);

    @Query("select distinct c.itemId from Cotizacion c where c.itemId in :ids and c.fechaValidez >= :hoy")
    List<Long> itemIdsConCotizacionVigente(@Param("ids") Collection<Long> ids, @Param("hoy") LocalDate hoy);
}
