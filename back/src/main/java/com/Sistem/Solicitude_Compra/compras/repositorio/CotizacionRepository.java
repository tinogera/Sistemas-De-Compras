package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {

    List<Cotizacion> findByItemIdOrderByCargadaEnDesc(Long itemId);
}
