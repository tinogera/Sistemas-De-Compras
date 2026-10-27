package com.Sistem.Solicitude_Compra.compras.repositorio;

import com.Sistem.Solicitude_Compra.compras.dominio.Cotizacion;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CotizacionRepository extends  JpaRepository<Cotizacion,Long>{
    Page<Cotizacion> findByItemIdOrderByCargadaEnDesc(Long id);
}
