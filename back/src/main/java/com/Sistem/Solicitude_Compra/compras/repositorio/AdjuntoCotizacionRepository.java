package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.AdjuntoCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjuntoCotizacionRepository extends JpaRepository<AdjuntoCotizacion, Long> {

    List<AdjuntoCotizacion> findByCotizacionIdOrderBySubidoEnAsc(Long cotizacionId);
}
